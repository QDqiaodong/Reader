package com.github.reader.app.model.entity;

import android.graphics.RectF;

/**
 * 搜索结果的类
 */
public class BaseSearchTaskResult {
	public final String txt;
	public final int   pageNumber;
	public final RectF searchBoxes[];
	static private BaseSearchTaskResult singleton;

	/**
	 *
	 * @param _txt　　内容
	 * @param _pageNumber　　页号
	 * @param _searchBoxes　　位置
	 */
	public BaseSearchTaskResult(String _txt, int _pageNumber, RectF _searchBoxes[]) {
		txt = _txt;
		pageNumber = _pageNumber;
		searchBoxes = _searchBoxes;
	}

	static public BaseSearchTaskResult get() {
		return singleton;
	}

	static public void set(BaseSearchTaskResult r) {
		singleton = r;
	}
}
