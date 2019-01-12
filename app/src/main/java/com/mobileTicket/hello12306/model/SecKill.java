package com.mobileTicket.hello12306.model;

import android.support.annotation.AnyThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.Pair;

import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 秒杀任务
 * 11点16分起售
 */
public class SecKill {

    private volatile static SecKill singleton;
    private volatile SecKillTask currentTask;
    private final Pattern pattern = Pattern.compile("^(\\d+)点(\\d+)分起售$");
    private static final String TAG = "SecKill";
    private volatile Pair<Timer, Long> timerPair;

    private SecKill() {
    }

    public static SecKill getInstance() {
        if (singleton == null) {
            synchronized (SecKill.class) {
                if (singleton == null) {
                    singleton = new SecKill();
                }
            }
        }
        return singleton;
    }

    // 12点30分起售
    public void addTask(@Nullable String message, @NonNull Runnable callback) {
        if (message == null) {
            return;
        }
        if (currentTask != null && currentTask.isSameTask(message)) {
            return;
        }
        synchronized (SecKill.class) {
            if (currentTask != null && currentTask.isSameTask(message)) {
                return;
            }
            Matcher matcher = pattern.matcher(message);
            if (matcher.find()) {
                Calendar calendar = Calendar.getInstance();
                calendar.set(Calendar.HOUR, Integer.parseInt(matcher.group(1)));
                calendar.set(Calendar.MINUTE, Integer.parseInt(matcher.group(2)));
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);
                long currentTime = calendar.getTimeInMillis();
                if (currentTime <= System.currentTimeMillis()) {
                    return;
                }
                currentTask = new SecKillTask(message, currentTime, callback);
                createTimer();
            }
        }
    }

    @AnyThread
    private synchronized void createTimeTask(long waitTime) {
        if (timerPair != null) {
            if (timerPair.second == waitTime) {
                Log.i(TAG, "相同等待时间:" + waitTime);
                return;
            }
            timerPair.first.cancel();
        }
        Timer timer = new Timer();
        timerPair = Pair.create(timer, waitTime);
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                createTimer();
                if (currentTask != null) {
                    long nowTime = System.currentTimeMillis();
                    if (currentTask.killTime - nowTime < 1000) {
                        currentTask.run();
                    }
                }
            }
        }, 0, waitTime);
        Log.i(TAG, "Timer:" + waitTime);
    }

    @AnyThread
    private void createTimer() {
        if (currentTask == null) {
            return;
        }
        long nowTime = System.currentTimeMillis();
        long interval = currentTask.getKillTime() - nowTime;
        // 任务结束
        if (interval <= -1000) {
            currentTask = null;
            if (timerPair != null) {
                timerPair.first.cancel();
            }
            Log.i(TAG, "任务结束");
            return;
        }
        if (interval > 5 * 60 * 1000) {
            createTimeTask(60_000);
        } else if (interval > 60 * 1000) {
            createTimeTask(30_000);
        } else if (interval > 5 * 1000) {
            createTimeTask(3000);
        } else {
            createTimeTask(200);
        }
    }


    private static class SecKillTask {
        @NonNull
        private final String message;
        private final long killTime;
        @NonNull
        private final Runnable callback;
        private final AtomicInteger runCount;

        private SecKillTask(@NonNull String message, long killTime, @NonNull Runnable callback) {
            this.message = message;
            this.killTime = killTime;
            this.callback = callback;
            this.runCount = new AtomicInteger(2);
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof SecKillTask)) {
                return false;
            }
            return isSameTask(((SecKillTask) obj).message);
        }

        public boolean isSameTask(String message) {
            return this.message.equals(message);
        }

        public long getKillTime() {
            return killTime;
        }

        public void run() {
            if (runCount.getAndDecrement() > 0) {
                callback.run();
                Log.i(TAG, "执行");
            }
        }
    }
}
