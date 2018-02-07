package com.github.reader.app.model.entity;

import android.graphics.RectF;

import java.util.ArrayList;

/**
 * 高亮,下划线,删除线,是由一个个Rect区域组成
 */

public class AnnotationPointsBean {
    private ArrayList<RectF> lists;

    public AnnotationPointsBean(ArrayList<RectF> lists) {
        this.lists = lists;
    }

    public ArrayList<RectF> getLists() {
        return lists;
    }

    public void setLists(ArrayList<RectF> lists) {
        this.lists = lists;
    }

}
