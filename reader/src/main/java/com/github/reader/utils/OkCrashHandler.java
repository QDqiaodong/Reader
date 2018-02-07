package com.github.reader.utils;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Calendar;


public class OkCrashHandler implements Thread.UncaughtExceptionHandler {

    private static final String TAG = "OkCrashHandler";

    private OkCrashHandler() {
    }

    private static OkCrashHandler instance = new OkCrashHandler();

    private Thread.UncaughtExceptionHandler mDefaultHandler;

    private Context mContext;

    public static OkCrashHandler getInstance() {
        return instance;
    }

    public void init(Context context) {
        mContext = context;
        mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    @Override
    public void uncaughtException(Thread thread, Throwable throwable) {
        Log.d(TAG, "OkCrashHandler uncaughtException");

        if (!handleException(throwable) && mDefaultHandler != null) {
            //如果用户没有处理则让系统默认的异常处理器来处理
            mDefaultHandler.uncaughtException(thread, throwable);
        } else {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                Log.e(TAG, "error : ", e);
            }
            //退出程序
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(1);
        }
    }


    private boolean handleException(Throwable ex) {
        if (ex == null) {
            return false;
        }
        //使用Toast来显示异常信息
       /* new Thread() {
            @Override
            public void run() {
                Looper.prepare();
                Toast.makeText(mContext, "很抱歉,程序出现异常,即将退出.", Toast.LENGTH_LONG).show();
                Looper.loop();
            }
        }.start();*/
        //保存日志文件
        saveCrashInfo2File(ex);
        return true;
    }

    private String saveCrashInfo2File(Throwable ex) {
        StringBuffer sb = new StringBuffer();

        Writer writer = new StringWriter();
        PrintWriter printWriter = new PrintWriter(writer);
        ex.printStackTrace(printWriter);
        Throwable cause = ex.getCause();

        while (cause != null) {
            cause.printStackTrace(printWriter);
            cause = cause.getCause();
        }

        printWriter.close();
        String result = writer.toString();
        sb.append(result);

        try {
            long timestamp = System.currentTimeMillis();

            Calendar myDate = Calendar.getInstance();

            StringBuffer date = new StringBuffer();
            date.append(myDate.get(Calendar.YEAR)).append("-").append(myDate.get(Calendar.MONTH)
                    + 1).append("-")
                    .append(myDate.get(Calendar.DAY_OF_MONTH)).append("-").append(myDate.get
                    (Calendar.HOUR))
                    .append("-").append(myDate.get(Calendar.MINUTE)).append("-").append(myDate.get(Calendar.SECOND));

            String fileName = "crash-" + date.toString() + "-" + timestamp + ".log";

            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {

                String path =  mContext.getExternalCacheDir().toString();
                File dir = new File(path);

                if (!dir.exists()) {
                    dir.mkdirs();
                }

                FileOutputStream fos = new FileOutputStream(new File(path,fileName));
                fos.write(sb.toString().getBytes());
                fos.close();
            }

            return fileName;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}
