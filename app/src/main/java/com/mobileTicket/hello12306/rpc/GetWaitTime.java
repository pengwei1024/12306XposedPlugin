package com.mobileTicket.hello12306.rpc;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Pair;

import com.mobileTicket.hello12306.model.ActionRunnable;
import com.mobileTicket.hello12306.model.RequestResult;
import com.mobileTicket.hello12306.util.Utils;

import org.json.JSONException;
import org.json.JSONObject;

public class GetWaitTime extends RpcRequest<Pair<Boolean, Integer>> {

    private ActionRunnable<String> callback;

    public GetWaitTime(ActionRunnable<String> callback) {
        this.callback = callback;
    }

    @NonNull
    @Override
    public String operationType() {
        return "com.cars.otsmobile.getWaitTime";
    }

    @NonNull
    @Override
    public JSONObject requestData() throws JSONException {
        JSONObject jsonArrayItem = new JSONObject();
        JSONObject _requestBody = new JSONObject();
        _requestBody.put("tourFlag", "dc");
        jsonArrayItem.put("_requestBody", _requestBody);
        return jsonArrayItem;
    }

    @Nullable
    @Override
    public Pair<Boolean, Integer> onResponse(@NonNull String response) {
        if (callback != null) {
            callback.run(response);
        }
        RequestResult result = RequestResult.parseJson(response);
        if (result.isSuccess() && result.getResponse().has("waitTime")) {
            String orderId = result.getResponse().optString("orderId");
            int waitTime = Utils.parseInt(result.getResponse().optString("waitTime"));
            if (!TextUtils.isEmpty(orderId)) {
                return Pair.create(true, -1);
            }
            return Pair.create(false, waitTime);
        }
        return Pair.create(false, -1);
    }
}
