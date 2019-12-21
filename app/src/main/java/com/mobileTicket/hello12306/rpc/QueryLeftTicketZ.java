package com.mobileTicket.hello12306.rpc;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.mobileTicket.hello12306.model.ActionRunnable;
import com.mobileTicket.hello12306.model.OrderConfig;
import com.mobileTicket.hello12306.model.Trains;
import com.mobileTicket.hello12306.util.LogUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class QueryLeftTicketZ extends RpcRequest<Trains> {

    private static final Map<String, Long> blackList = new ConcurrentHashMap<>();

    private final QueryTicketProxy proxy;
    @Nullable
    private ActionRunnable<JSONArray> callback;

    public QueryLeftTicketZ(@Nullable String trainDate, @Nullable String from, @Nullable String to,
                            @Nullable ActionRunnable<JSONArray> callback) {
        proxy = new QueryTicketProxy(trainDate, from, to);
        this.callback = callback;
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
    public Trains onResponse(@NonNull String response) {
        try {
            JSONObject jsonObject = new JSONObject(response);
            JSONArray array = jsonObject.optJSONArray("ticketResult");
            if (array == null) {
                return null;
            }
            if (callback != null) {
                callback.run(array);
            }
            for (int i = 0; i < array.length(); i++) {
                JSONObject item = array.optJSONObject(i);
                Trains trains = Trains.loads(item);
                if (trains.isMatch(OrderConfig.INSTANCE)) {
                    if (blackList.containsKey(trains.code)) {
                        Long blackTime = blackList.get(trains.code);
                        if (blackTime != null) {
                            if (System.currentTimeMillis() - blackTime >= 10_000) {
                                blackList.remove(trains.code);
                                LogUtil.print("移出小黑屋:" + trains.code);
                            } else {
                                continue;
                            }
                        }
                    }
                    return trains;
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public boolean isHttpGet() {
        return proxy.isHttpGet();
    }

    public static void addBlackList(@NonNull String code) {
        LogUtil.print("加入小黑屋:" + code);
        blackList.put(code, System.currentTimeMillis());
    }
}
