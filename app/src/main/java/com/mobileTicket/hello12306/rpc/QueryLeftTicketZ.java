package com.mobileTicket.hello12306.rpc;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.mobileTicket.hello12306.model.ActionRunnable;
import com.mobileTicket.hello12306.model.OrderConfig;
import com.mobileTicket.hello12306.model.Trains;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class QueryLeftTicketZ extends RpcRequest<Trains> {

    private static final Map<String, Long> blackList = new ConcurrentHashMap<>();

    @Nullable
    private String trainDate;
    @Nullable
    private String from;
    @Nullable
    private String to;
    @Nullable
    private ActionRunnable<JSONArray> callback;

    public QueryLeftTicketZ(@Nullable String trainDate, @Nullable String from, @Nullable String to,
                            @Nullable ActionRunnable<JSONArray> callback) {
        this.trainDate = trainDate;
        this.from = from;
        this.to = to;
        this.callback = callback;
    }

    @NonNull
    @Override
    public String operationType() {
        return "com.cars.otsmobile.queryLeftTicketZ";
    }

    @NonNull
    @Override
    public JSONObject requestData() throws JSONException {
        JSONObject jsonArrayItem = new JSONObject();
        jsonArrayItem.put("train_date", trainDate);
        jsonArrayItem.put("from_station", from);
        jsonArrayItem.put("to_station", to);
        jsonArrayItem.put("station_train_code", "");
        jsonArrayItem.put("train_headers", "QB#");
        jsonArrayItem.put("train_flag", "");
        jsonArrayItem.put("start_time_begin", "0000");
        jsonArrayItem.put("start_time_end", "2400");
        jsonArrayItem.put("seat_type", "0");
        jsonArrayItem.put("ticket_num", "");
        jsonArrayItem.put("seatBack_Type", "");
        jsonArrayItem.put("dfpStr", "");
        jsonArrayItem.put("purpose_codes", "00");
        return jsonArrayItem;
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
        return true;
    }

    public static void addBlackList(@NonNull String code) {
        blackList.put(code, System.currentTimeMillis());
    }
}
