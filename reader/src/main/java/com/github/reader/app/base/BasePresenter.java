/*
 * Copyright 2016, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.reader.app.base;


import com.github.reader.utils.AppUtils;

interface IPresenter<V> {

    void attachView(V view);

    void detachView();

}

public class BasePresenter<V> implements IPresenter<V>{
    public V mvpView;

    public BasePresenter(V mvpView){
        attachView(mvpView);
    }

    @Override
    public void attachView(V view) {
        this.mvpView = view;
    }

    @Override
    public void detachView() {
        this.mvpView = null;
    }

    protected String getString(int resId) {
        return AppUtils.getAppContext().getString(resId);
    }
}



