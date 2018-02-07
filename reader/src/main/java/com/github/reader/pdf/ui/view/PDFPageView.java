package com.github.reader.pdf.ui.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;
import android.widget.ImageView;

import com.github.reader.R;
import com.github.reader.app.model.entity.AnnotationPointsBean;
import com.github.reader.app.model.entity.BaseAnnotation;
import com.github.reader.app.model.entity.DocumentBean;
import com.github.reader.app.model.entity.InkPointsBean;
import com.github.reader.app.model.manager.AnnotationMediator;
import com.github.reader.app.model.manager.DBManager;
import com.github.reader.app.model.manager.TextSelector;
import com.github.reader.app.ui.view.BasePageView;
import com.github.reader.app.ui.view.IBaseDocView;
import com.github.reader.app.ui.view.OpaqueImageView;
import com.github.reader.pdf.model.Annotation;
import com.github.reader.pdf.model.AsyncTask;
import com.github.reader.pdf.model.CancellableAsyncTask;
import com.github.reader.pdf.model.CancellableTaskDefinition;
import com.github.reader.pdf.model.Hit;
import com.github.reader.pdf.model.MuPDFCancellableTaskDefinition;
import com.github.reader.pdf.model.MuPDFCore;
import com.github.reader.pdf.model.PassClickResult;
import com.github.reader.pdf.model.TextWord;
import com.github.reader.utils.LogUtils;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * 每一页对应的view
 */
public class PDFPageView extends BasePageView implements IBaseDocView {
    private static final String TAG = "PDFPageView";
    private final MuPDFCore mCore;
    private int mSelectedAnnotationIndex = -1;

    private Point mPatchViewSize; // View size on the basis of which the patch was created
    private Rect mPatchArea;
    private ImageView mPatch;
    private Bitmap mPatchBm;

    private CancellableAsyncTask<Void, Void> mDrawPatch;
    private AsyncTask<Void, Void, Annotation[]> mLoadAnnotations;
    private AsyncTask<Void, Void, Void> mAddInk;
    private AsyncTask<Void, Void, Void> mSelectTask;
    private AsyncTask<Void, Void, Void> mDeleteAnnotation;
    private AsyncTask<Void, Void, PassClickResult> mPassClick;
    /**
     * 表示高亮,下划线,删除线,墨迹,所在的删除框的集合
     */
    private ArrayList<BaseAnnotation> baseAnnoLists = new ArrayList<>();
    private ArrayList<AnnotationPointsBean> mHighlightLists;
    private ArrayList<AnnotationPointsBean> mUnderlineLists;
    private ArrayList<AnnotationPointsBean> mStrikeOutLists;
    private ArrayList<InkPointsBean> mInkLists;

    public PDFPageView(Context c, MuPDFCore core, Point parentSize, Bitmap sharedHqBm) {
        super(c, parentSize);
        mCore = core;
        mPatchBm = drawBg4Bitmap(getResources().getColor(R.color.button_pressed),sharedHqBm);
    }

    @Override
    protected void reintBgTask() {
        if (mDrawPatch != null) {
            mDrawPatch.cancelAndWait();
            mDrawPatch = null;
        }
        if (mPatch != null) {
            mPatch.setImageBitmap(null);
            mPatch.invalidate();
        }

        if (mAddInk != null) {
            mAddInk.cancel(true);
            mAddInk = null;
        }

        if (mLoadAnnotations != null) {
            mLoadAnnotations.cancel(true);
            mLoadAnnotations = null;
        }

        if (mSelectTask != null) {
            mSelectTask.cancel(true);
            mSelectTask = null;
        }

        if (mDeleteAnnotation != null) {
            mDeleteAnnotation.cancel(true);
            mDeleteAnnotation = null;
        }

        if (mPassClick != null) {
            mPassClick.cancel(true);
            mPassClick = null;
        }

        mPatchViewSize = null;
        mPatchArea = null;

        baseAnnoLists.clear();
    }

    @Override
    public void blank(int page) {
        super.blank(page);
    }

    @Override
    public void setPage(final int page, PointF size) {
        mPageNumber = page;
        loadAnnotations();
        super.setPage(page, size);
    }

    @Override
    public int getPage() {
        return mPageNumber;
    }

    /**
     * 加载当前页相关的批注,点击事件使用
     */
    @Override
    public void loadAnnotations() {
        baseAnnoLists.clear();
        loadHighlights();
        loadUnderlines();
        loadStrikeOuts();
        loadInks();
        Log.d(TAG, "loadAnnotations: 加载高亮和墨迹的删除区域后的size" + baseAnnoLists.size());
        if (mSearchView != null) {
            mSearchView.invalidate();
        }
    }

    /**
     * 单击屏幕时
     *
     * @param x
     * @param y
     * @return
     */
    @Override
    public Hit passClickEvent(float x, float y) {
        LogUtils.d(TAG, "passClickEvent x=" + x + " y=" + y);
        float scale = mSourceScale * (float) getWidth() / (float) mSize.x;
        final float docRelX = (x - getLeft()) / scale;
        final float docRelY = (y - getTop()) / scale;
        boolean hit = false;
        int i = 0;

        if (baseAnnoLists != null) {
            for (i = 0; i < baseAnnoLists.size(); i++) {
                BaseAnnotation baseAnnotation = baseAnnoLists.get(i);
                if (baseAnnotation.contains(docRelX, docRelY)) {
                    hit = true;
                    break;
                }
            }
        }
        if (hit) {
            switch (baseAnnoLists.get(i).type) {
                case Highlight:
                case StrikeOut:
                case Underline:
                case Ink:
                    mSelectedAnnotationIndex = i;
                    Log.d(TAG, "passClickEvent: mSelectedAnnotationIndex=" + mSelectedAnnotationIndex + " type=" + baseAnnoLists.get(i).type);
                    setItemSelectBox();
                    return Hit.Annotation;
            }
        }

        mSelectedAnnotationIndex = -1;
        setItemSelectBox();
        return Hit.Nothing;
    }

    /**
     * 复制文本
     *
     * @return
     */
    @TargetApi(11)
    public boolean copySelection() {
        final StringBuilder text = new StringBuilder();

        processSelectedText(new TextSelector.TextProcessor() {
            StringBuilder line;

            public void onStartLine() {
                line = new StringBuilder();
            }

            public void onWord(TextWord word) {
                if (line.length() > 0)
                    line.append(' ');
                line.append(word.w);
                LogUtils.d(TAG, "copySelection onWord line.toString=" + line.toString() + " word.w=" + word.w);
            }

            public void onEndLine() {
                if (text.length() > 0)
                    text.append('\n');
                text.append(line);
            }
        });

        if (!AnnotationMediator.getInstance(mContext).copyText(text)) {
            return false;
        }

        deselectText();

        return true;
    }

    /**
     * 标记选择　(高亮，下划线，删除线)
     *
     * @param type
     * @return
     */
    @Override
    public boolean markupSelection(final BaseAnnotation.Type type) {
        LogUtils.d(TAG, "markupSelection...type=" + type.ordinal()+" page="+getPage());
        final ArrayList<RectF> quadPointsQD = new ArrayList<RectF>();
        processSelectedText(new TextSelector.TextProcessor() {
            RectF rect;

            public void onStartLine() {
                rect = new RectF();
            }

            public void onWord(TextWord word) {
                rect.union(word);
            }

            //一行文字，所对应的四个角的坐标点
            public void onEndLine() {
                if (!rect.isEmpty()) {
                    Log.d(TAG, "onEndLine: rect.left="+rect.left+" rect.top="+rect.top+" rect.right="+rect.right+" rect.bottom="+rect.bottom);
                    quadPointsQD.add(new RectF(rect.left, rect.top, rect.right, rect.bottom));
                }
            }
        });

        mSelectTask = new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... voids) {
                Log.d(TAG, "markupSelection doInBackground: quadPointsQD.size="+quadPointsQD.size()+" type="+type.ordinal());
                AnnotationMediator.getInstance(getContext()).saveDocAnnPoints(quadPointsQD, getPage(), type);
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                loadAnnotations();
                update();
            }
        };
        mSelectTask.execute();
        deselectText();
        return true;
    }

    /**
     * 删除选中的注释
     */
    @Override
    public void deleteSelectedAnnotation() {
        if (mSelectedAnnotationIndex != -1) {
            if (mDeleteAnnotation != null)
                mDeleteAnnotation.cancel(true);

            Log.d(TAG, "deleteSelectedAnnotation: 删除注释 mSelectedAnnotationIndex=" + mSelectedAnnotationIndex + " 删除之前的size=" + baseAnnoLists.size());
            mDeleteAnnotation = new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... voids) {
                    BaseAnnotation annotation = baseAnnoLists.get(mSelectedAnnotationIndex);
                    if (annotation != null) {
                        int index = annotation.index;
                        DocumentBean documentBean = new DocumentBean();
                        documentBean.setId((long) index);
                        DBManager.getInstance().getSession().delete(documentBean);
                        baseAnnoLists.remove(mSelectedAnnotationIndex);
                        Log.d(TAG, "deleteSelectedAnnotation: index=" + index + " 删除后的size=" + baseAnnoLists.size());
                    }
                    return null;
                }

                @Override
                protected void onPostExecute(Void aVoid) {
                    super.onPostExecute(aVoid);
                    loadAnnotations();
                    mSelectedAnnotationIndex = -1;
                    setItemSelectBox();
                }
            };
            mDeleteAnnotation.execute();
        }
    }

    /**
     * 取消选择框
     */
    @Override
    public void deselectAnnotation() {
        mSelectedAnnotationIndex = -1;
        setItemSelectBox();
    }

    /**
     * 保存墨迹
     *
     * @return
     */
    @Override
    public boolean saveDraw() {
        if (mDrawing == null)
            return false;

        for (int i = 0; i < mDrawing.size(); i++) {
            ArrayList<PointF> arc = mDrawing.get(i);
            if (arc.size() < 2) {
                mDrawing.remove(arc);
                Log.d(TAG, "getDraw: 不保存一个点的墨迹");
            }
        }

        if (mDrawing.size() <= 0)
            return false;

        if (mAddInk != null) {
            mAddInk.cancel(true);
            mAddInk = null;
        }

        mAddInk = new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... voids) {
                AnnotationMediator.getInstance(getContext()).saveDocInkPoints(getPage(), mDrawing);
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                cancelDraw();
                loadAnnotations();
            }
        };
        mAddInk.execute();
        return true;
    }

    @Override
    public TextWord[][] getText() {
        return mCore.textLines(mPageNumber);
    }


    /**
     * 加载当前页所有的高亮rect
     */
    public void loadHighlights() {
        ArrayList<AnnotationPointsBean> list = AnnotationMediator.getInstance(getContext()).getCurrDocAnnPoints(getPage(), BaseAnnotation.Type.Highlight);
        AnnotationMediator.getInstance(getContext()).loadDeletRects(getPage(), BaseAnnotation.Type.Highlight, baseAnnoLists);
        if (list != null) {
            mHighlightLists = list;
        } else {
            mHighlightLists = null;
            LogUtils.d(TAG, "loadAnnotations 没有搜索到高亮文本");
        }
    }

    public void loadUnderlines() {
        ArrayList<AnnotationPointsBean> list = AnnotationMediator.getInstance(getContext()).getCurrDocAnnPoints(getPage(), BaseAnnotation.Type.Underline);
        AnnotationMediator.getInstance(getContext()).loadDeletRects(getPage(), BaseAnnotation.Type.Underline, baseAnnoLists);
        if (list != null) {
            mUnderlineLists = list;
        } else {
            mUnderlineLists = null;
            LogUtils.d(TAG, "loadAnnotations 没有搜索到下划线");
        }
    }

    public void loadStrikeOuts() {
        ArrayList<AnnotationPointsBean> list = AnnotationMediator.getInstance(getContext()).getCurrDocAnnPoints(getPage(), BaseAnnotation.Type.StrikeOut);
        AnnotationMediator.getInstance(getContext()).loadDeletRects(getPage(), BaseAnnotation.Type.StrikeOut, baseAnnoLists);
        if (list != null) {
            mStrikeOutLists = list;
        } else {
            mStrikeOutLists = null;
            LogUtils.d(TAG, "loadAnnotations 没有搜索到删除线");
        }
    }


    /**
     * 加载所有的墨迹点
     */
    @Override
    public void loadInks() {
        ArrayList<InkPointsBean> list = AnnotationMediator.getInstance(getContext()).getCurrDocInkPoints(getPage());
        AnnotationMediator.getInstance(mContext).loadDeletRects(getPage(), BaseAnnotation.Type.Ink, baseAnnoLists);

        if (list != null) {
            mInkLists = list;
        } else {
            mInkLists = null;
            LogUtils.d(TAG, "loadAnnotations 没有搜索到墨迹");
        }
    }

    @Override
    protected void drawHighlightRect(float scale, Canvas canvas, Paint paint) {
        LogUtils.d(TAG, "drawHighlightRect　page=" + getPage()+" scale="+scale);
        if (mHighlightLists != null && mHighlightLists.size() > 0) {
            LogUtils.d(TAG, "drawHighlightRect mHighlightLists.size()="+mHighlightLists.size());

            for (AnnotationPointsBean point : mHighlightLists) {
                ArrayList<RectF> lists = point.getLists();
                for (int i = 0; i < lists.size(); i++) {
                    RectF rect = lists.get(i);
                    paint.setStyle(Paint.Style.FILL); //实心效果
                    paint.setColor(getResources().getColor(R.color.my_highlight_color));
                    if (!rect.isEmpty())
                        canvas.drawRect(rect.left * scale, rect.top * scale, rect.right * scale, rect.bottom * scale, paint);
                }
            }
            Log.d(TAG, "drawHighlightRect: 当前删除框集合的大小size=" + baseAnnoLists.size());
        }
    }

    @Override
    protected void drawUnderline(float scale, Canvas canvas, Paint paint) {
        LogUtils.d(TAG, "drawUnderline　page=" + getPage()+" scale="+scale);
        if (mUnderlineLists != null && mUnderlineLists.size() > 0) {
            LogUtils.d(TAG, "drawUnderline mUnderlineLists.size()="+mUnderlineLists.size());

            for (AnnotationPointsBean point : mUnderlineLists) {
                ArrayList<RectF> lists = point.getLists();
                for (int i = 0; i < lists.size(); i++) {
                    RectF rect = lists.get(i);
                    paint.setStrokeWidth(3.0f * scale);//设置笔刷的粗细度
                    paint.setStyle(Paint.Style.FILL); //实心效果
                    paint.setColor(getResources().getColor(R.color.my_underline_color));
                    if (!rect.isEmpty()){
                        Log.d(TAG, "drawUnderline: rect.left="+rect.left+" rect.top="+rect.top+" rect.right="+rect.right+" bottom="+rect.bottom);
                        canvas.drawLine(rect.left* scale,rect.bottom* scale,rect.right* scale,rect.bottom*scale,paint);
                    }

                }
            }
            Log.d(TAG, "drawUnderline: 当前删除框集合的大小size=" + baseAnnoLists.size());
        }
    }

    @Override
    protected void drawStrikeOut(float scale, Canvas canvas, Paint paint) {
        LogUtils.d(TAG, "drawStrikeOut　page=" + getPage()+" scale="+scale);
        if (mStrikeOutLists != null && mStrikeOutLists.size() > 0) {
            LogUtils.d(TAG, "drawStrikeOut mStrikeOutLists.size()="+mStrikeOutLists.size());

            for (AnnotationPointsBean point : mStrikeOutLists) {
                ArrayList<RectF> lists = point.getLists();
                for (int i = 0; i < lists.size(); i++) {
                    RectF rect = lists.get(i);
                    paint.setStrokeWidth(3.0f * scale);//设置笔刷的粗细度
                    paint.setStyle(Paint.Style.FILL); //实心效果
                    paint.setColor(getResources().getColor(R.color.my_strikeout_color));
                    if (!rect.isEmpty()){
                        Log.d(TAG, "drawStrikeOut: rect.left="+rect.left+" rect.top="+rect.top+" rect.right="+rect.right+" bottom="+rect.bottom);
                        canvas.drawLine(rect.left*scale,(rect.bottom+rect.top)/2*scale,rect.right*scale,(rect.bottom+rect.top)/2*scale,paint);
                    }

                }
            }
            Log.d(TAG, "drawUnderline: 当前删除框集合的大小size=" + baseAnnoLists.size());
        }
    }

    @Override
    protected void drawInkRect(float scale, Canvas canvas, Paint paint) {
        LogUtils.d(TAG, "drawInkRect　page=" + getPage());
        if (mInkLists != null) {
            Path path = new Path();
            PointF p;
            LogUtils.d(TAG, "mDrawing != null ");
            paint.setAntiAlias(true);
            paint.setDither(true); //设定是否使用图像抖动处理，会使绘制出来的图片颜色更加平滑和饱满，图像更加清晰
            paint.setStrokeJoin(Paint.Join.ROUND); //设置绘制时各图形的结合方式，如平滑效果等
            paint.setStrokeCap(Paint.Cap.ROUND);//设置笔刷的图形样式，如圆形样式

            paint.setStyle(Paint.Style.FILL);
            paint.setStrokeWidth(INK_THICKNESS * scale);//设置笔刷的粗细度
            paint.setColor(getResources().getColor(R.color.seek_thumb));

            Iterator<InkPointsBean> it = mInkLists.iterator();
            while (it.hasNext()) {
                //1.
                path.reset();
                paint.setColor(getResources().getColor(R.color.seek_thumb));
                paint.setStyle(Paint.Style.FILL);

                //2.
                ArrayList<PointF> arc = it.next().getLists();
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
                //3.
                paint.setStyle(Paint.Style.STROKE);
                canvas.drawPath(path, paint);
            }
            Log.d(TAG, "drawInkRect: 当前删除框集合的大小size=" + baseAnnoLists.size());
        }
    }

    @Override
    protected void drawDeleteRect(float scale, Canvas canvas, Paint paint) {
        Log.d(TAG, "drawDeleteRect: mSelectedAnnotationIndex=" + mSelectedAnnotationIndex + " size=" + baseAnnoLists.size());
        if (mSelectedAnnotationIndex != -1) {
            if (baseAnnoLists != null) {
                BaseAnnotation baseAnnotation = baseAnnoLists.get(mSelectedAnnotationIndex);
                if (baseAnnotation == null) {
                    return;
                }
                paint.setStyle(Paint.Style.STROKE); //空心效果
                paint.setColor(getResources().getColor(R.color.ink_color));
                Log.d(TAG, "drawDeleteRect: left=" + baseAnnotation.left + " top=" + baseAnnotation.top + " right=" + baseAnnotation.right + " bottom=" + baseAnnotation.bottom);
                canvas.drawRect(baseAnnotation.left * scale, baseAnnotation.top * scale, baseAnnotation.right * scale, baseAnnotation.bottom * scale, paint);
            }
        }
    }

    @Override
    public void update() {
        // Cancel pending render task
        if (mDrawEntire != null) {
            mDrawEntire.cancelAndWait();
            mDrawEntire = null;
        }

        if (mDrawPatch != null) {
            mDrawPatch.cancelAndWait();
            mDrawPatch = null;
        }

        // Render the page in the background
        mDrawEntire = new CancellableAsyncTask<Void, Void>(getUpdatePageTask(mEntireBm, mSize.x, mSize.y, 0, 0, mSize.x, mSize.y)) {

            public void onPostExecute(Void result) {
                mEntire.setImageBitmap(mEntireBm);
                mEntire.invalidate();
            }
        };

        mDrawEntire.execute();

        updateHq(true);
    }

    @Override
    public void updateHq(boolean update) {
        Rect viewArea = new Rect(getLeft(), getTop(), getRight(), getBottom());
        if (viewArea.width() == mSize.x || viewArea.height() == mSize.y) {
            // If the viewArea's size matches the unzoomed size, there is no need for an hq patch
            if (mPatch != null) {
                mPatch.setImageBitmap(null);
                mPatch.invalidate();
            }
        } else {
            //当前Pageview的实际大小
            final Point patchViewSize = new Point(viewArea.width(), viewArea.height());
            //表示实际要显示pdf的区域,也就是pdf与屏幕的重叠处
            final Rect patchArea = new Rect(0, 0, mParentSize.x, mParentSize.y);

            // Intersect and test that there is an intersection
            if (!patchArea.intersect(viewArea)) {
                return;
            }

            //重新计算重叠处的坐标，这个坐标是相对于当前PDF实际大小
            // Offset patch area to be relative to the view top left
            patchArea.offset(-viewArea.left, -viewArea.top);
            boolean area_unchanged = patchArea.equals(mPatchArea) && patchViewSize.equals(mPatchViewSize);

            // If being asked for the same area as last time and not because of an update then nothing to do
            if (area_unchanged && !update)
                return;
            boolean completeRedraw = !(area_unchanged && update);
            // Stop the drawing of previous patch if still going
            if (mDrawPatch != null) {
                mDrawPatch.cancelAndWait();
                mDrawPatch = null;
            }

            // Create and add the image view if not already done
            if (mPatch == null) {
                mPatch = new OpaqueImageView(mContext);
                mPatch.setScaleType(ImageView.ScaleType.MATRIX);
                addView(mPatch);
                mSearchView.bringToFront();
            }

            CancellableTaskDefinition<Void, Void> task;

            if (completeRedraw) {
                task = getDrawPageTask(mPatchBm, patchViewSize.x, patchViewSize.y,
                        patchArea.left, patchArea.top,
                        patchArea.width(), patchArea.height());
            } else {
                task = getUpdatePageTask(mPatchBm, patchViewSize.x, patchViewSize.y,
                        patchArea.left, patchArea.top,
                        patchArea.width(), patchArea.height());
            }

            mDrawPatch = new CancellableAsyncTask<Void, Void>(task) {

                public void onPostExecute(Void result) {
                    mPatchViewSize = patchViewSize;
                    mPatchArea = patchArea;
                    mPatch.setImageBitmap(mPatchBm);
                    mPatch.invalidate();
                    //requestLayout();
                    // Calling requestLayout here doesn't lead to a later call to layout. No idea
                    // why, but apparently others have run into the problem.
                    mPatch.layout(mPatchArea.left, mPatchArea.top, mPatchArea.right, mPatchArea.bottom);
                }
            };

            mDrawPatch.execute();
        }
    }

    @Override
    public void removeHq() {
        LogUtils.d(TAG, TAG + " removeHq");
        // Stop the drawing of the patch if still going
        if (mDrawPatch != null) {
            mDrawPatch.cancelAndWait();
            mDrawPatch = null;
        }

        // And get rid of it
        mPatchViewSize = null;
        mPatchArea = null;
        if (mPatch != null) {
            mPatch.setImageBitmap(null);
            mPatch.invalidate();
        }
    }


    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        int w = right - left;
        int h = bottom - top;
        if (mPatchViewSize != null) {
            if (mPatchViewSize.x != w || mPatchViewSize.y != h) {
                // Zoomed since patch was created
                mPatchViewSize = null;
                mPatchArea = null;
                if (mPatch != null) {
                    mPatch.setImageBitmap(null);
                    mPatch.invalidate();
                }
            } else {
                mPatch.layout(mPatchArea.left, mPatchArea.top, mPatchArea.right, mPatchArea.bottom);
            }
        }
    }

    /**
     * 真正绘制pdf内容
     *
     * @param bm          pdf要绘制的bitmap,画板一定要大于绘画,即此画板宽高必须大于sizeX,sizeY
     * @param sizeX       要绘制的pdf的宽
     * @param sizeY       要绘制的pdf的高
     * @param patchX      展示在界面上的区域,在整张pdf中的x坐标的位置
     * @param patchY      展示在界面上的区域,在整张pdf中的y坐标的位置
     * @param patchWidth  展示在界面上的区域的宽度
     * @param patchHeight 展示在界面上的区域的高度
     * @return
     */
    @Override
    protected CancellableTaskDefinition<Void, Void> getDrawPageTask(final Bitmap bm, final int sizeX, final int sizeY,
                                                                    final int patchX, final int patchY, final int patchWidth, final int patchHeight) {
        LogUtils.d(TAG, "getDrawPageTask sizeX=" + sizeX + " sizeY=" + sizeY + " patchWidth=" + patchWidth + " patchHeight=" + patchHeight + " mPageNumber=" + mPageNumber);
        return new MuPDFCancellableTaskDefinition<Void, Void>(mCore) {
            @Override
            public Void doInBackground(MuPDFCore.Cookie cookie, Void... params) {
                // Workaround bug in Android Honeycomb 3.x, where the bitmap generation count
                // is not incremented when drawing.
              /*  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB &&
                        Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH)
                    bm.eraseColor(0);*/
                mCore.drawPage(bm, mPageNumber, sizeX, sizeY, patchX, patchY, patchWidth, patchHeight, cookie);
                return null;
            }
        };

    }


    protected CancellableTaskDefinition<Void, Void> getUpdatePageTask(final Bitmap bm, final int sizeX, final int sizeY,
                                                                      final int patchX, final int patchY, final int patchWidth, final int patchHeight) {
        LogUtils.d(TAG, "getUpdatePageTask sizeX=" + sizeX + " sizeY=" + sizeY + " patchWidth=" + patchWidth + " patchHeight=" + patchHeight + " mPageNumber=" + mPageNumber);
        return new MuPDFCancellableTaskDefinition<Void, Void>(mCore) {

            @Override
            public Void doInBackground(MuPDFCore.Cookie cookie, Void... params) {
                // Workaround bug in Android Honeycomb 3.x, where the bitmap generation count
                // is not incremented when drawing.
               /* if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB &&
                        Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH)
                    bm.eraseColor(0);*/
                mCore.updatePage(bm, mPageNumber, sizeX, sizeY, patchX, patchY, patchWidth, patchHeight, cookie);
                return null;
            }
        };
    }

    /**
     * 只是暂停一些任务，清空编辑的内容
     */
    @Override
    public void releaseResources() {
        if (mPassClick != null) {
            mPassClick.cancel(true);
            mPassClick = null;
        }

        if (mLoadAnnotations != null) {
            mLoadAnnotations.cancel(true);
            mLoadAnnotations = null;
        }

        if (mDeleteAnnotation != null) {
            mDeleteAnnotation.cancel(true);
            mDeleteAnnotation = null;
        }

        if (mSelectTask != null) {
            mSelectTask.cancel(true);
        }
        mSelectTask = null;

        mHighlightLists = null;
        mUnderlineLists = null;
        mStrikeOutLists = null;
        super.releaseResources();
    }

    @Override
    public void onDestory() {
        releaseBitmaps();
        if (mPatchBm != null)
            mPatchBm.recycle();
        mPatchBm = null;
    }
}
