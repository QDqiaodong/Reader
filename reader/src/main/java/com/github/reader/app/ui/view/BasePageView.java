package com.github.reader.app.ui.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.RectF;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.github.reader.R;
import com.github.reader.app.model.manager.TextSelector;
import com.github.reader.pdf.model.AsyncTask;
import com.github.reader.pdf.model.CancellableAsyncTask;
import com.github.reader.pdf.model.CancellableTaskDefinition;
import com.github.reader.pdf.model.TextWord;
import com.github.reader.utils.Constants;
import com.github.reader.utils.LogUtils;

import java.util.ArrayList;
import java.util.Iterator;

public abstract class BasePageView extends ViewGroup {
    private static final String TAG = "BasePageView";
    protected static final float INK_THICKNESS = 10.0f;//画笔厚度
    private static final int PROGRESS_DIALOG_DELAY = 200;
    protected final Context mContext;
    protected int mPageNumber;
    protected Point mParentSize;
    protected Point mSize;   // Size of page at minimum zoom
    protected float mSourceScale;

    protected ImageView mEntire; // Image rendered at minimum zoom
    protected Bitmap mEntireBm;//整个屏幕大的图片
    protected Matrix mEntireMat;
    private AsyncTask<Void, Void, TextWord[][]> mGetText;
    protected CancellableAsyncTask<Void, Void> mDrawEntire;

    private RectF mSearchBoxes[];
    private RectF mSelectBox;
    private TextWord mText[][];
    protected ArrayList<ArrayList<PointF>> mDrawing;
    protected View mSearchView;
    private boolean mIsBlank;

    private ProgressBar mBusyIndicator;
    private final Handler mHandler = new Handler();

    public BasePageView(Context c, Point parentSize) {
        super(c);
        mContext = c;
        mParentSize = parentSize;//屏幕的大小
        setBackgroundColor(getResources().getColor(R.color.common_black));
        mEntireMat = new Matrix();
    }

    protected abstract CancellableTaskDefinition<Void, Void> getDrawPageTask(Bitmap bm, int sizeX, int sizeY, int patchX, int patchY, int patchWidth, int patchHeight);

    protected abstract TextWord[][] getText();

    protected abstract void reintBgTask();

    protected abstract void drawHighlightRect(float scale, Canvas canvas, Paint paint);
    protected abstract void drawUnderline(float scale, Canvas canvas, Paint paint);
    protected abstract void drawStrikeOut(float scale, Canvas canvas, Paint paint);

    protected abstract void drawInkRect(float scale, Canvas canvas, Paint paint);

    protected abstract void drawDeleteRect(float scale, Canvas canvas, Paint paint);

    /**
     * 停止后台任务
     */
    private void reinit() {
        LogUtils.d(TAG, "reinit----------");
        //1.　核心绘制的任务
        if (mDrawEntire != null) {
            mDrawEntire.cancelAndWait();
            mDrawEntire = null;
        }

        //2.　获取内容文字的任务
        if (mGetText != null) {
            mGetText.cancel(true);
            mGetText = null;
        }
        //3. 其他后台任务
        reintBgTask();

        mIsBlank = true;
        mPageNumber = 0;

        if (mSize == null)
            mSize = mParentSize;

        if (mEntire != null) {
            mEntire.setImageBitmap(null);
            mEntire.invalidate();
        }

        //4. 搜索，选择，删除，
        mSearchBoxes = null;
        mSelectBox = null;
        mText = null;
    }

    public void releaseResources() {
        reinit();

        if (mBusyIndicator != null) {
            removeView(mBusyIndicator);
            mBusyIndicator = null;
        }
    }

    public void releaseBitmaps() {
        reinit();

        //  recycle bitmaps before releasing them.
        if (mEntireBm != null)
            mEntireBm.recycle();
        mEntireBm = null;
    }

    /**
     * 　当pdf的size还未知时，先显示加载的页面
     *
     * @param page
     */
    public void blank(int page) {
        reinit();
        mPageNumber = page;

        if (mBusyIndicator == null) {
            mBusyIndicator = new ProgressBar(mContext);
            mBusyIndicator.setIndeterminate(true);
            mBusyIndicator.setBackgroundResource(R.drawable.busy);
            addView(mBusyIndicator);
        }

        setBackgroundColor(getResources().getColor(R.color.background_color));
    }

    /**
     * 初始化当前页面的第一步
     *
     * @param page
     * @param size
     */
    public void setPage(int page, PointF size) {
        LogUtils.d(TAG, "setpage................page=." + page);

        //1.停止核心渲染内容的任务
        if (mDrawEntire != null) {
            mDrawEntire.cancelAndWait();
            mDrawEntire = null;
        }
        mIsBlank = false;

        //2.所有标注浮层的绘制
        if (mSearchView != null)
            mSearchView.invalidate();

        mPageNumber = page;

        //3.承载Doc的imageview
        if (mEntire == null) {
            mEntire = new OpaqueImageView(mContext);
            //matrix表示原图从ImageView的左上角开始绘制，如果原图大于ImageView，那么多余的部分则剪裁掉，如果原图小于ImageView，那么对原图不做任何处理
            mEntire.setScaleType(ImageView.ScaleType.MATRIX);
           // mEntire.setColorFilter(getResources().getColor(R.color.pdf_bg_color));
            addView(mEntire);

        }

        //4.动态计算当前,文档应该展示的实际大小
        if (Constants.HORIZONTAL_SCROLLING) {
            mSourceScale = Math.min(mParentSize.x / size.x, mParentSize.y / size.y);
        } else {
            mSourceScale = Math.max(mParentSize.x / size.x, mParentSize.y / size.y);
        }
        Point newSize = new Point((int) (size.x * mSourceScale), (int) (size.y * mSourceScale));
        mSize = newSize;
        LogUtils.d(TAG, "最终效果是高度适配屏幕高度原始尺寸size.x=" + size.x + " size.y=" + size.y + " 新尺寸mSize.x=" + mSize.x + " mSize.y=" + mSize.y + " mSourceScale=" + mSourceScale);
        mEntire.setImageBitmap(null);
        mEntire.invalidate();

        //5.计算承载Doc的Bitmap
        if (Constants.HORIZONTAL_SCROLLING) {
            mEntireBm = Bitmap.createBitmap(mParentSize.x, mParentSize.y, Bitmap.Config.ARGB_8888);
        } else {
            mEntireBm = Bitmap.createBitmap(mSize.x, mSize.y, Bitmap.Config.ARGB_8888);
        }
        mEntireBm = drawBg4Bitmap(getResources().getColor(R.color.button_pressed),mEntireBm);
        //6.子线程创建核心doc,交由子view来处理
        mDrawEntire = new CancellableAsyncTask<Void, Void>(getDrawPageTask(mEntireBm, mSize.x, mSize.y, 0, 0, mSize.x, mSize.y)) {
            @Override
            public void onPreExecute() {
                setBackgroundColor(getResources().getColor(R.color.background_color));
                mEntire.setImageBitmap(null);
                mEntire.invalidate();

                if (mBusyIndicator == null) {
                    mBusyIndicator = new ProgressBar(mContext);
                    mBusyIndicator.setIndeterminate(true);
                    mBusyIndicator.setBackgroundResource(R.drawable.busy);
                    addView(mBusyIndicator);
                    mBusyIndicator.setVisibility(INVISIBLE);
                    mHandler.postDelayed(new Runnable() {
                        public void run() {
                            if (mBusyIndicator != null)
                                mBusyIndicator.setVisibility(VISIBLE);
                        }
                    }, PROGRESS_DIALOG_DELAY);
                }
            }

            @Override
            public void onPostExecute(Void result) {
                removeView(mBusyIndicator);
                mBusyIndicator = null;
                mEntire.setImageBitmap(mEntireBm);
                mEntire.invalidate();
                setBackgroundColor(getResources().getColor(R.color.background_color));

            }
        };

        mDrawEntire.execute();

        //7. TODO　绘制标注
        if (mSearchView == null) {
            mSearchView = new View(mContext) {
                @Override
                protected void onDraw(final Canvas canvas) {
                    super.onDraw(canvas);
                    // Work out current total scale factor
                    // from source to view
                    final float scale = mSourceScale * (float) getWidth() / (float) mSize.x;
                    final Paint paint = new Paint();

                    //1. 绘制搜索框,只有搜索到内容的时候才会被绘制
                    if (!mIsBlank && mSearchBoxes != null) {
                        paint.setColor(getResources().getColor(R.color.highlight_color));
                        for (RectF rect : mSearchBoxes)
                            canvas.drawRect(rect.left * scale, rect.top * scale,
                                    rect.right * scale, rect.bottom * scale,
                                    paint);
                    }

                    //2. 绘制曾经，保存的Annoation
                    //2.1.绘制高亮区域
                    drawHighlightRect(scale, canvas, paint);
                    //2.2.绘制墨迹区域
                    drawInkRect(scale, canvas, paint);
                    //2.3.绘制删除框
                    drawDeleteRect(scale, canvas, paint);
                    //2.4 绘制下滑线
                    drawUnderline(scale, canvas, paint);
                    //2.5 绘制删除线
                    drawStrikeOut(scale, canvas, paint);

                    //3. 绘制选中框（比如:复制，高亮，删除线，下划线，选择文字时绘制）
                    if (mSelectBox != null && mText != null) {
                        paint.setStyle(Paint.Style.FILL); //实心效果
                        paint.setColor(getResources().getColor(R.color.highlight_color));
                        processSelectedText(new TextSelector.TextProcessor() {
                            RectF rect;

                            public void onStartLine() {
                                rect = new RectF();
                            }

                            //计算所有字符串所在的最大区域
                            public void onWord(TextWord word) {
                                rect.union(word);
                            }

                            public void onEndLine() {
                                if (!rect.isEmpty())
                                    canvas.drawRect(rect.left * scale, rect.top * scale, rect.right * scale, rect.bottom * scale, paint);
                            }
                        });
                    }

                    //4. 绘制墨迹
                    if (mDrawing != null) {
                        Path path = new Path();
                        PointF p;
                        paint.setAntiAlias(true);
                        paint.setDither(true); //设定是否使用图像抖动处理，会使绘制出来的图片颜色更加平滑和饱满，图像更加清晰
                        paint.setStrokeJoin(Paint.Join.ROUND); //设置绘制时各图形的结合方式，如平滑效果等
                        paint.setStrokeCap(Paint.Cap.ROUND);//设置笔刷的图形样式，如圆形样式

                        paint.setStyle(Paint.Style.FILL); //实心效果
                        paint.setStrokeWidth(INK_THICKNESS * scale);//设置笔刷的粗细度
                        paint.setColor(getResources().getColor(R.color.ink_color));

                        Iterator<ArrayList<PointF>> it = mDrawing.iterator();
                        while (it.hasNext()) {
                            ArrayList<PointF> arc = it.next();
                            if (arc.size() >= 2) {
                                Iterator<PointF> iit = arc.iterator();
                                p = iit.next();
                                float mX = p.x * scale;
                                float mY = p.y * scale;
                                path.moveTo(mX, mY);
                                while (iit.hasNext()) {
                                    p = iit.next();
                                    float x = p.x * scale;
                                    float y = p.y * scale;
                                    path.quadTo(mX, mY, (x + mX) / 2, (y + mY) / 2);
                                    mX = x;
                                    mY = y;
                                }
                                path.lineTo(mX, mY);
                            } else {
                                p = arc.get(0);
                                canvas.drawCircle(p.x * scale, p.y * scale, INK_THICKNESS * scale / 2, paint);
                            }
                        }

                        paint.setStyle(Paint.Style.STROKE);
                        canvas.drawPath(path, paint);
                    }
                }
            };

            addView(mSearchView);
        }
        requestLayout();
    }

    public static Bitmap drawBg4Bitmap(int color, Bitmap orginBitmap) {
        Paint paint = new Paint();
        paint.setColor(color);
        Bitmap bitmap = Bitmap.createBitmap(orginBitmap.getWidth(),
                orginBitmap.getHeight(), orginBitmap.getConfig());
        Canvas canvas = new Canvas(bitmap);
        canvas.drawRect(0, 0, orginBitmap.getWidth(), orginBitmap.getHeight(), paint);
        canvas.drawBitmap(orginBitmap, 0, 0, paint);
        return bitmap;
    }

    /**
     * 设置搜索内容
     *
     * @param searchBoxes
     */
    public void setSearchBoxes(RectF searchBoxes[]) {
        mSearchBoxes = searchBoxes;
        if (mSearchView != null)
            mSearchView.invalidate();
    }

    /**
     * 取消选择
     */
    public void deselectText() {
        mSelectBox = null;
        mSearchView.invalidate();
    }

    /**
     * 选择文字
     *
     * @param x0
     * @param y0
     * @param x1
     * @param y1
     */
    public void selectText(float x0, float y0, float x1, float y1) {
        LogUtils.d(TAG, "selectText x0=" + x0 + " y0=" + y0 + " x1=" + x1 + " y1=" + y1);
        float scale = mSourceScale * (float) getWidth() / (float) mSize.x;
        float docRelX0 = (x0 - getLeft()) / scale;
        float docRelY0 = (y0 - getTop()) / scale;
        float docRelX1 = (x1 - getLeft()) / scale;
        float docRelY1 = (y1 - getTop()) / scale;
        // Order on Y but maintain the point grouping
        if (docRelY0 <= docRelY1)
            mSelectBox = new RectF(docRelX0, docRelY0, docRelX1, docRelY1);
        else
            mSelectBox = new RectF(docRelX1, docRelY1, docRelX0, docRelY0);
        LogUtils.d(TAG, "selectText mSelectBox=left=" + mSelectBox.left + " top=" + mSelectBox.top + " right=" + mSelectBox.right + " bottom=" + mSelectBox.bottom);
        mSearchView.invalidate();

        //此处取出的是一页的内容
        if (mGetText == null) {
            mGetText = new AsyncTask<Void, Void, TextWord[][]>() {
                @Override
                protected TextWord[][] doInBackground(Void... params) {
                    return getText();
                }

                @Override
                protected void onPostExecute(TextWord[][] result) {
                    mText = result;
                    LogUtils.d(TAG, "result == null" + (result == null)+" mText.length="+mText.length);
                    //TODO result不为空，并且长度大于１，认为是流式pdf,可以解析到内容
                    if (result != null && mText.length > 0) {
                        LogUtils.d(TAG, "选中的文字=" + mText[0][0].w);
                    }
                    mSearchView.invalidate();
                }
            };
            mGetText.execute();
        }
    }

    /***
     * 开始墨迹绘制
     * @param x
     * @param y
     */
    public void startDraw(float x, float y) {
        float scale = mSourceScale * (float) getWidth() / (float) mSize.x;
        float docRelX = (x - getLeft()) / scale;
        float docRelY = (y - getTop()) / scale;
        if (mDrawing == null)
            mDrawing = new ArrayList<ArrayList<PointF>>();

        ArrayList<PointF> arc = new ArrayList<PointF>();
        arc.add(new PointF(docRelX, docRelY));
        mDrawing.add(arc);
        mSearchView.invalidate();
    }

    /***
     * 继续墨迹
     * @param x
     * @param y
     */
    public void continueDraw(float x, float y) {
        float scale = mSourceScale * (float) getWidth() / (float) mSize.x;
        float docRelX = (x - getLeft()) / scale;
        float docRelY = (y - getTop()) / scale;

        if (mDrawing != null && mDrawing.size() > 0) {
            ArrayList<PointF> arc = mDrawing.get(mDrawing.size() - 1);
            arc.add(new PointF(docRelX, docRelY));
            mSearchView.invalidate();
        }
    }

    /**
     * 取消墨迹
     */
    public void cancelDraw() {
        mDrawing = null;
        mSearchView.invalidate();
    }

    protected void processSelectedText(TextSelector.TextProcessor tp) {
        (new TextSelector(mText, mSelectBox)).select(tp);
    }

    /**
     * 删除框
     */
    public void setItemSelectBox() {
        if (mSearchView != null)
            mSearchView.invalidate();
    }

    /**
     * 第一次在ReadView调用了getOrCreateChild，LayoutParms用wrap_content,MeasureSpec用的是MeasureSpec.AT_MOST
     *
     * @param widthMeasureSpec
     * @param heightMeasureSpec
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int x, y;
        switch (MeasureSpec.getMode(widthMeasureSpec)) {
            case MeasureSpec.UNSPECIFIED:
                x = mSize.x;
                break;
            default:
                x = MeasureSpec.getSize(widthMeasureSpec);
        }
        switch (MeasureSpec.getMode(heightMeasureSpec)) {
            case MeasureSpec.UNSPECIFIED:

                y = mSize.y;
                break;
            default:
                y = MeasureSpec.getSize(heightMeasureSpec);
        }

        setMeasuredDimension(x, y);
        LogUtils.d(TAG, "onMeasure width=" + getMeasuredWidth() + " height=" + getMeasuredHeight());
        if (mBusyIndicator != null) {
            int limit = Math.min(mParentSize.x, mParentSize.y) / 2;
            mBusyIndicator.measure(MeasureSpec.AT_MOST | limit, MeasureSpec.AT_MOST | limit);
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        int w = right - left;
        int h = bottom - top;
        if (mEntire != null) {
            if (mEntire.getWidth() != w || mEntire.getHeight() != h) {
                mEntireMat.setScale(w / (float) mSize.x, h / (float) mSize.y);
                mEntire.setImageMatrix(mEntireMat);
                mEntire.invalidate();
            }
            mEntire.layout(0, 0, w, h);
        }

        if (mSearchView != null) {
            mSearchView.layout(0, 0, w, h);
        }

        if (mBusyIndicator != null) {
            int bw = mBusyIndicator.getMeasuredWidth();
            int bh = mBusyIndicator.getMeasuredHeight();

            mBusyIndicator.layout((w - bw) / 2, (h - bh) / 2, (w + bw) / 2, (h + bh) / 2);
        }
    }

    @Override
    public boolean isOpaque() {
        return true;
    }
}
