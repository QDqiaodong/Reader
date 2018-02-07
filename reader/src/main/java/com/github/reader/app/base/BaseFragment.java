package com.github.reader.app.base;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.View;

/**
 * Created by ZhanTao on 11/17/17.
 */

public /*abstract*/ class BaseFragment extends Fragment {

    protected Activity mContext;
    protected boolean mIsFirstVisible = true;
    protected View rootView;

//    @Nullable
//    @Override
//    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
//
//        return loadViewLayout(inflater, container);
//    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mContext = getActivity();
        rootView = view;
        initView(view);
        boolean isVis = isHidden() || getUserVisibleHint();
        if (isVis && mIsFirstVisible) {
            lazyLoad();
            mIsFirstVisible = false;
        }
    }


    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) {
            onVisible();
        } else {
            onInVisible();
        }
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            onVisible();
        } else {
            onInVisible();
        }
    }

    /**
     * 当界面可见时的操作
     */
    protected void onVisible() {
        if (mIsFirstVisible && isResumed()) {
            lazyLoad();
            mIsFirstVisible = false;
        }
    }

    /**
     * 数据懒加载
     */
    protected void lazyLoad() {

    }

    /**
     * 当界面不可见时的操作
     */
    protected void onInVisible() {

    }

    /**
     * 初始化界面
     *
     * @param view
     */
    private void initView(View view) {
//        bindViews(view);
//        processLogic();
//        setListener();
    }
//
//    /**
//     * 加载布局
//     */
//    protected abstract View loadViewLayout(LayoutInflater inflater, ViewGroup container);
//
//    /**
//     * find控件
//     *
//     * @param view
//     */
//    protected abstract void bindViews(View view);
//
//    /**
//     * 处理数据
//     */
//    protected abstract void processLogic();
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
        intent.setClass(mContext, cla);
        startActivity(intent);
    }


    public final void startActivity(Class<?> cla, Bundle bundle) {
        Intent intent = new Intent();
        intent.putExtras(bundle);
        intent.setClass(mContext, cla);
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
        return (E) rootView.findViewById(id);
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

//    protected void showToast(String msg) {
//        ToastUtil.getInstance().showToast(msg);
//    }
//
//    protected void showToast(final int tips) {
//        ToastUtil.getInstance().showToast(tips);
//    }
//
//
//
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

}
