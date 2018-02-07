package com.github.reader.pdf.ui.presentation;

import android.app.Presentation;
import android.content.Context;
import android.os.Bundle;
import android.view.Display;
import android.view.ViewGroup;

import com.github.reader.R;

/**
 * 副屏V的实现
 */

public class PdfPresentation extends Presentation{

    private ViewGroup mPdfPresenRootView;

    public PdfPresentation(Context outerContext, Display display) {
        super(outerContext, display);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.presentation_main_layout);
        mPdfPresenRootView = (ViewGroup) findViewById(R.id.main_presentation_layout_root);
    }

    public ViewGroup getRootView(){
        return mPdfPresenRootView;
    }
}
