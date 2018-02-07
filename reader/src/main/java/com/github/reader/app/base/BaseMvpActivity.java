package com.github.reader.app.base;

import android.os.Bundle;

/**
 * Created by RayYeung on 2016/8/8.
 */
public abstract class BaseMvpActivity<P extends BasePresenter> extends BaseActivity {
    protected P mvpPresenter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mvpPresenter = setPresenter();
        super.onCreate(savedInstanceState);
    }

    protected abstract P setPresenter();

    /**
     * 获取Presenter
     * @return 返回子类创建的Presenter
     */
    public P getPresenter() {
        return mvpPresenter;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mvpPresenter != null) {
            mvpPresenter.detachView();
        }
    }
}
