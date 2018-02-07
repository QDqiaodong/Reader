package com.github.reader.app.model.entity;

import android.graphics.PointF;

import java.util.ArrayList;

/**
 * Created by qiaodong on 17-12-12.
 */

public class InkPointsBean {
    private ArrayList<PointF> lists;

    public InkPointsBean(ArrayList<PointF> lists) {
        this.lists = lists;
    }

    public ArrayList<PointF> getLists() {
        return lists;
    }

    public void setLists(ArrayList<PointF> lists) {
        this.lists = lists;
    }
}
