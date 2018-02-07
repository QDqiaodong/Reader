package com.github.reader.app.model.manager;

import com.github.reader.app.model.entity.BaseAnnotation;
import com.github.reader.app.model.entity.DocumentBean;
import com.github.reader.app.model.entity.DocumentBeanDao;
import com.github.reader.utils.Constants;
import com.github.reader.utils.LogUtils;

import java.util.List;

/**
 * 复制,高亮,下滑线,删除线,墨迹的数据库管理类
 */

public class AnnotationDBManager {
    private static final String TAG = AnnotationDBManager.class.getSimpleName();
    private static AnnotationDBManager instance;

    private AnnotationDBManager() {
    }

    public static AnnotationDBManager getInstance() {
        if (instance == null) {
            instance = new AnnotationDBManager();
        }
        return instance;
    }

    /**
     * 保存当前页面的注释区域
     *
     * @param currentPage
     * @param type
     */
    public void setCurrDocAnnPoints(int currentPage, BaseAnnotation.Type type, String points, String deleteRect) {
        LogUtils.d(TAG, "setCurrDocAnnPoints currentPage=" + currentPage + " type=" + type.ordinal());
        DocumentBean documentBean = DBManager.getInstance().getCurrentDocumentBean();
        documentBean.setAnnotationType(type.ordinal());
        documentBean.setAnnotationPage(currentPage);
        documentBean.setAnnotationPoints(points);
        documentBean.setDeleteRect(deleteRect);
        DBManager.getInstance().getSession().getDocumentBeanDao().insert(documentBean);
    }

    /**
     * 保存当前页面的墨迹点
     *
     * @param currentPage
     * @param inkStrs
     * @param deleteRect
     */
    public void setCurrDocInksPoints(int currentPage, String inkStrs, String deleteRect) {
        DocumentBean documentBean = DBManager.getInstance().getCurrentDocumentBean();
        documentBean.setAnnotationType(BaseAnnotation.Type.Ink.ordinal());
        documentBean.setAnnotationPage(currentPage);
        documentBean.setInkPoints(inkStrs);
        documentBean.setDeleteRect(deleteRect);
        DBManager.getInstance().getSession().getDocumentBeanDao().insert(documentBean);
    }

    /**
     * 获取当前文档中的注释区域
     *
     * @param currentPage
     * @param type
     * @return
     */
    public List<DocumentBean> getDocAnnPoints(int currentPage, BaseAnnotation.Type type) {
        String strQuery = "where " + DocumentBeanDao.Properties.Path.columnName + " = ?AND " + DocumentBeanDao.Properties.AnnotationPage.columnName
                + " = ?AND " + DocumentBeanDao.Properties.AnnotationType.columnName + " = ?";
        List<DocumentBean> documentBeans = DBManager.getInstance().getSession().getDocumentBeanDao()
                .queryRaw(strQuery, Constants.DOCUMENT_PATH, String.valueOf(currentPage), String.valueOf(type.ordinal()));

        return documentBeans;
    }

}
