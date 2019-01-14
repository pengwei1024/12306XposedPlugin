package com.mobileTicket.hello12306.rpc;

import android.support.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;


public class QueryPassenger extends RpcRequest<String> {
    @NonNull
    @Override
    public String operationType() {
        return "com.cars.otsmobile.queryPassenger";
    }

    @NonNull
    @Override
    public JSONObject requestData() throws JSONException {
        JSONObject jsonArrayItem = new JSONObject();
        JSONObject _requestBody = new JSONObject();
        jsonArrayItem.put("_requestBody", _requestBody);
        return jsonArrayItem;
    }

    @Override
    public String onResponse(@NonNull String response) {
        try {
            JSONObject jsonObject = new JSONObject(response);
            return jsonObject.optString("passengerResult");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }
}
