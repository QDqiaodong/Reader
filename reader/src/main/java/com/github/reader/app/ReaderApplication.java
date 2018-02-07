package com.github.reader.app;

import android.app.Application;
import android.content.Context;
import android.hardware.display.DisplayManager;
import android.view.Display;

import com.github.reader.app.model.manager.DBManager;
import com.github.reader.utils.AppUtils;
import com.github.reader.utils.Constants;
import com.github.reader.utils.SharedPreferencesUtil;

/**
 * Created by qiaodong on 17-11-15.
 */

public class ReaderApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        AppUtils.init(this);
        initData();
        DBManager.getInstance().init(this);
        SharedPreferencesUtil.init(this, this.getPackageName() + "_preference", Context.MODE_PRIVATE);
        // OkCrashHandler.getInstance().init(this);

    }

    public void initData() {
        DisplayManager displayManager = (DisplayManager) getSystemService(Context.DISPLAY_SERVICE);
        Display[] presentationDisplays = displayManager
                .getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION);
        if (presentationDisplays.length <= 0) {
            Constants.isDoubleScreen = false;
        } else {
            Constants.isDoubleScreen = true;
        }

    }
}
