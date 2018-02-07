package com.github.reader.pdf.ui.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import com.github.reader.app.ui.view.BaseDocView;
import com.github.reader.app.ui.view.IBaseDocView;
import com.github.reader.pdf.model.SearchTaskResult;
import com.github.reader.utils.LogUtils;

/**
 * 这个是adapterview
 */
public class MuPdfDocView extends BaseDocView {
	private static final String TAG = "MuPDFReaderView";
	private final Context mContext;

	public MuPdfDocView(Context context) {
		super(context);
		mContext = context;
	}

	public MuPdfDocView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		mContext = context;
	}


	/**
	 * 初始化view时候进行设置
	 * @param i
	 * @param v
     */
	protected void onChildSetup(int i, View v) {
		LogUtils.d(TAG,"onChildSetup..........");
		if (SearchTaskResult.get() != null
				&& SearchTaskResult.get().pageNumber == i)
			((IBaseDocView) v).setSearchBoxes(SearchTaskResult.get().searchBoxes);
		else
			((IBaseDocView) v).setSearchBoxes(null);
	}

	@Override
	protected void onMoveToChild(int i) {
		if (SearchTaskResult.get() != null
				&& SearchTaskResult.get().pageNumber != i) {
			SearchTaskResult.set(null);
			resetupChildren();
		}
	}


	@Override
	protected void onMoveOffChild(int i) {
		View v = getView(i);
		if (v != null)
			((IBaseDocView)v).deselectAnnotation();
	}

	@Override
	protected void onSettle(View v) {
		// When the layout has settled ask the page to render
		// in HQ
		LogUtils.d(TAG,"onSettle ......");
		((IBaseDocView) v).updateHq(false);
	}

	/**
	 *  When something changes making the previous settled view
	 *  no longer appropriate, tell the page to remove HQ
	 * @param v
     */
	protected void onUnsettle(View v) {
		LogUtils.d(TAG,TAG+" onUnsettle");
		((IBaseDocView) v).removeHq();
	}

	@Override
	protected void onNotInUse(View v) {
		((IBaseDocView) v).releaseResources();
	}

	@Override
	public void onDestory() {
		super.onDestory();
	}
}
