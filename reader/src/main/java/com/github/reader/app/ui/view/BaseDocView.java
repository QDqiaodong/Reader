package com.github.reader.app.ui.view;

import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.Scroller;

import com.github.reader.R;
import com.github.reader.pdf.model.Hit;
import com.github.reader.pdf.model.Stepper;
import com.github.reader.pdf.ui.adapater.MuPDFPageAdapter;
import com.github.reader.utils.Constants;
import com.github.reader.utils.LogUtils;
import com.github.reader.utils.ScreenUtils;
import com.github.reader.utils.ToastUtil;

import java.util.LinkedList;
import java.util.NoSuchElementException;

public class BaseDocView
        extends AdapterView<Adapter>
        implements GestureDetector.OnGestureListener, ScaleGestureDetector.OnScaleGestureListener, Runnable {
    private static final String TAG = "ReaderView";
    private Context mContext;
    private boolean isVerticalScrollInit = false;

    /**
     * Viewing:属于正常状态
     * Selecting:当要绘制高亮,复制,下划线,删除线时的状态
     * Drawing: 开启墨迹状态
     */
    public enum Mode {
        Viewing, Selecting, Drawing
    }

    private Mode mMode = Mode.Viewing;

    public void setMode(Mode m) {
        mMode = m;
    }

    private boolean tapDisabled = false; //表示此时刻隐藏buttons
    private int tapPageMargin;

    private static final int MOVING_DIAGONALLY = 0;
    private static final int MOVING_LEFT = 1;
    private static final int MOVING_RIGHT = 2;
    private static final int MOVING_UP = 3;
    private static final int MOVING_DOWN = 4;

    private static final int FLING_MARGIN = 100;
    private static final int GAP = 20;

    private static float MIN_SCALE = 1.0f;
    private static final float MAX_SCALE = 5.0f;

    private Adapter mAdapter;
    private int mCurrent;    // Adapter's index for the current view
    private boolean mResetLayout;
    private final SparseArray<View>
            mChildViews = new SparseArray<View>(3);
    // Shadows the children of the adapter view
    // but with more sensible indexing

    //双向列表
    private final LinkedList<View>
            mViewCache = new LinkedList<View>();
    private boolean mUserInteracting;  // Whether the user is interacting
    private boolean mScaling;    // Whether the user is currently pinch zooming
    private float mScale = 1.0f;  //用户手势缩放的大小
    private int mXScroll;    // Scroll amounts recorded from events.
    private int mYScroll;    // and then accounted for in onLayout
    private GestureDetector mGestureDetector;
    private ScaleGestureDetector
            mScaleGestureDetector;
    private Scroller mScroller;
    private Stepper mStepper;
    private int mScrollerLastX;
    private int mScrollerLastY;
    private float mLastScaleFocusX;
    private float mLastScaleFocusY;

    public BaseDocView(Context context) {
        super(context);
        init(context);

    }

    private void init(Context context) {
        mContext = context;
        MIN_SCALE = Constants.HORIZONTAL_SCROLLING ? 1.0f : 0.5f;
        mGestureDetector = new GestureDetector(this);
        mScaleGestureDetector = new ScaleGestureDetector(context, this);
        mScroller = new Scroller(context);
        mStepper = new Stepper(this, this);
        setup();
    }

    public BaseDocView(Context context, AttributeSet attrs) {
        super(context, attrs);

        // "Edit mode" means when the View is being displayed in the Android GUI editor. (this class
        // is instantiated in the IDE, so we need to be a bit careful what we do).
        if (isInEditMode()) {
            mGestureDetector = null;
            mScaleGestureDetector = null;
            mScroller = null;
            mStepper = null;
        } else {
            init(context);
        }
    }

    public BaseDocView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void setup() {
        if(Constants.HORIZONTAL_SCROLLING){
            tapPageMargin = ScreenUtils.getScreenWidth() / 3;
        }else{
            tapPageMargin = ScreenUtils.getScreenHeight() / 3;
        }
    }

    /**
     * 显示或者隐藏buttons
     */
    protected void onTapMainDocArea() {
    }

    /**
     * 隐藏buttons
     */
    protected void onDocMotion() {
    }

    /**
     * 点击到不同区域的回调
     *
     * @param item
     */
    protected void onHit(Hit item) {
    }

    /**
     * 返回当前展示的页号
     *
     * @return
     */
    public int getDisplayedViewIndex() {
        return mCurrent;
    }

    /**
     * 跳转到指定页面
     *
     * @param i
     */
    public void setDisplayedViewIndex(int i) {
        if (0 <= i && i < mAdapter.getCount()) {
            onMoveOffChild(mCurrent);
            mCurrent = i;
            onMoveToChild(i);
            mResetLayout = true;
            requestLayout();
        }
    }

    // When advancing down the page, we want to advance by about
    // 90% of a screenful. But we'd be happy to advance by between
    // 80% and 95% if it means we hit the bottom in a whole number
    // of steps.
    private int smartAdvanceAmount(int screenHeight, int max) {
        LogUtils.d(TAG, "smartAdvanceAmount screenHeight=" + screenHeight + " max=" + max);
        int advance = (int) (screenHeight * 0.9 + 0.5);
        int leftOver = max % advance;
        int steps = max / advance;
        if (leftOver == 0) {
            // We'll make it exactly. No adjustment
        } else if ((float) leftOver / steps <= screenHeight * 0.05) {
            // We can adjust up by less than 5% to make it exact.
            advance += (int) ((float) leftOver / steps + 0.5);
        } else {
            int overshoot = advance - leftOver;
            if ((float) overshoot / steps <= screenHeight * 0.1) {
                // We can adjust down by less than 10% to make it exact.
                advance -= (int) ((float) overshoot / steps + 0.5);
            }
        }
        if (advance > max)
            advance = max;
        return advance;
    }

    public void smartMoveForwards() {
        View v = mChildViews.get(mCurrent);
        if (v == null)
            return;

        // The following code works in terms of where the screen is on the views;
        // so for example, if the currentView is at (-100,-100), the visible
        // region would be at (100,100). If the previous page was (2000, 3000) in
        // size, the visible region of the previous page might be (2100 + GAP, 100)
        // (i.e. off the previous page). This is different to the way the rest of
        // the code in this file is written, but it's easier for me to think about.
        // At some point we may refactor this to fit better with the rest of the
        // code.

        // screenWidth/Height are the actual width/height of the screen. e.g. 480/800
        int screenWidth = getWidth();
        int screenHeight = getHeight();
        // We might be mid scroll; we want to calculate where we scroll to based on
        // where this scroll would end, not where we are now (to allow for people
        // bashing 'forwards' very fast.
        int remainingX = mScroller.getFinalX() - mScroller.getCurrX();
        int remainingY = mScroller.getFinalY() - mScroller.getCurrY();
        Log.d(TAG, "smartMoveForwards: mScroller.getFinalX()="+mScroller.getFinalX()+" mScroller.getCurrX()="+mScroller.getCurrX()
                +" remainingX="+remainingX
        );
        // right/bottom is in terms of pixels within the scaled document; e.g. 1000
        int top = -(v.getTop() + mYScroll + remainingY);
        int right = screenWidth - (v.getLeft() + mXScroll + remainingX);
        int bottom = screenHeight + top;
        // docWidth/Height are the width/height of the scaled document e.g. 2000x3000
        int docWidth = v.getMeasuredWidth();
        int docHeight = v.getMeasuredHeight();

        int xOffset, yOffset;
        if (bottom >= docHeight) {
            // We are flush with the bottom. Advance to next column.
            if (right + screenWidth > docWidth) {
                // No room for another column - go to next page
                View nv = mChildViews.get(mCurrent + 1);
                if (nv == null) // No page to advance to
                    return;
                int nextTop = -(nv.getTop() + mYScroll + remainingY);
                int nextLeft = -(nv.getLeft() + mXScroll + remainingX);
                int nextDocWidth = nv.getMeasuredWidth();
                int nextDocHeight = nv.getMeasuredHeight();

                // Allow for the next page maybe being shorter than the screen is high
                yOffset = (nextDocHeight < screenHeight ? ((nextDocHeight - screenHeight) >> 1) : 0);

                if (nextDocWidth < screenWidth) {
                    // Next page is too narrow to fill the screen. Scroll to the top, centred.
                    xOffset = (nextDocWidth - screenWidth) >> 1;
                } else {
                    // Reset X back to the left hand column
                    xOffset = right % screenWidth;
                    // Adjust in case the previous page is less wide
                    if (xOffset + screenWidth > nextDocWidth)
                        xOffset = nextDocWidth - screenWidth;
                }
                xOffset -= nextLeft;
                yOffset -= nextTop;
            } else {
                // Move to top of next column
                xOffset = screenWidth;
                yOffset = screenHeight - bottom;
            }
        } else {
            // Advance by 90% of the screen height downwards (in case lines are partially cut off)
            xOffset = 0;
            yOffset = smartAdvanceAmount(screenHeight, docHeight - bottom);
        }
        mScrollerLastX = mScrollerLastY = 0;
        Log.d(TAG, "smartMoveForwards: startScroll");
        mScroller.startScroll(0, 0, remainingX - xOffset, remainingY - yOffset, 400);
        mStepper.prod();
    }

    public void smartMoveBackwards() {
        View v = mChildViews.get(mCurrent);
        if (v == null)
            return;

        // The following code works in terms of where the screen is on the views;
        // so for example, if the currentView is at (-100,-100), the visible
        // region would be at (100,100). If the previous page was (2000, 3000) in
        // size, the visible region of the previous page might be (2100 + GAP, 100)
        // (i.e. off the previous page). This is different to the way the rest of
        // the code in this file is written, but it's easier for me to think about.
        // At some point we may refactor this to fit better with the rest of the
        // code.

        // screenWidth/Height are the actual width/height of the screen. e.g. 480/800
        int screenWidth = getWidth();
        int screenHeight = getHeight();
        // We might be mid scroll; we want to calculate where we scroll to based on
        // where this scroll would end, not where we are now (to allow for people
        // bashing 'forwards' very fast.
        int remainingX = mScroller.getFinalX() - mScroller.getCurrX();
        int remainingY = mScroller.getFinalY() - mScroller.getCurrY();
        // left/top is in terms of pixels within the scaled document; e.g. 1000
        int left = -(v.getLeft() + mXScroll + remainingX);
        int top = -(v.getTop() + mYScroll + remainingY);
        // docWidth/Height are the width/height of the scaled document e.g. 2000x3000
        int docHeight = v.getMeasuredHeight();

        int xOffset, yOffset;
        if (top <= 0) {
            // We are flush with the top. Step back to previous column.
            if (left < screenWidth) {
                /* No room for previous column - go to previous page */
                View pv = mChildViews.get(mCurrent - 1);
                if (pv == null) /* No page to advance to */
                    return;
                int prevDocWidth = pv.getMeasuredWidth();
                int prevDocHeight = pv.getMeasuredHeight();

                // Allow for the next page maybe being shorter than the screen is high
                yOffset = (prevDocHeight < screenHeight ? ((prevDocHeight - screenHeight) >> 1) : 0);

                int prevLeft = -(pv.getLeft() + mXScroll);
                int prevTop = -(pv.getTop() + mYScroll);
                if (prevDocWidth < screenWidth) {
                    // Previous page is too narrow to fill the screen. Scroll to the bottom, centred.
                    xOffset = (prevDocWidth - screenWidth) >> 1;
                } else {
                    // Reset X back to the right hand column
                    xOffset = (left > 0 ? left % screenWidth : 0);
                    if (xOffset + screenWidth > prevDocWidth)
                        xOffset = prevDocWidth - screenWidth;
                    while (xOffset + screenWidth * 2 < prevDocWidth)
                        xOffset += screenWidth;
                }
                xOffset -= prevLeft;
                yOffset -= prevTop - prevDocHeight + screenHeight;
            } else {
                // Move to bottom of previous column
                xOffset = -screenWidth;
                yOffset = docHeight - screenHeight + top;
            }
        } else {
            // Retreat by 90% of the screen height downwards (in case lines are partially cut off)
            xOffset = 0;
            yOffset = -smartAdvanceAmount(screenHeight, top);
        }
        mScrollerLastX = mScrollerLastY = 0;
        mScroller.startScroll(0, 0, remainingX - xOffset, remainingY - yOffset, 400);
        mStepper.prod();
    }

    public void resetupChildren() {
        for (int i = 0; i < mChildViews.size(); i++)
            onChildSetup(mChildViews.keyAt(i), mChildViews.valueAt(i));
    }

    public void refresh() {
        mResetLayout = true;
        mScale = 1.0f;
        mXScroll = mYScroll = 0;
        MIN_SCALE = Constants.HORIZONTAL_SCROLLING ? 1.0f:0.5f;
        Log.d(TAG, "refresh: MIN_SCALE="+MIN_SCALE);
        setup();
        requestLayout();
    }

    /**
     * 初始化"搜索"功能的view,如果有就设置，没用就设置null
     *
     * @param i
     * @param v
     */
    protected void onChildSetup(int i, View v) {
    }

    /**
     * 如果有搜索内容的，绘制出搜索内容
     *
     * @param i
     */
    protected void onMoveToChild(int i) {
    }

    /**
     * 清除搜索内容
     *
     * @param i
     */
    protected void onMoveOffChild(int i) {
    }

    /**
     * 用于创建高清文档的ImageView
     *
     * @param v
     */
    protected void onSettle(View v) {
    }

    /**
     * 用于移除高清文档的ImageView
     *
     * @param v
     */
    protected void onUnsettle(View v) {
    }

    /**
     * 销毁Bitmap
     */
    protected void onDestory() {
        for (int i = 0; i < mChildViews.size(); i++) {
            ((IBaseDocView) mChildViews.valueAt(i)).onDestory();
        }
    }

    /**
     * 页面被重新利用时调用
     *
     * @param v
     */
    protected void onNotInUse(View v) {
    }

    public View getView(int i) {
        return mChildViews.get(i);
    }

    public View getDisplayedView() {
        return mChildViews.get(mCurrent);
    }

    public void run() {
        Log.d(TAG, "run: ..........");
        if (!mScroller.isFinished()) {
            mScroller.computeScrollOffset();
            int x = mScroller.getCurrX();
            int y = mScroller.getCurrY();
            mXScroll += x - mScrollerLastX;
            mYScroll += y - mScrollerLastY;
            mScrollerLastX = x;
            mScrollerLastY = y;

            Log.d(TAG, "run: mScale="+mScale+" x="+x+" mXScroll="+mXScroll+" mScrollerLastX="+mScrollerLastX);
            if(mScale < 1.0f){
                mScale += (float)(mXScroll+1) / 100.0f;
                mScale = Math.min(mScale,1.0f);
            }

            requestLayout();
            mStepper.prod();

        } else if (!mUserInteracting) {
            // End of an inertial scroll and the user is not interacting.
            // The layout is stable
            View v = mChildViews.get(mCurrent);

            if (!Constants.HORIZONTAL_SCROLLING && mScale < 1.0f) {
                mScale = 1.0f;
                requestLayout();
            }
            if (v != null)
                LogUtils.d(TAG, "run postSettle");
            postSettle(v);
        }
    }

    public boolean onDown(MotionEvent arg0) {
        mScroller.forceFinished(true);
        return true;
    }

    /**
     * @param e1
     * @param e2
     * @param velocityX 　　终点－起点
     * @param velocityY　　
     * @return
     */
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
                           float velocityY) {
        switch (mMode) {
            case Viewing:
                break;
            default:
                return true;
        }

        if (mScaling)
            return true;

        LogUtils.d(TAG, "onFling.................velocityX=..." + velocityX + " velocityY=" + velocityY
                + " e1.x=" + e1.getRawX() + " e1.y=" + e1.getRawY() + " e2.x=" + e2.getRawX() + " e2.y=" + e2.getRawY());
        View v = mChildViews.get(mCurrent);
        if (v != null) {
            Rect bounds = getScrollBounds(v);
            switch (directionOfTravel(velocityX, velocityY)) {
                case MOVING_LEFT:
                    if (Constants.HORIZONTAL_SCROLLING && bounds.left >= 0) {
                        // Fling off to the left bring next view onto screen
                        View vl = mChildViews.get(mCurrent + 1);

                        if (vl != null) {
                            slideViewOntoScreen(vl);
                            return true;
                        }
                    }
                    break;
                case MOVING_UP:
                    if (!Constants.HORIZONTAL_SCROLLING && bounds.top >= 0) {
                        // Fling off to the top bring next view onto screen
                        View vl = mChildViews.get(mCurrent + 1);

                        if (vl != null) {
                            slideViewOntoScreen(vl);
                            return true;
                        }
                    }
                    break;
                case MOVING_RIGHT:
                    if (Constants.HORIZONTAL_SCROLLING && bounds.right <= 0) {
                        // Fling off to the right bring previous view onto screen
                        View vr = mChildViews.get(mCurrent - 1);

                        if (vr != null) {
                            slideViewOntoScreen(vr);
                            return true;
                        }
                    }
                    break;
                case MOVING_DOWN:
                    if (!Constants.HORIZONTAL_SCROLLING && bounds.bottom <= 0) {
                        // Fling off to the bottom bring previous view onto screen
                        View vr = mChildViews.get(mCurrent - 1);

                        if (vr != null) {
                            slideViewOntoScreen(vr);
                            return true;
                        }
                    }
                    break;
            }
            mScrollerLastX = mScrollerLastY = 0;
            // If the page has been dragged out of bounds then we want to spring back
            // nicely. fling jumps back into bounds instantly, so we don't want to use
            // fling in that case. On the other hand, we don't want to forgo a fling
            // just because of a slightly off-angle drag taking us out of bounds other
            // than in the direction of the drag, so we test for out of bounds only
            // in the direction of travel.
            //
            // Also don't fling if out of bounds in any direction by more than fling
            // margin
            Rect expandedBounds = new Rect(bounds);
            expandedBounds.inset(-FLING_MARGIN, -FLING_MARGIN);

            if (withinBoundsInDirectionOfTravel(bounds, velocityX, velocityY)
                    && expandedBounds.contains(0, 0)) {
                mScroller.fling(0, 0, (int) velocityX, (int) velocityY, bounds.left, bounds.right, bounds.top, bounds.bottom);
                Log.d(TAG, "onFling: mScroller.fling bounds.left="+bounds.left+" bounds.right="+bounds.right+" bounds.top="+bounds.top+" bounds.bottom="+bounds.bottom);
                mStepper.prod();
            }
        }

        return true;
    }

    public void onLongPress(MotionEvent e) {
    }

    /**
     * @param e1
     * @param e2
     * @param distanceX 　上一次的值-滑动的坐标
     * @param distanceY 　上一次的值-起始-滑动的坐标
     * @return
     */
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
                            float distanceY) {
        Log.d(TAG, "onScroll: ..................");
        IBaseDocView pageView = (IBaseDocView) getDisplayedView();
        switch (mMode) {
            case Viewing:
                //滑动时，如果button显示，则先隐藏
                if (!tapDisabled)
                    onDocMotion();
                break;
            case Selecting:
                if (pageView != null)
                    pageView.selectText(e1.getX(), e1.getY(), e2.getX(), e2.getY());
                return true;
            default:
                return true;
        }

        if (!mScaling) {
            mXScroll -= distanceX;
            mYScroll -= distanceY;
            requestLayout();
        }
        LogUtils.d(TAG, "onScroll end distanceX=" + distanceX + " distanceY=" + distanceY + " mXScroll=" + mXScroll + " mYScroll=" + mYScroll);
        return true;
    }

    public void onShowPress(MotionEvent e) {
    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        if (mMode == Mode.Viewing && !tapDisabled) {
            IBaseDocView pageView = (IBaseDocView) getDisplayedView();
            //1.返回点击出的类型，普通区域，或者annoation区域
            Hit item = pageView.passClickEvent(e.getX(), e.getY());
            //2.由Activity中做处理
            onHit(item);
            LogUtils.d(TAG, "onSingleTapUp hit.item=" + item.ordinal());

            //TODO 3.点击翻页策略　
            if (item == Hit.Nothing) {
                LogUtils.d(TAG,"onSingleTapUp Constants.HORIZONTAL_SCROLLING="+Constants.HORIZONTAL_SCROLLING
                        +" e.getX()="+e.getX()+" tapPageMargin="+tapPageMargin);
                if (Constants.HORIZONTAL_SCROLLING) {
                    if (e.getX() < tapPageMargin) {
                        smartMoveBackwards();
                    } else if (e.getX() > super.getWidth() - tapPageMargin) {
                        smartMoveForwards();
                    } else {
                        onTapMainDocArea();
                    }
                } else {
                    if (e.getY() < tapPageMargin) {
                        smartMoveBackwards();
                    } else if (e.getY() > super.getHeight() - tapPageMargin) {
                        smartMoveForwards();
                    } else {
                        onTapMainDocArea();
                    }
                }
            }
        }
        return false;
    }

    //双向列表
    private final LinkedList<Float> mScaleCache= new LinkedList<Float>();
    public boolean onScale(ScaleGestureDetector detector) {
        LogUtils.d(TAG, "ReaderView onScale　detector.getScaleFactor()＝"+detector.getScaleFactor());
        float previousScale = mScale;
        float scale_factor = 1.0f;
        float min_scale = MIN_SCALE * scale_factor;
        float max_scale = MAX_SCALE * scale_factor;
        mScale = Math.min(Math.max(mScale * detector.getScaleFactor(), min_scale), max_scale);
        mScaleCache.addFirst(detector.getScaleFactor());

        float factor = mScale / previousScale;
        View v = mChildViews.get(mCurrent);
        if (v != null) {
            float currentFocusX = detector.getFocusX();
            float currentFocusY = detector.getFocusY();
            // Work out the focus point relative to the view top left
            int viewFocusX = (int) currentFocusX - (v.getLeft() + mXScroll);
            int viewFocusY = (int) currentFocusY - (v.getTop() + mYScroll);
            LogUtils.d(TAG, "previousScale=" + previousScale + " mScale=" + mScale + " factor=" + factor + " currentFocusX=" + currentFocusX + " currentFocusY=" + currentFocusY
                    + " mXScroll=" + mXScroll + " mYScroll=" + mYScroll + " viewFocusX=" + viewFocusX + " viewFocusY=" + viewFocusY + " mLastScaleFocusX=" + mLastScaleFocusX + " mLastScaleFocusY=" + mLastScaleFocusY
            );
            // Scroll to maintain the focus point
            mXScroll += viewFocusX - viewFocusX * factor;
            mYScroll += viewFocusY - viewFocusY * factor;

            if (mLastScaleFocusX >= 0)
                mXScroll += currentFocusX - mLastScaleFocusX;
            if (mLastScaleFocusY >= 0)
                mYScroll += currentFocusY - mLastScaleFocusY;

            mLastScaleFocusX = currentFocusX;
            mLastScaleFocusY = currentFocusY;
            requestLayout();
        }

        return true;
    }

    public boolean onScaleBegin(ScaleGestureDetector detector) {
        tapDisabled = true;
        mScaling = true;
        // Ignore any scroll amounts yet to be accounted for: the
        // screen is not showing the effect of them, so they can
        // only confuse the user
        mXScroll = mYScroll = 0;
        mLastScaleFocusX = mLastScaleFocusY = -1;
        mScaleCache.clear();
        return true;
    }

    public void onScaleEnd(ScaleGestureDetector detector) {
        mScaling = false;
    }

    private float mX, mY;
    private static final float TOUCH_TOLERANCE = 2;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        LogUtils.d(TAG, "ReaderView onTouchEvent");
        mScaleGestureDetector.onTouchEvent(event);
        mGestureDetector.onTouchEvent(event);

        if (mMode == Mode.Drawing) {
            float x = event.getX();
            float y = event.getY();
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    touch_start(x, y);
                    break;
                case MotionEvent.ACTION_MOVE:
                    touch_move(x, y);
                    break;
                case MotionEvent.ACTION_UP:
                    touch_up();
                    break;
            }
        }

        if ((event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_DOWN) {
            mUserInteracting = true;
            tapDisabled = false;
        }
        if ((event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_UP) {
            mUserInteracting = false;
            LogUtils.d(TAG, "onTouchEvent ACTION_UP");
            View v = mChildViews.get(mCurrent);
            if (v != null) {
                if (mScroller.isFinished()) {
                    // If, at the end of user interaction, there is no
                    // current inertial scroll in operation then animate
                    // the view onto screen if necessary
                    if(Constants.HORIZONTAL_SCROLLING){
                        slideViewOntoScreen(v);
                    }else if(!mScaling && !Constants.HORIZONTAL_SCROLLING){
                        Log.d(TAG, "onTouchEvent: mScale="+mScale);
                        if(mScale < 1.0f){
                            mScrollerLastX = mScrollerLastY = 0;
                            int scaleFactor = (int) ((1-mScale)*100);
                            mScroller.startScroll(0,0,scaleFactor,0,400);
                            mStepper.prod();
                        }
                    }
                }

                if (mScroller.isFinished()) {
                    // If still there is no inertial scroll in operation
                    // then the layout is stable
                    LogUtils.d(TAG, "onTouchEvent postSettle");
                    postSettle(v);
                }
            }
        }

        requestLayout();
        return true;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int n = getChildCount();
        LogUtils.d(TAG, "readerview onMeasure..............n=" + n);
        for (int i = 0; i < n; i++) {
            LogUtils.d(TAG, "ReaderView onMeasure i=" + i + " n==" + n);
            measureView(getChildAt(i));
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        try {
            onLayout2(changed, left, top, right, bottom);
        } catch (OutOfMemoryError e) {
            ToastUtil.getInstance().showToast("Out of memory during layout");
        }
    }

    private void onLayout2(boolean changed, int left, int top, int right,
                           int bottom) {
        LogUtils.d(TAG, "readview onlayout2　mResetLayout=" + mResetLayout);
        if (isInEditMode())
            return;

        View cv = mChildViews.get(mCurrent);
        Point cvOffset;
        if (!mResetLayout) {
            // Move to next or previous if current is sufficiently off center
            if (cv != null) {
                boolean move = false;
                cvOffset = subScreenSizeOffset(cv);
                /**用于判定是否需要加载下一页
                 * 规则: 左右滑动时，当pdf的右边+偏移量小于屏幕的一半时，符合加载下一页规则
                 * */
                // cv.getRight() may be out of date with the current scale
                // so add left to the measured width for the correct position
                if (Constants.HORIZONTAL_SCROLLING) {
                    move = cv.getLeft() + cv.getMeasuredWidth() + cvOffset.x + GAP / 2 + mXScroll < getWidth() / 2;
                } else {
                    move = cv.getTop() + cv.getMeasuredHeight() + cvOffset.y + GAP / 2 + mYScroll < getHeight() / 2;
                }
                if (move && mCurrent + 1 < mAdapter.getCount()) {
                    //移除当前Hq
                    postUnsettle(cv);
                    // post to invoke test for end of animation
                    // where we must set hq area for the new current view
                    mStepper.prod();
                    //去掉当前页的选择框
                    onMoveOffChild(mCurrent);
                    mCurrent++;
                    //绘制下一页的搜索框
                    onMoveToChild(mCurrent);
                }

                /**用于判断上一页*/
                if (Constants.HORIZONTAL_SCROLLING)
                    move = cv.getLeft() - cvOffset.x - GAP / 2 + mXScroll >= getWidth() / 2;
                else
                    move = cv.getTop() - cvOffset.y - GAP / 2 + mYScroll >= getHeight() / 2;
                if (move && mCurrent > 0) {
                    postUnsettle(cv);
                    // post to invoke test for end of animation
                    // where we must set hq area for the new current view
                    mStepper.prod();

                    onMoveOffChild(mCurrent);
                    mCurrent--;
                    onMoveToChild(mCurrent);
                }
            }

            // Remove not needed children and hold them for reuse
            int numChildren = mChildViews.size();
            int childIndices[] = new int[numChildren];
            for (int i = 0; i < numChildren; i++)
                childIndices[i] = mChildViews.keyAt(i);

            for (int i = 0; i < numChildren; i++) {
                int ai = childIndices[i];
                if (ai < mCurrent - 1 || ai > mCurrent + 1) {
                    View v = mChildViews.get(ai);
                    onNotInUse(v);
                    mViewCache.add(v);
                    removeViewInLayout(v);
                    mChildViews.remove(ai);
                }
            }
        } else {
            mResetLayout = false;
            mXScroll = mYScroll = 0;

            // Remove all children and hold them for reuse
            int numChildren = mChildViews.size();
            for (int i = 0; i < numChildren; i++) {
                View v = mChildViews.valueAt(i);
                onNotInUse(v);
                mViewCache.add(v);
                removeViewInLayout(v);
            }
            mChildViews.clear();

            // Don't reuse cached views if the adapter has changed
            mViewCache.clear();

            // post to ensure generation of hq area
            mStepper.prod();
        }

        // Ensure current view is present
        int cvLeft, cvRight, cvTop, cvBottom;

        //qiaodong
        if(Constants.HORIZONTAL_SCROLLING && mScale <= 1.0f){
            mYScroll = 0;
        }else if(!Constants.HORIZONTAL_SCROLLING && mScale <= 1.0f){
            mXScroll = 0;
        }

        boolean notPresent = (mChildViews.get(mCurrent) == null);
        cv = getOrCreateChild(mCurrent);
        // When the view is sub-screen-size in either dimension we
        // offset it to center within the screen area, and to keep
        // the views spaced out
        cvOffset = subScreenSizeOffset(cv);
        if (notPresent) {
            //Main item not already present. Just place it top left
            cvLeft = cvOffset.x;
            cvTop = cvOffset.y;
        } else {
            // Main item already present. Adjust by scroll offsets
            cvLeft = cv.getLeft() + mXScroll;
            cvTop = cv.getTop() + mYScroll;
        }
        Log.d(TAG, "onLayout2: mXScroll="+mXScroll+" mYScroll="+mYScroll+" cvLeft="+cvLeft
                +" cvTop="+cvTop+" cv.getMeasuredWidth()="+cv.getMeasuredWidth()+" cvOffset.x="+cvOffset.x);
        // Scroll values have been accounted for
        mXScroll = mYScroll = 0;
        cvRight = cvLeft + cv.getMeasuredWidth();
        cvBottom = cvTop + cv.getMeasuredHeight();

        //用与将view回归原位置
        if (!mUserInteracting && mScroller.isFinished()) {
            //qiaodong add
            if(Constants.HORIZONTAL_SCROLLING){
                Point corr = getCorrection(getScrollBounds(cvLeft, cvTop, cvRight, cvBottom));
                cvRight += corr.x;
                cvLeft += corr.x;
                cvTop += corr.y;
                cvBottom += corr.y;
            }

            if (!Constants.HORIZONTAL_SCROLLING && !isVerticalScrollInit) {
                Point corr = getCorrection(getScrollBounds(cvLeft, cvTop, cvRight, cvBottom));
                cvRight += corr.x;
                cvLeft += corr.x;
                cvTop += corr.y;
                cvBottom += corr.y;
                isVerticalScrollInit = true;
            }


        } else if (Constants.HORIZONTAL_SCROLLING && cv.getMeasuredHeight() <= getHeight()) {
            // When the current view is as small as the screen in height, clamp
            // it vertically
            Point corr = getCorrection(getScrollBounds(cvLeft, cvTop, cvRight, cvBottom));
            cvTop += corr.y;
            cvBottom += corr.y;
        } else if (!Constants.HORIZONTAL_SCROLLING && cv.getMeasuredWidth() <= getWidth()) {
            // When the current view is as small as the screen in width, clamp
            // it horizontally
            Point corr = getCorrection(getScrollBounds(cvLeft, cvTop, cvRight, cvBottom));
            cvRight += corr.x;
            cvLeft += corr.x;
            if( cvTop > 0 && mCurrent==0 ){
                cvTop = 0;
            }
            cvBottom = cvTop + cv.getMeasuredHeight();
            Log.d(TAG, "onLayout2: !Constants.HORIZONTAL_SCROLLING cvLeft="+cvLeft+" cvRight="+cvRight);
        }

        cv.layout(cvLeft, cvTop, cvRight, cvBottom);
        LogUtils.d(TAG, "onlayout2 cvLeft=" + cvLeft + " cvTop=" + cvTop + " cvRight=" + cvRight + " cvBottom=" + cvBottom + " mCurrent=" + mCurrent);

        //计算并且布局上一页,以当前的坐标来绘制下一页
        if (mCurrent > 0) {
            View lv = getOrCreateChild(mCurrent - 1);
            Point leftOffset = subScreenSizeOffset(lv);
            if (Constants.HORIZONTAL_SCROLLING) {
                int gap = leftOffset.x + GAP + cvOffset.x;
                lv.layout(cvLeft - lv.getMeasuredWidth() - gap,
                        (cvBottom + cvTop - lv.getMeasuredHeight()) / 2,
                        cvLeft - gap,
                        (cvBottom + cvTop + lv.getMeasuredHeight()) / 2);
                LogUtils.d(TAG, "onlayou2计算并且布局上一页，leftOffset.x=" + leftOffset.x + " cvOffset.x" + cvOffset.x +
                        " cvLeft=" + cvLeft + " cvTop=" + cvTop + " cvBottom=" + cvBottom + " lv.getMeasuredHeight()=" + lv.getMeasuredHeight()
                        + " lv.getMeasuredWidth()=" + lv.getMeasuredWidth()
                );
                LogUtils.d(TAG, "onlayout计算并布局上一页 lv.left=" + lv.getLeft() + " lv.top=" + lv.getTop() + " lv.right=" + lv.getRight() + " lv.getbottom=" + lv.getBottom());
            } else {
                int gap = leftOffset.y + GAP + cvOffset.y;
                lv.layout((cvLeft + cvRight - lv.getMeasuredWidth()) / 2,
                        cvTop - lv.getMeasuredHeight() - gap,
                        (cvLeft + cvRight + lv.getMeasuredWidth()) / 2,
                        cvTop - gap);
            }
        }

        //计算并且布局下一页
        if (mCurrent + 1 < mAdapter.getCount()) {
            View rv = getOrCreateChild(mCurrent + 1);
            Point rightOffset = subScreenSizeOffset(rv);
            if (Constants.HORIZONTAL_SCROLLING) {
                int gap = cvOffset.x + GAP + rightOffset.x;
                rv.layout(cvRight + gap,
                        (cvBottom + cvTop - rv.getMeasuredHeight()) / 2,
                        cvRight + rv.getMeasuredWidth() + gap,
                        (cvBottom + cvTop + rv.getMeasuredHeight()) / 2);
                LogUtils.d(TAG, "onlayou2计算并且布局下一页，rightOffset.x=" + rightOffset.x + " cvOffset.x" + cvOffset.x +
                        " cvTop=" + cvTop + " cvBottom=" + cvBottom + " cvRight=" + cvRight + " rv.getMeasuredHeight()=" + rv.getMeasuredHeight()
                        + " rv.getMeasuredWidth()=" + rv.getMeasuredWidth()
                );

                LogUtils.d(TAG, "onlayout计算并布局下一页 rv.left=" + rv.getLeft() + " rv.top=" + rv.getTop() + " rv.right=" + rv.getRight() + " rv.getbottom=" + rv.getBottom());
            } else {
                int gap = cvOffset.y + GAP + rightOffset.y;
                rv.layout((cvLeft + cvRight - rv.getMeasuredWidth()) / 2,
                        cvBottom + gap,
                        (cvLeft + cvRight + rv.getMeasuredWidth()) / 2,
                        cvBottom + gap + rv.getMeasuredHeight());
            }
        }

        invalidate();
    }

    @Override
    public Adapter getAdapter() {
        return mAdapter;
    }

    @Override
    public View getSelectedView() {
        return null;
    }

    @Override
    public void setAdapter(Adapter adapter) {

        //  release previous adapter's bitmaps
        if (null != mAdapter && adapter != mAdapter) {
            if (adapter instanceof MuPDFPageAdapter) {
                ((MuPDFPageAdapter) adapter).releaseBitmaps();
            }
        }

        mAdapter = adapter;

        requestLayout();
    }

    @Override
    public void setSelection(int arg0) {
        throw new UnsupportedOperationException(getContext().getString(R.string.not_supported));
    }

    private View getCached() {
        if (mViewCache.size() == 0)
            return null;
        else
            return mViewCache.removeFirst();
    }

    private View getOrCreateChild(int i) {
        LogUtils.d(TAG, "getOrCreateChild i==" + i);
        View v = mChildViews.get(i);
        if (v == null) {
            v = mAdapter.getView(i, getCached(), this);
            addAndMeasureChild(i, v);
            onChildSetup(i, v);
        }

        return v;
    }

    private void addAndMeasureChild(int i, View v) {
        LayoutParams params = v.getLayoutParams();
        if (params == null) {
            params = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        }
        addViewInLayout(v, 0, params, true);
        mChildViews.append(i, v); // Record the view against it's adapter index
        LogUtils.d(TAG, "addAndMeasureChild i=" + i);
        measureView(v);
    }

    private void measureView(View v) {
        // See what size the view wants to be
        v.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);

        float scale = Math.min((float) getWidth() / (float) v.getMeasuredWidth(),
                    (float) getHeight() / (float) v.getMeasuredHeight());

        // Use the fitting values scaled by our current scale factor
        v.measure(MeasureSpec.EXACTLY | (int) (v.getMeasuredWidth() * scale * mScale),
                MeasureSpec.EXACTLY | (int) (v.getMeasuredHeight() * scale * mScale));
    }

    private Rect getScrollBounds(int left, int top, int right, int bottom) {
        int xmin = getWidth() - right;
        int xmax = -left;
        int ymin = getHeight() - bottom;
        int ymax = -top;
        Log.d(TAG, "getScrollBounds: left="+left+" top="+top+" right="+right+" bottom="+bottom+" width="+getWidth()+" height="+getHeight());
        // In either dimension, if view smaller than screen then
        // constrain it to be central
        if (xmin > xmax) {
            Log.d(TAG, "getScrollBounds: xmin > xmax xmin="+xmin+" xmax="+xmax);
            xmin = xmax = (xmin + xmax) / 2;
        }

        if (ymin > ymax) {
            Log.d(TAG, "getScrollBounds: ymin > ymax ymin="+ymin+" ymax="+ymax);
            ymin = ymax = (ymin + ymax) / 2;
        }


        return new Rect(xmin, ymin, xmax, ymax);
    }

    private Rect getScrollBounds(View v) {
        // There can be scroll amounts not yet accounted for in
        // onLayout, so add mXScroll and mYScroll to the current
        // positions when calculating the bounds.
        LogUtils.d(TAG, "getScrollBounds mXScroll=" + mXScroll + " mYScroll=" + mYScroll);
        return getScrollBounds(v.getLeft() + mXScroll,
                v.getTop() + mYScroll,
                v.getLeft() + v.getMeasuredWidth() + mXScroll,
                v.getTop() + v.getMeasuredHeight() + mYScroll);
    }

    /**
     * 这个方法相当于修正，即：
     * 如果是左右滑动，view向左滑动了20px,
     * 那么需要修正回来，即向右滑动20才能回归原位置
     * @param bounds
     * @return
     */
    private Point getCorrection(Rect bounds) {
        return new Point(Math.min(Math.max(0, bounds.left), bounds.right),
                Math.min(Math.max(0, bounds.top), bounds.bottom));
    }

    /**
     * 画笔开始
     *
     * @param x
     * @param y
     */
    private void touch_start(float x, float y) {
        IBaseDocView pageView = (IBaseDocView) getDisplayedView();
        if (pageView != null) {
            pageView.startDraw(x, y);
        }
        mX = x;
        mY = y;
    }

    /**
     * 画笔移动
     *
     * @param x
     * @param y
     */
    private void touch_move(float x, float y) {
        float dx = Math.abs(x - mX);
        float dy = Math.abs(y - mY);
        if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
            IBaseDocView pageView = (IBaseDocView) getDisplayedView();
            if (pageView != null) {
                pageView.continueDraw(x, y);
            }
            mX = x;
            mY = y;
        }
    }

    /**
     * 画笔离开
     */
    private void touch_up() {
        // NOOP
    }


    /**
     * 设置当前页
     *
     * @param v
     */
    private void postSettle(final View v) {
        // onSettle and onUnsettle are posted so that the calls
        // wont be executed until after the system has performed
        // layout.
        post(new Runnable() {
            public void run() {
                onSettle(v);
            }
        });
    }

    private void postUnsettle(final View v) {
        post(new Runnable() {
            public void run() {
                onUnsettle(v);
            }
        });
    }

    /**
     * 自动滚动View
     * @param v
     */
    private void slideViewOntoScreen(View v) {
        Point corr = getCorrection(getScrollBounds(v));
        if (corr.x != 0 || corr.y != 0) {
            mScrollerLastX = mScrollerLastY = 0;
            LogUtils.d(TAG, "slideViewOntoScreen  corr.x=" + corr.x + " corr.y=" + corr.y);

            if(Constants.HORIZONTAL_SCROLLING){
                mScroller.startScroll(0, 0, corr.x, corr.y, 400);
            }else{
                mScroller.startScroll(0, 0, -corr.x, -corr.y, 400);
            }

            mStepper.prod();
        }
    }

    /**
     * pdf　页面距离左右和上下的偏移量
     * 当图片被放大，大于屏幕的宽高时，此时偏移量为0
     */
    private Point subScreenSizeOffset(View v) {
        LogUtils.d(TAG, "subScreenSizeOffset 页面距离左右和上下的偏移量 xoffset=" + Math.max((getWidth() - v.getMeasuredWidth()) / 2, 0)
                + " yoffset=" + Math.max((getHeight() - v.getMeasuredHeight()) / 2, 0));
       /* return new Point(Math.max((getWidth() - v.getMeasuredWidth()) / 2, 0),
                Math.max((getHeight() - v.getMeasuredHeight()) / 2, 0));*/

        if(!Constants.HORIZONTAL_SCROLLING){
           return  new Point(Math.max((getWidth() - v.getMeasuredWidth()) / 2, 0),0);
        }else{
           return new Point(Math.max((getWidth() - v.getMeasuredWidth()) / 2, 0),
                    Math.max((getHeight() - v.getMeasuredHeight()) / 2, 0));
        }
    }

    /**
     * 移动方向
     *
     * @param vx
     * @param vy
     * @return
     */
    private static int directionOfTravel(float vx, float vy) {
        if (Math.abs(vx) > 2 * Math.abs(vy))
            return (vx > 0) ? MOVING_RIGHT : MOVING_LEFT;
        else if (Math.abs(vy) > 2 * Math.abs(vx))
            return (vy > 0) ? MOVING_DOWN : MOVING_UP;
        else
            return MOVING_DIAGONALLY;//对角
    }

    private static boolean withinBoundsInDirectionOfTravel(Rect bounds, float vx, float vy) {
        switch (directionOfTravel(vx, vy)) {
            case MOVING_DIAGONALLY:
                return bounds.contains(0, 0);
            case MOVING_LEFT:
                return bounds.left <= 0;
            case MOVING_RIGHT:
                return bounds.right >= 0;
            case MOVING_UP:
                return bounds.top <= 0;
            case MOVING_DOWN:
                return bounds.bottom >= 0;
            default:
                throw new NoSuchElementException();
        }
    }
}
