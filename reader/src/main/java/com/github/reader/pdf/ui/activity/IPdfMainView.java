package com.github.reader.pdf.ui.activity;

import android.content.Intent;

import com.github.reader.app.base.IBaseDocumentView;
import com.github.reader.pdf.model.MuPDFAlert;

/**
 * 主屏V的接口
 */

public interface IPdfMainView extends IBaseDocumentView {
    void requestPassword();
    void showMupdfAlertDialog(final boolean alertsActive, final MuPDFAlert result, final MuPDFAlert.ButtonPressed pressed[]);
    void startActivityForResult(Intent intent);
}
