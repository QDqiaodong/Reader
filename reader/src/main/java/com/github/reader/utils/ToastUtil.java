package com.github.reader.utils;

import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;


/**
 * Created by ZhanTao on 6/1/16.
 */
public class ToastUtil {
    private static volatile ToastUtil sToastUtil = null;

    private Toast mToast = null;

    /**
     *
     * @return
     */
    public static ToastUtil getInstance() {
        if (sToastUtil == null) {
            synchronized (ToastUtil.class) {
                if (sToastUtil == null) {
                    sToastUtil = new ToastUtil();
                }
            }
        }
        return sToastUtil;
    }

    protected Handler handler = new Handler(Looper.getMainLooper());

    /**
     * 显示Toast，多次调用此函数时，Toast显示的时间不会累计，并且显示内容为最后一次调用时传入的内容
     * 持续时间默认为short
     * @param tips 要显示的内容
     *            {@link Toast#LENGTH_LONG}
     */
    public void showToast(final String tips){
        showToast(tips, Toast.LENGTH_LONG);
    }

    public void showToast(final int tips){
        showToast(tips, Toast.LENGTH_SHORT);
    }
    /**
     * 显示Toast，多次调用此函数时，Toast显示的时间不会累计，
     * 并且显示内容为最后一次调用时传入的内容
     *
     * @param tips 要显示的内容
     * @param duration 持续时间，参见{@link Toast#LENGTH_SHORT}和
     *            {@link Toast#LENGTH_LONG}
     */
    public void showToast(final String tips, final int duration) {
        if (android.text.TextUtils.isEmpty(tips)) {
            return;
        }
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (mToast == null) {
                    mToast = Toast.makeText(AppUtils.getAppContext(), tips, duration);
                    mToast.show();
                } else {
                    //mToast.cancel();
                    //mToast.setView(mToast.getView());
                    mToast.setText(tips);
                    mToast.setDuration(duration);
                    mToast.show();
                }
            }
        });
    }

    public void showToast(final int tips, final int duration) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (mToast == null) {
                    mToast = Toast.makeText(AppUtils.getAppContext(), tips, duration);
                    mToast.show();
                } else {
                    //mToast.cancel();
                    //mToast.setView(mToast.getView());
                    mToast.setText(tips);
                    mToast.setDuration(duration);
                    mToast.show();
                }
            }
        });
    }

    /*//自定义toast的位置
    public void showToast(final String tips, final int duration, final int gravity, final int xOffset, final int yOffset) {
        if (android.text.TextUtils.isEmpty(tips)) {
            return;
        }
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (mToast == null) {
                    mToast =  new Toast(AppUtils.getAppContext());
                    View view = LayoutInflater.from(AppUtils.getAppContext()).inflate(R.layout.reader_toast, null);
                    mToast.setView(view);
                    mToast.setDuration(duration);
                    mToast.setText(tips);
                    mToast.setGravity(gravity,xOffset,yOffset);
                    mToast.show();
                } else {
                    mToast.setText(tips);
                    mToast.setDuration(duration);
                    mToast.show();
                }

            }
        });
    }*/
}
