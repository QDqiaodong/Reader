package com.github.reader.app.base;

/**
 * Created by ZhanTao on 2/1/18.
 */

import android.app.Presentation;
import android.content.Intent;

public interface IBaseDocumentPresenter {
    void openDocument(Intent intent);

    void initData();

    void onResume();

    void onPause();

    void onDestory();

    void switchReadMode();

    Presentation initPresentation();

    Object getDocManager();

    void search(int direction);

}
