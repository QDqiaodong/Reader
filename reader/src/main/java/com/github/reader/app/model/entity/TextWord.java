package com.github.reader.app.model.entity;

import android.graphics.RectF;

/**
 * 对应一串
 */
public class TextWord extends RectF {
	public String w;

	public TextWord() {
		super();
		w = new String();
	}

	//两个区域，拼接成的最大区域
	public void Add(TextChar tc) {
		super.union(tc);
		w = w.concat(new String(new char[]{tc.c}));
	}
}
