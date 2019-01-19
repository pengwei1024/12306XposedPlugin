package com.mobileTicket.hello12306.util;

import android.os.Build;
import android.support.annotation.AnyThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.mobileTicket.hello12306.model.Trains;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * 发送通知消息
 */
public class MessageUtil {

    private static final String TAG = "MessageUtil";

    /**
     * 通知服务器抢到票了
     *
     * @param trains Trains
     */
    @AnyThread
    public static void sendToHi(@NonNull Trains trains) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("ticket", "抢到票了!!! " + trains.code + "("
                    + trains.fromStation + "=>" + trains.toStation + ") " + trains.train_date);
            jsonObject.put("device", deviceInfo());
            jsonObject.put("time", System.currentTimeMillis());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        HttpUtil.jsonPost("http://tools.apkfuns.com/api/sendMessage.php",
                jsonObject, new HttpUtil.OnRequestResult() {
                    @Override
                    public void onSuccess(String result) {
                        Log.w(TAG, "success:" + result);
                    }

                    @Override
                    public void onFailure(int code, String result, @Nullable Throwable throwable) {
                        Log.w(TAG, "onFailure:" + code + ", " + result, throwable);
                    }
                });
    }

    /**
     * 设备信息
     *
     * @return string
     */
    private static String deviceInfo() {
        return Build.BRAND + " " + Build.MODEL + "(" + Build.VERSION.SDK_INT + ")";
    }
}
