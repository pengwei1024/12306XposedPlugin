package com.mobileTicket.hello12306.util;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import java.util.concurrent.ConcurrentHashMap;

public class MessageClient {
    private static final String TAG = "MessageClient";
    private static final String ACTION = "com.apkfuns.hook12306.cast";
    private Context context;
    private ConcurrentHashMap<String, Callback> callbacks = new ConcurrentHashMap<>();
    private QueryListener queryListener;
    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Message msg = intent.getParcelableExtra("message");
            String id = intent.getStringExtra("id");
            if (msg == null || msg.sendingUid == MessageClient.this.hashCode() || TextUtils.isEmpty(id)) {
                return;
            }
            Log.d(TAG, "what=" + msg.what + ", id=" + id + ", uid=" + msg.sendingUid);
            if (queryListener != null) {
                queryListener.onQuery(msg, new Response(id, msg));
            }
            Callback callback = callbacks.remove(id);
            if (callback != null) {
                callback.onResponse(msg);
            }
        }
    };

    public MessageClient(@NonNull Context context, @Nullable QueryListener queryListener) {
        this.context = context;
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION);
        context.registerReceiver(receiver, intentFilter);
        this.queryListener = queryListener;
        Log.d(TAG, "init =>" + hashCode() + ", context=" + this.context.getClass());
    }

    public void sendToTarget(@NonNull Message message, @Nullable Callback callback) {
        sendToTarget(message, callback, null);
    }

    public void sendToTarget(int what) {
        sendToTarget(Message.obtain(null, what), null);
    }

    public void sendToTarget(int what, int value) {
        sendToTarget(Message.obtain(null, what, value, 0), null);
    }

    public void sendToTarget(int what, String value, @Nullable Callback callback) {
        Message message = Message.obtain(null, what);
        Bundle bundle = new Bundle();
        bundle.putString("data", value);
        message.setData(bundle);
        sendToTarget(message, callback);
    }

    private void sendToTarget(@NonNull Message message, @Nullable Callback callback, @Nullable String id) {
        if (TextUtils.isEmpty(id)) {
            id = String.valueOf(System.currentTimeMillis());
        }
        Log.d(TAG, "sendToTarget what=" + message.what + ", id=" + id + ", uid=" + hashCode());
        Intent intent = new Intent();
        intent.putExtra("id", id);
        message.sendingUid = hashCode();
        intent.putExtra("message", message);
        intent.setAction(ACTION);
        if (callback != null) {
            callbacks.put(id, callback);
        }
        context.sendBroadcast(intent);
    }

    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        context.unregisterReceiver(receiver);
        callbacks.clear();
    }

    public Activity getActivity() {
        if (context instanceof Activity) {
            return (Activity) context;
        }
        return null;
    }

    public interface Callback {
        void onResponse(Message message);
    }

    public interface QueryListener {
        void onQuery(Message msg, Response response);
    }

    public class Response {
        private String id;
        private Message message;

        private Response(String id, Message message) {
            this.id = id;
            this.message = message;
        }

        public void call(@NonNull Message msg) {
            msg.sendingUid = MessageClient.this.hashCode();
            msg.what = message.what;
            sendToTarget(msg, null, id);
        }
    }

}
