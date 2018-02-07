package com.github.reader.app.ui.adapater;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.github.reader.R;
import com.github.reader.pdf.model.OutlineItem;

/**
 * Created by qiaodong on 18-1-23.
 */

public class CatalogAdapter extends BaseAdapter {
    private final OutlineItem mItems[];
    private Context mContext;

    public CatalogAdapter(Context context, OutlineItem items[]) {
        mContext = context;
        mItems = items;
    }

    @Override
    public int getCount() {
        return mItems.length;
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(R.layout.catalog_menu_item_layout, null);
            viewHolder = new ViewHolder(convertView);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        //ViewHolder viewHolder = new ViewHolder(v);
        int level = mItems[position].level;
        if (level > 8) level = 8;
        String space = "";
        for (int i = 0; i < level; i++) {
            space += "   ";
        }
        viewHolder.mTitle.setText(space + mItems[position].title);
        viewHolder.mPageNum.setText(String.valueOf(mItems[position].page + 1));
        return convertView;
    }

    public class ViewHolder {
        public TextView mTitle;
        public TextView mPageNum;

        public ViewHolder(View v) {
            mTitle = (TextView) v.findViewById(R.id.catalog_title);
            mPageNum = (TextView) v.findViewById(R.id.catalog_page_num);
        }

    }
}
