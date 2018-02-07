package com.github.reader.pdf.ui.adapater;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.PointF;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.github.reader.pdf.model.AsyncTask;
import com.github.reader.pdf.model.MuPDFCore;
import com.github.reader.pdf.ui.view.PDFPageView;
import com.github.reader.utils.LogUtils;

public class MuPDFPageAdapter extends BaseAdapter {
	private static final String TAG = "MuPDFPageAdapter";
	private final Context mContext;
	private final MuPDFCore mCore;
	private final SparseArray<PointF> mPageSizes = new SparseArray<PointF>();
	private       Bitmap mSharedHqBm;

	public MuPDFPageAdapter(Context c, MuPDFCore core) {
		mContext = c;
		mCore = core;
	}

	public int getCount() {
		return mCore.countPages();
	}

	public Object getItem(int position) {
		return null;
	}

	public long getItemId(int position) {
		return 0;
	}

	public void releaseBitmaps()
	{
		//  recycle and release the shared bitmap.
		if (mSharedHqBm!=null)
			mSharedHqBm.recycle();
		mSharedHqBm = null;
	}

	public View getView(final int position, View convertView, ViewGroup parent) {
		final PDFPageView pageView;
		LogUtils.d(TAG,"getView position="+position+" parent.getWidth="+parent.getWidth()+" height="+parent.getHeight());
		if (convertView == null) {
			if (mSharedHqBm == null || mSharedHqBm.getWidth() != parent.getWidth() || mSharedHqBm.getHeight() != parent.getHeight())
				mSharedHqBm = Bitmap.createBitmap(parent.getWidth(), parent.getHeight(), Bitmap.Config.ARGB_8888);

			pageView = new PDFPageView(mContext, mCore, new Point(parent.getWidth(), parent.getHeight()), mSharedHqBm);
		} else {
			pageView = (PDFPageView) convertView;
		}

		PointF pageSize = mPageSizes.get(position);
		if (pageSize != null) {
			// We already know the page size. Set it up
			// immediately
			pageView.setPage(position, pageSize);
		} else {
			// Page size as yet unknown. Blank it for now, and
			// start a background task to find the size
			pageView.blank(position);
			AsyncTask<Void,Void,PointF> sizingTask = new AsyncTask<Void,Void,PointF>() {
				@Override
				protected PointF doInBackground(Void... arg0) {
					return mCore.getPageSize(position);
				}

				@Override
				protected void onPostExecute(PointF result) {
					super.onPostExecute(result);
					// We now know the page size
					mPageSizes.put(position, result);
					// Check that this view hasn't been reused for
					// another page since we started
					if (pageView.getPage() == position) {
						LogUtils.d(TAG,"position="+position+" result.width="+result.x+" result.y="+result.y);
						pageView.setPage(position, result);
					}
				}
			};

			sizingTask.execute((Void)null);
		}
		return pageView;
	}
}
