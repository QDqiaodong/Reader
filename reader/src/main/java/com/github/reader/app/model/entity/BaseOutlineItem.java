package com.github.reader.app.model.entity;

/**
 * 目录对象
 */
public class BaseOutlineItem {
	public final int    level;
	public final String title;
	public final int    page;

	BaseOutlineItem(int _level, String _title, int _page) {
		level = _level;
		title = _title;
		page  = _page;
	}

}
