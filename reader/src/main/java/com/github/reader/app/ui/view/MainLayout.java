package com.github.reader.app.ui.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.widget.RelativeLayout;

import com.github.reader.utils.Constants;

/**
 * Created by ZhanTao on 6/2/17.
 */

public class MainLayout extends RelativeLayout {

    private float mStartX = 0.0f;
    private float mStartY = 0.0f;
    private int screenWidth;
    private int screenHight;

    private int mTouchSlop = 5;

    private boolean bInterceptCenterClickEvent = true;

    private OnCenterClickListener mOnCenterClickListener;

    public void setOnCenterClickListener(OnCenterClickListener onCenterClickListener) {
        this.mOnCenterClickListener = onCenterClickListener;
    }

    public void willInterceptCenterClickEvent(boolean bInterceptCenterClickEvent) {
        this.bInterceptCenterClickEvent = bInterceptCenterClickEvent;
    }

    public static interface OnCenterClickListener {
        void onCenterClick();

        void onPreClick();

        void onLaterClick();
    }

    public MainLayout(Context context) {
        super(context);
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
    }

    public MainLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
    }

    public MainLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        screenHight = MeasureSpec.getSize(heightMeasureSpec);
        screenWidth = MeasureSpec.getSize(widthMeasureSpec);
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        //LogUtils.d("onInterceptTouchEvent() ev=" + ev.toString());
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        //LogUtils.d("dispatchTouchEvent() ev=" + ev.toString());
        int actionMasked = ev.getActionMasked();
        switch (actionMasked) {
            case MotionEvent.ACTION_DOWN:
                mStartX = ev.getX();
                mStartY = ev.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (bInterceptCenterClickEvent && Math.abs(ev.getX() - mStartX) < mTouchSlop && Math.abs(ev.getY() - mStartY) < mTouchSlop) {
                    if (mOnCenterClickListener == null) return super.dispatchTouchEvent(ev);

                    if (!Constants.HORIZONTAL_SCROLLING) {//上下方向滑动
                        if (mStartY < screenHight / 3) {
                            mOnCenterClickListener.onPreClick();
                        } else if (mStartY < (screenHight * 2 / 3)) {
                            mOnCenterClickListener.onCenterClick();
                        } else {
                            mOnCenterClickListener.onLaterClick();
                        }
                    } else {
                        if (mStartX < screenWidth / 3) {
                            mOnCenterClickListener.onPreClick();
                        } else if (mStartX < (screenWidth * 2 / 3)) {
                            mOnCenterClickListener.onCenterClick();
                        } else {
                            mOnCenterClickListener.onLaterClick();
                        }
                    }
                    return true;
                }
                break;
            default:
                break;
        }
        return super.dispatchTouchEvent(ev);
    }
}
