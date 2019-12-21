package com.mobileTicket.hello12306.rpc;

import android.support.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * 查询余票代理类
 */
public class QueryTicketProxy {

    @Nullable
    private String trainDate;
    @Nullable
    private String from;
    @Nullable
    private String to;

    QueryTicketProxy(@Nullable String trainDate, @Nullable String from, @Nullable String to) {
        this.trainDate = trainDate;
        this.from = from;
        this.to = to;
    }

    public String operationType() {
        return "com.cars.otsmobile.queryLeftTicketZ";
    }

    public JSONObject requestData() throws JSONException {
        // 必须根据cacheDataKeys来排序，官方排序参考 com.MobileTicket.common.plugins.RpcWithBaseDTOPlugin#getSortedJSON
        JSONObject jsonArrayItem = new JSONObject();
        jsonArrayItem.put("train_date", trainDate);
        jsonArrayItem.put("purpose_codes", "00");
        jsonArrayItem.put("from_station", from);
        jsonArrayItem.put("to_station", to);
        jsonArrayItem.put("station_train_code", "");
        jsonArrayItem.put("start_time_begin", "0000");
        jsonArrayItem.put("start_time_end", "2400");
        jsonArrayItem.put("train_headers", "QB#");
        jsonArrayItem.put("train_flag", "");
        jsonArrayItem.put("seat_type", "0");
        jsonArrayItem.put("seatBack_Type", "");
        jsonArrayItem.put("ticket_num", "");
        jsonArrayItem.put("dfpStr", "");
        return jsonArrayItem;
    }

    public boolean isHttpGet() {
        return true;
    }
}
