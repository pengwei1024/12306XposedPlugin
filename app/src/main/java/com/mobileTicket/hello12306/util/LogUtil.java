package com.mobileTicket.hello12306.util;

import android.annotation.SuppressLint;
import android.os.Environment;
import android.support.annotation.AnyThread;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LogUtil {

    private static final String TAG = "LogUtil";

    private static final ExecutorService singleService = Executors.newSingleThreadExecutor();
    private static volatile PrintStream printStream = null;
    @SuppressLint("SimpleDateFormat")
    private static SimpleDateFormat dateFormat = new SimpleDateFormat("MM-dd HH:mm:ss:SSS");
    private static TimeZone gmt = TimeZone.getTimeZone("Asia/Shanghai");

    @AnyThread
    public static void print(final String content) {
        if (content == null) {
            return;
        }
        final String threadName = Thread.currentThread().getName();
        singleService.execute(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, content);
                if (printStream == null || printStream.checkError()) {
                    try {
                        printStream = new PrintStream(new File(Environment.getExternalStorageDirectory(),
                                "12306Hook.txt"));
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                    dateFormat.setTimeZone(gmt);
                }
                String time = dateFormat.format(new Date());
                if (printStream != null) {
                    printStream.println(String.format("[%s][%s] ", time, threadName) + content);
                }
            }
        });
    }
}
