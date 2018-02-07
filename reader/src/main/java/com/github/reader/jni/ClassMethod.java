package com.github.reader.jni;

import com.github.reader.utils.LogUtils;

/**
 * Created by qiaodong on 18-2-1.
 */

public class ClassMethod {
    private static void callStaticMethod(String str, int i) {
        LogUtils.d("ClassMethod","ClassMethod::callStaticMethod called!-->str="+str+"\n i="+i);
    }
}
