package com.mobileTicket.hello12306.model;

import android.support.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

public class RequestResult {
    private boolean isSuccess;
    private String errorMsg = "";
    private String rtnMsg = "";
    private JSONObject response;

    public boolean isSuccess() {
        return isSuccess;
    }

    public void setSuccess(boolean success) {
        isSuccess = success;
    }

    @NonNull
    public String getErrorMsg() {
        return errorMsg;
    }

    @NonNull
    public JSONObject getResponse() {
        if (response == null) {
            return new JSONObject();
        }
        return response;
    }

    public String getRtnMsg() {
        return rtnMsg;
    }

    public void setRtnMsg(String rtnMsg) {
        this.rtnMsg = rtnMsg;
    }

    public void setResponse(JSONObject response) {
        this.response = response;
    }

    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }

    public static RequestResult parseJson(String response) {
        RequestResult result = new RequestResult();
        try {
            JSONObject jsonObject = new JSONObject(response);
            result.setSuccess("1".equals(jsonObject.optString("succ_flag")));
            result.setErrorMsg(jsonObject.optString("error_msg", ""));
            result.setRtnMsg(jsonObject.optString("rtnMsg", ""));
            result.setResponse(jsonObject);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return result;
    }

    public String getShowMsg() {
        return rtnMsg + ";" + errorMsg;
    }
}
