package com.github.reader.app.model.entity;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.annotation.Id;

/**
 * Created by qiaodong on 17-12-8.
 */
@Entity
public class DocumentBean {
    @Id(autoincrement = true)
    private Long id;
    private String path;  //文档路径
    private Integer index; //上次阅读的页号
    private Integer pageCount;//总页数
    private Integer annotationType;  //高亮的区域，墨迹，删除线，下划线
    private String annotationPoints; //注释区域，坐标点的集合,一个区域由4个点组成
    private String inkPoints; //墨迹
    private int annotationPage; //注释区域,所在页号
    private String deleteRect; //一块高亮文字区域,或者一片墨迹,所在的区域点.由一个Rect组成

    @Generated(hash = 541554859)
    public DocumentBean(Long id, String path, Integer index, Integer pageCount,
            Integer annotationType, String annotationPoints, String inkPoints,
            int annotationPage, String deleteRect) {
        this.id = id;
        this.path = path;
        this.index = index;
        this.pageCount = pageCount;
        this.annotationType = annotationType;
        this.annotationPoints = annotationPoints;
        this.inkPoints = inkPoints;
        this.annotationPage = annotationPage;
        this.deleteRect = deleteRect;
    }
    @Generated(hash = 1348634967)
    public DocumentBean() {
    }
    public Long getId() {
        return this.id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public String getPath() {
        return this.path;
    }
    public void setPath(String path) {
        this.path = path;
    }
    public Integer getIndex() {
        return this.index;
    }
    public void setIndex(Integer index) {
        this.index = index;
    }
    public Integer getPageCount() {
        return this.pageCount;
    }
    public void setPageCount(Integer pageCount) {
        this.pageCount = pageCount;
    }
    public Integer getAnnotationType() {
        return this.annotationType;
    }
    public void setAnnotationType(Integer annotationType) {
        this.annotationType = annotationType;
    }
    public String getAnnotationPoints() {
        return this.annotationPoints;
    }
    public void setAnnotationPoints(String annotationPoints) {
        this.annotationPoints = annotationPoints;
    }
    public int getAnnotationPage() {
        return this.annotationPage;
    }
    public void setAnnotationPage(int annotationPage) {
        this.annotationPage = annotationPage;
    }
    public String getInkPoints() {
        return this.inkPoints;
    }
    public void setInkPoints(String inkPoints) {
        this.inkPoints = inkPoints;
    }
    public String getDeleteRect() {
        return this.deleteRect;
    }
    public void setDeleteRect(String deleteRect) {
        this.deleteRect = deleteRect;
    }
}