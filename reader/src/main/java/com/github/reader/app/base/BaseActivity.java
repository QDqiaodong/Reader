/**
 * Copyright 2016 JustWayward Team
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.reader.app.base;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.github.reader.utils.ToastUtil;


public class BaseActivity extends Activity {

    private static final String TAG = "BaseActivity";


//    //页面加载弹框动画
//    private CustomProgressDialog progressDialog = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        getWindow().setBackgroundDrawable(null);
        initView(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
//        dismissLoadingDialog();
    }


    /**
     * 初始化界面
     */
    protected void initView(Bundle savedInstanceState) {
//        if (!isStatusBarOverlay()) {
//            setSystemBarColorPrimary();
//        } else {
//            StatusBarCompat.translucentStatusBar(this);
//        }
//
//        loadViewLayout();
//        bindViews();
//        processLogic(savedInstanceState);
//        setListener();
    }

//    protected boolean isStatusBarOverlay() {
//        return false;
//    }
//
//    @TargetApi(Build.VERSION_CODES.KITKAT)
//    protected void setSystemBarWhite(){
//        setSystemBarColor(android.R.color.white);
//    }
//
//    @TargetApi(Build.VERSION_CODES.KITKAT)
//    protected void setSystemBarColorPrimary(){
//        setSystemBarColor(R.color.colorPrimary);
//    }
//
//    @TargetApi(Build.VERSION_CODES.KITKAT)
//    protected void setSystemBarColor(int resColor){
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
////            mBaseView.setFitsSystemWindows(true);
////            mBaseView.setClipToPadding(false);
//            setTranslucentStatus(true);
//            SystemBarTintManager tintManager = new SystemBarTintManager(this);
//            tintManager.setStatusBarTintEnabled(true);
//            tintManager.setStatusBarTintResource(resColor);
//        }
//    }
//
//
//    @TargetApi(Build.VERSION_CODES.KITKAT)
//    private void setTranslucentStatus(boolean on) {
//        Window win = getWindow();
//        WindowManager.LayoutParams winParams = win.getAttributes();
//        final int bits = WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS;
//        if (on) {
//            winParams.flags |= bits;
//        } else {
//            winParams.flags &= ~bits;
//        }
//        win.setAttributes(winParams);
//    }
//
//    /**
//     * 加载布局
//     */
//    protected abstract void loadViewLayout();
//
//    /**
//     * find控件
//     */
//    protected abstract void bindViews();
//
//
//    /**
//     * 处理数据
//     */
//    protected abstract void processLogic(Bundle savedInstanceState);
//
//    /**
//     * 设置监听
//     */
//    protected abstract void setListener();

    /*
     * Activity的跳转
	 */
    public final void startActivity(Class<?> cla) {
        Intent intent = new Intent();
        intent.setClass(this, cla);
        startActivity(intent);
    }


    public final void startActivity(Class<?> cla, Bundle bundle) {
        Intent intent = new Intent();
        intent.putExtras(bundle);
        intent.setClass(this, cla);
        startActivity(intent);
    }

    /**
     * 获取控件
     *
     * @param id  控件的id
     * @param <E>
     * @return
     */
    protected <E extends View> E get(int id) {
        return (E) findViewById(id);
    }

    protected void gone(final View... views) {
        if (views != null && views.length > 0) {
            for (View view : views) {
                if (view != null) {
                    view.setVisibility(View.GONE);
                }
            }
        }
    }

    protected void showToast(String msg) {
        ToastUtil.getInstance().showToast(msg);
    }

    protected void showToast(final int resId) {
        ToastUtil.getInstance().showToast(resId);
    }


//    protected void showCenterToast(String msg) {
//        showCenterToast(msg, Toast.LENGTH_SHORT);
//    }
//
//
//    protected void showCenterToast(String msg, int duration) {
//        ToastUtil.getInstance().showCenterToast(msg, duration);
//    }
//
//    protected void showCenterToast(final int resId) {
//        showCenterToast(getString(resId));
//    }
//
//    protected void showCenterToast(final int resId, int duration) {
//        showCenterToast(getString(resId), duration);
//    }
//
//
//
//    /**
//     * 页面跳转的时候弹框动画
//     */
//    public void showLoadingDialog() {
//        showLoadingDialog(getString(R.string.loading_tip));
//    }
//
//    /**
//     * 页面跳转的时候动画效果
//     *
//     * @param loadingTip loading提示
//     */
//    protected void showLoadingDialog(String loadingTip) {
//        try {
//            if (progressDialog == null) {
//                progressDialog = new CustomProgressDialog(this);
//                progressDialog.setMessage(loadingTip);
//                progressDialog.setCanceledOnTouchOutside(false);
//                android.view.WindowManager.LayoutParams lay =
//                        progressDialog.getWindow().getAttributes();
//                DisplayMetrics dm = new DisplayMetrics();
//                getWindowManager().getDefaultDisplay().getMetrics(dm);
//                Rect rect = new Rect();
//                View view = getWindow().getDecorView();
//                view.getWindowVisibleDisplayFrame(rect);
//                lay.height = dm.heightPixels - rect.top;
//                lay.width = dm.widthPixels;
//            }
//            if (!isFinishing() && !progressDialog.isShowing()) {
//                progressDialog.show();
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//    /**
//     * 页面跳转结束弹窗
//     */
//    public void dismissLoadingDialog() {
//        ViewUtils.runInHandlerThread(new Runnable() {
//
//            public void run() {
//
//                try {
//                    if (progressDialog != null && progressDialog.isShowing()) {
//                        progressDialog.dismiss();
//                        progressDialog = null;
//                    }
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            }
//        });
//    }
}
