package com.github.reader.app.ui.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.github.reader.R;
import com.github.reader.app.ui.adapater.CatalogAdapter;
import com.github.reader.pdf.model.OutlineActivityData;
import com.github.reader.pdf.model.OutlineItem;
import com.github.reader.utils.Constants;
import com.github.reader.utils.ScreenUtils;

/**
 * Created by qiaodong on 17-6-14.
 */
public class CatalogMenu extends LinearLayout {

    private Context mContext;
    private int mMenuWidth;
    private TextView mTitle;
    private TextView mAuthor;
    private TextView mTotleSize;
    private ListView mListView;
    private OutlineItem[] mOutLineItems;
    private CatalogAdapter adapter;

    public CatalogMenu(Context context) {
        super(context);
        init(context);
    }

    public CatalogMenu(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public CatalogMenu(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        mContext = context;
        if (Constants.isShowInMainSceen) {
            LayoutInflater.from(context).inflate(R.layout.catalog_menu_layout, this, true);
        }
        initView();
        refactorMenuWith();
    }

    public void refactorMenuWith(){
        if(ScreenUtils.getScreenWidth() > ScreenUtils.getScreenHeight()){
            mMenuWidth = ScreenUtils.getScreenWidth() * 2 / 5;
        }else{
            mMenuWidth = ScreenUtils.getScreenWidth() * 4 / 5;
        }
        requestLayout();
    }

    private void initView() {
        mTitle = (TextView) findViewById(R.id.tvName);
        mAuthor = (TextView) findViewById(R.id.tvAuthor);
        mTotleSize = (TextView) findViewById(R.id.tvTotalSize);
        mListView = (ListView) findViewById(R.id.listview);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (mMenuWidth > 0) {
            setMeasuredDimension(mMenuWidth, MeasureSpec.getSize(heightMeasureSpec));
            mListView.setSelection(OutlineActivityData.get().position);
        }
    }

    private void initData() {
        if (mOutLineItems != null) {
            adapter = new CatalogAdapter(mContext, mOutLineItems);
            mListView.setAdapter(adapter);
            adapter.notifyDataSetChanged();
            if(mListener != null){
                mListView.setOnItemClickListener(mListener);
            }

        }
    }

    public void setData(OutlineItem[] data) {
        mOutLineItems = data;
        mTitle.setText(Constants.FILE_NAME);
        mAuthor.setText("XXXX");
        mTotleSize.setText(String.valueOf(Constants.DOC_PAGE_COUNT));
        initData();
    }

    public int getCurPage(int position){
        return mOutLineItems[position].page;
    }

    private AdapterView.OnItemClickListener mListener;
    public void setOnItemClickListener(AdapterView.OnItemClickListener listener){
        mListener = listener;
    }
}
