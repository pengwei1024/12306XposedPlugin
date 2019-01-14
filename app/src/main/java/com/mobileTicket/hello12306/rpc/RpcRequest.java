package com.mobileTicket.hello12306.rpc;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;

import org.json.JSONException;
import org.json.JSONObject;

public abstract class RpcRequest<T> {
    @NonNull
    public abstract String operationType();

    @NonNull
    public abstract JSONObject requestData() throws JSONException;

    public boolean isHttpGet() {
        return false;
    }

    public int signType() {
        return -1;
    }

    @WorkerThread
    @Nullable
    public abstract T onResponse(@NonNull String response);

}
