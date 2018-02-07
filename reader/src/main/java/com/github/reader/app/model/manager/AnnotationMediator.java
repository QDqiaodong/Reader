package com.github.reader.app.model.manager;

import android.content.ClipData;
import android.content.Context;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.os.Build;
import android.util.Log;
import android.util.SparseArray;

import com.github.reader.app.model.entity.AnnotationPointsBean;
import com.github.reader.app.model.entity.BaseAnnotation;
import com.github.reader.app.model.entity.DocumentBean;
import com.github.reader.app.model.entity.InkPointsBean;
import com.github.reader.utils.LogUtils;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by qiaodong on 17-12-14.
 */

public class AnnotationMediator {
    private static final String TAG = AnnotationMediator.class.getSimpleName();
    private static AnnotationMediator instance;
    private Gson mGson;
    private AnnotationDBManager mDbManager;
    private Context mContext;

    private AnnotationMediator(Context context) {
        mContext = context;
        mGson = new Gson();
        mDbManager = AnnotationDBManager.getInstance();
    }

    public static AnnotationMediator getInstance(Context context) {
        if (instance == null) {
            instance = new AnnotationMediator(context);
        }
        return instance;
    }

    /**
     * 1.存储一块高亮区域中每行文字的rect
     * 2.存储当前整个高亮区域对应的rect(即以后的删除区域)
     *
     * @param annLists 一块区域由多行文字构成,annLists存储了多行文字对应的真实文档中对应Rect
     * @param curPage
     * @param type     类型(高亮,删除线,下划线)
     */
    public void saveDocAnnPoints(ArrayList<RectF> annLists, int curPage, BaseAnnotation.Type type) {
        LogUtils.d(TAG, "saveDocAnnPoints:curPage=" + curPage + " type=" + type.ordinal());

        AnnotationPointsBean textBean = new AnnotationPointsBean(annLists);
        String annResult = mGson.toJson(textBean);
        RectF deleteRect = new RectF();
        for (int i = 0; i < annLists.size(); i++) {
            RectF rectF = annLists.get(i);
            deleteRect.union(rectF);
        }
        String deleteResult = mGson.toJson(deleteRect);
        mDbManager.setCurrDocAnnPoints(curPage, type, annResult, deleteResult);
    }

    /**
     * 1.存储当前页面的墨迹点
     * 2.存储每块墨迹点区域Rect
     *
     * @param curPage
     * @param drawlists
     */
    public void saveDocInkPoints(int curPage, ArrayList<ArrayList<PointF>> drawlists) {
        LogUtils.d(TAG, "saveDocInkPoints: curPage" + curPage + " drawlists.size=" + drawlists.size());

        for (int i = 0; i < drawlists.size(); i++) {
            ArrayList<PointF> arc = drawlists.get(i);
            InkPointsBean inkPointsBean = new InkPointsBean(arc);
            String result = mGson.toJson(inkPointsBean);

            RectF rect = calculateInkRect(arc);
            String deleteResult = mGson.toJson(rect);
            mDbManager.setCurrDocInksPoints(curPage, result, deleteResult);
        }
    }

    /**
     * 获取当前文档中的注释的区域
     *
     * @param currentPage 　　当前页面
     * @param type        　　　　　　类型
     * @return
     */
    public ArrayList<AnnotationPointsBean> getCurrDocAnnPoints(int currentPage, BaseAnnotation.Type type) {
        LogUtils.d(TAG, "getCurrDocAnnPoints currentPage=" + currentPage + " type=" + type);

        List<DocumentBean> documentBeans = mDbManager.getDocAnnPoints(currentPage, type);
        ArrayList<AnnotationPointsBean> lists = new ArrayList<>();
        if (documentBeans != null && documentBeans.size() > 0) {
            for (DocumentBean bean : documentBeans) {
                String annotationPoints = bean.getAnnotationPoints();
                AnnotationPointsBean quads = mGson.fromJson(annotationPoints, AnnotationPointsBean.class);
                lists.add(quads);
            }
            return lists;
        } else {
            LogUtils.d(TAG, "end loadAnnotation　数据不存在");
            return null;
        }
    }

    /**
     * 获取当前文档中的墨迹
     *
     * @param currentPage
     * @return
     */
    public ArrayList<InkPointsBean> getCurrDocInkPoints(int currentPage) {
        List<DocumentBean> documentBeans = mDbManager.getDocAnnPoints(currentPage, BaseAnnotation.Type.Ink);
        ArrayList<InkPointsBean> lists = new ArrayList<>();
        if (documentBeans != null && documentBeans.size() > 0) {
            for (DocumentBean bean : documentBeans) {
                String inkPoints = bean.getInkPoints();
                Gson gson = new Gson();
                InkPointsBean quads = gson.fromJson(inkPoints, InkPointsBean.class);
                lists.add(quads);
            }
            LogUtils.d(TAG, "getCurrDocInkPoints 数据list.size=" + lists.size());
            return lists;
        } else {
            LogUtils.d(TAG, "getCurrDocInkPoints　数据不存在");
            return null;
        }
    }

    /**
     * 获取当前页注释区域的删除框的集合(高亮,下划线,删除线,墨迹)
     *
     * @param currentPage
     * @return
     */
    public SparseArray<RectF> getCurrDocDeleteRect(int currentPage, BaseAnnotation.Type type) {
        LogUtils.d(TAG, "getCurrDocAnnDeleteRect: 获取当前页注释区域的删除框的集合 currentPage=" + currentPage + " type=" + type.ordinal());
        List<DocumentBean> documentBeans = mDbManager.getDocAnnPoints(currentPage, type);
        SparseArray<RectF> deleteRects = new SparseArray<>();
        if (documentBeans != null && documentBeans.size() > 0) {
            for (DocumentBean bean : documentBeans) {
                String deleteRect = bean.getDeleteRect();
                RectF rectF = mGson.fromJson(deleteRect, RectF.class);
                deleteRects.append(new Long(bean.getId()).intValue(), rectF);
            }
        }
        return deleteRects;
    }

    /**
     * 计算当前墨迹点,对应的区域
     *
     * @param lists
     * @return
     */
    public RectF calculateInkRect(ArrayList<PointF> lists) {
        if (lists == null || (lists != null) && lists.size() == 0) {
            return null;
        }
        PointF p;
        Path path = new Path();
        if (lists.size() >= 2) {
            Iterator<PointF> iit = lists.iterator();
            p = iit.next();
            float mX = p.x;
            float mY = p.y;
            path.moveTo(mX, mY);
            while (iit.hasNext()) {
                p = iit.next();
                float x = p.x;
                float y = p.y;
                path.quadTo(mX, mY, (x + mX) / 2, (y + mY) / 2);
                mX = x;
                mY = y;
            }
            path.lineTo(mX, mY);
        } else {
            p = lists.get(0);
            return new RectF(p.x, p.y, p.x, p.y);
        }

        RectF rect = new RectF();
        path.computeBounds(rect, false);
        return rect;
    }

    public void loadDeletRects(int curPage, BaseAnnotation.Type type, ArrayList<BaseAnnotation> baseAnnoLists) {
        SparseArray<RectF> deleteRects;
        deleteRects = getCurrDocDeleteRect(curPage, type);

        if (deleteRects != null && deleteRects.size() > 0) {
            for (int i = 0; i < deleteRects.size(); i++) {
                RectF rectF = deleteRects.valueAt(i);
                int index = deleteRects.keyAt(i);
                BaseAnnotation annotation = new BaseAnnotation(rectF.left, rectF.top, rectF.right, rectF.bottom, type.ordinal(), index);
                baseAnnoLists.add(annotation);
                Log.d(TAG, "loadHighlights: rectF.left=" + rectF.left + " top=" + rectF.top + " right=" + rectF.right + " bottom=" + rectF.bottom + " index=" + index + " type=" + type.ordinal());
            }
        }
    }

    /**
     * 复制文本
     *
     * @param text
     */
    public boolean copyText(StringBuilder text) {
        if (text.length() == 0) {
            return false;
        }

        int currentApiVersion = Build.VERSION.SDK_INT;
        if (currentApiVersion >= Build.VERSION_CODES.HONEYCOMB) {
            android.content.ClipboardManager cm = (android.content.ClipboardManager) mContext.getSystemService(Context.CLIPBOARD_SERVICE);

            cm.setPrimaryClip(ClipData.newPlainText("MuPDF", text));
        } else {
            android.text.ClipboardManager cm = (android.text.ClipboardManager) mContext.getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setText(text);
        }
        return true;
    }
}
