package com.mobileTicket.hello12306.rpc;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;


public class QueryTicketString extends RpcRequest<String> {

    private final QueryTicketProxy proxy;

    public QueryTicketString(@Nullable String trainDate, @Nullable String from, @Nullable String to) {
        proxy = new QueryTicketProxy(trainDate, from, to);
    }

    @NonNull
    @Override
    public String operationType() {
        return proxy.operationType();
    }

    @NonNull
    @Override
    public JSONObject requestData() throws JSONException {
        return proxy.requestData();
    }

    @Nullable
    @Override
    public String onResponse(@NonNull String response) {
        return response;
    }

    @Override
    public boolean isHttpGet() {
        return proxy.isHttpGet();
    }
}
