package com.github.reader.app.ui.view;

import android.content.Context;
import android.widget.ImageView;

/**
 * Created by qiaodong on 17-12-5.
 */

public class OpaqueImageView extends ImageView {
    public OpaqueImageView(Context context) {
        super(context);
    }

    @Override
    public boolean isOpaque() {
        return true;
    }
}
