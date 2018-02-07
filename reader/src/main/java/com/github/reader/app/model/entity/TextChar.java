package com.github.reader.app.model.entity;

import android.graphics.RectF;

/**
 * 对应单个字符的坐标以及字符
 */
public class TextChar extends RectF {
	public char c;

	public TextChar(float x0, float y0, float x1, float y1, char _c) {
		super(x0, y0, x1, y1);
		c = _c;
	}
}
