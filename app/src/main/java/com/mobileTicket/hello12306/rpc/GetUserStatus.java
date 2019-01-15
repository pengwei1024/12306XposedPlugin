package com.mobileTicket.hello12306.rpc;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.mobileTicket.hello12306.model.RequestResult;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * 获取用户状态
 * {"error_msg":"","integration_flag":"3","member_status":"-1","succ_flag":"1"}
 */
public class GetUserStatus extends RpcRequest<RequestResult> {

    @NonNull
    @Override
    public String operationType() {
        return "com.cars.otsmobile.getUserStatus";
    }

    @NonNull
    @Override
    public JSONObject requestData() throws JSONException {
        JSONObject jsonArrayItem = new JSONObject();
        JSONObject _requestBody = new JSONObject();
        jsonArrayItem.put("_requestBody", _requestBody);
        return jsonArrayItem;
    }

    @Nullable
    @Override
    public RequestResult onResponse(@NonNull String response) {
        return RequestResult.parseJson(response);
    }
}
