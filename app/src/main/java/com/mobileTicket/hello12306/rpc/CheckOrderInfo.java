package com.mobileTicket.hello12306.rpc;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.mobileTicket.hello12306.model.RequestResult;
import com.mobileTicket.hello12306.model.Trains;

import org.json.JSONException;
import org.json.JSONObject;

public class CheckOrderInfo extends RpcRequest<Trains> {

    private Trains train;

    public CheckOrderInfo(Trains train) {
        this.train = train;
    }

    @NonNull
    @Override
    public String operationType() {
        return "com.cars.otsmobile.checkOrderInfo";
    }

    @NonNull
    @Override
    public JSONObject requestData() throws JSONException {
        JSONObject jsonArrayItem = new JSONObject();
        JSONObject _requestBody = new JSONObject();
        _requestBody.put("tour_flag", "dc");
        _requestBody.put("secret_str", train.location_code);
        jsonArrayItem.put("_requestBody", _requestBody);
        return jsonArrayItem;
    }

    @Nullable
    @Override
    public Trains onResponse(@NonNull String response) {
        RequestResult result = RequestResult.parseJson(response);
        String location = result.getResponse().optString("location_code");
        if (!TextUtils.isEmpty(location) && result.isSuccess()) {
            train.location_code = location;
            return train;
        }
        return null;
    }
}
