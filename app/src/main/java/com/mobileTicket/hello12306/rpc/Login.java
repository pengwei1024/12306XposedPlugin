package com.mobileTicket.hello12306.rpc;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.mobileTicket.hello12306.model.RequestResult;
import com.mobileTicket.hello12306.util.Md5Util;

import org.json.JSONException;
import org.json.JSONObject;

public class Login extends RpcRequest<RequestResult> {

    private String uid;
    private String pwd;

    public Login(String uid, String pwd) {
        this.uid = uid;
        this.pwd = pwd;
    }

    @NonNull
    @Override
    public String operationType() {
        return "com.cars.otsmobile.login";
    }

    @NonNull
    @Override
    public JSONObject requestData() throws JSONException {
        JSONObject jsonArrayItem = new JSONObject();
        JSONObject _requestBody = new JSONObject();
        _requestBody.put("user_name", uid);
        _requestBody.put("password", Md5Util.encrypt(pwd));
        jsonArrayItem.put("_requestBody", _requestBody);
        return jsonArrayItem;
    }

    @Nullable
    @Override
    public RequestResult onResponse(@NonNull String response) {
        return RequestResult.parseJson(response);
    }
}
