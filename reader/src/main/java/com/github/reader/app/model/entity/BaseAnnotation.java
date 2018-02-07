package com.github.reader.app.model.entity;

import android.graphics.RectF;

/**
 * 注解:高亮，下划线，删除线，
 * 墨迹?复制　是否了加入？
 */
public class BaseAnnotation extends RectF {
	public enum Type {
		Highlight,Underline,StrikeOut,Ink, UNKNOWN
	}

	public final Type type;
	public int index;

	public BaseAnnotation(float x0, float y0, float x1, float y1, int _type,int _index) {
		super(x0, y0, x1, y1);
		type = _type == -1 ? Type.UNKNOWN : Type.values()[_type];
		index = _index;
	}


	@Override
	public boolean equals(Object o) {
		return super.equals(o);
	}
}
