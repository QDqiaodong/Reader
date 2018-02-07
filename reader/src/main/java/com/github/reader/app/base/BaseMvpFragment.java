package com.github.reader.app.base;


import android.os.Bundle;
import android.view.View;


/**
 * Created by RayYeung on 2016/8/8.
 */
public abstract class BaseMvpFragment<P extends BasePresenter> extends BaseFragment {
    protected P mvpPresenter;

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        if (mvpPresenter == null) mvpPresenter = createPresenter();
        super.onViewCreated(view, savedInstanceState);
    }


    @Override
    protected void lazyLoad() {
        if (mvpPresenter == null) mvpPresenter = createPresenter();
        super.lazyLoad();
    }

    protected abstract P createPresenter();


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mvpPresenter != null) {
            mvpPresenter.detachView();
            mvpPresenter = null;
        }
    }

}
