package com.github.reader.app.ui.view;

import android.graphics.PointF;
import android.graphics.RectF;

import com.github.reader.app.model.entity.BaseAnnotation;
import com.github.reader.pdf.model.Hit;
import com.github.reader.pdf.model.TextWord;


public interface IBaseDocView {
	/**
	 * 设置当前页号
	 * @param page　　页号
	 * @param size　　页面的实际大小
	 */
	public void setPage(int page, PointF size);

	/**
	 * 获取当前页号
	 * @return
	 */
	public int getPage();

	/**
	 * 设置DOC的初始背景
	 * @param page
	 */
	public void blank(int page);

	/**
	 * 判断点击区域的类型
	 * @param x
	 * @param y
	 * @return
	 */
	public Hit passClickEvent(float x, float y);

	/**
	 * 选择文字
	 * @param x0
	 * @param y0
	 * @param x1
	 * @param y1
	 */
	public void selectText(float x0, float y0, float x1, float y1);

	/**
	 * 清空选择文字
	 */
	public void deselectText();

	/**
	 * 复制文字
	 * @return
	 */
	public boolean copySelection();

	/**
	 * 高亮，下划线，删除线
	 * @param type
	 * @return
	 */
	public boolean markupSelection(BaseAnnotation.Type type);

	/**
	 * 删除选中的注释项
	 */
	public void deleteSelectedAnnotation();

	/**
	 * 取消，删除框
	 */
	public void deselectAnnotation();

	/**
	 * 设置搜索内容的区域
	 * @param searchBoxes
	 */
	public void setSearchBoxes(RectF searchBoxes[]);


	public void update();
	public void updateHq(boolean update);
	public void removeHq();

	/**
	 * 清空当前页面的一些任务(本页面所需的imageview还需重复利用)
	 */
	public void releaseResources();

	/**
	 * 清空当前的bitmap,释放当前所占通的资源
	 */
	public void onDestory();

	/**
	 * 绘制墨迹
	 * @param x
	 * @param y
	 */
	public void startDraw(float x, float y);
	public void continueDraw(float x, float y);
	public void cancelDraw();
	public boolean saveDraw();
	public TextWord[][] getText();

	/**
	 * 加载上次保存的注释项
	 * 高亮，下划线，删除线,墨迹
	 */
	public void loadAnnotations();

	public void loadInks();
}
