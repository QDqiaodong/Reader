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

interface IView<P>{

    void setPresenter(P presenter);

}

public class BaseView<P> implements IView<P>{

    public P mvpPresenter;
    @Override
    public void setPresenter(P presenter) {
        this.mvpPresenter = presenter;
    }
}


