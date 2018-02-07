package com.github.reader.utils;

import android.util.Log;

public class LogUtils {
    public static final String LogUtils_ID = "okay_mirror";
    public static final boolean DEV_BUILD = true;

    public static void d(String tag, String outMessage) {
        if (DEV_BUILD) {
            Log.d(tag, outMessage);
        }
    }

    public static void d(String outMessage) {
        if (DEV_BUILD) {
            Log.d(LogUtils_ID, outMessage);
        }
    }

    public static void e(String tag, String outMessage) {
        if (DEV_BUILD) {
            Log.e(tag, outMessage);
        }
    }

    public static void e(String outMessage) {
        if (DEV_BUILD) {
            Log.e(LogUtils_ID, outMessage);
        }
    }

    public static void w(String tag, String outMessage) {
        if (DEV_BUILD) {
            Log.d(tag, outMessage);
        }
    }

    public static void w(String outMessage) {
        if (DEV_BUILD) {
            Log.w(LogUtils_ID, outMessage);
        }
    }

    public static void i(String tag, String outMessage) {
        if (DEV_BUILD) {
            Log.i(tag, outMessage);
        }
    }

    public static void i(String outMessage) {
        if (DEV_BUILD) {
            Log.i(LogUtils_ID, outMessage);
        }
    }

    public static void v(String tag, String outMessage) {
        if (DEV_BUILD) {
            Log.v(tag, outMessage);
        }
    }

    public void v(String outMessage) {
        if (DEV_BUILD) {
            Log.v(LogUtils_ID, outMessage);
        }
    }
}
