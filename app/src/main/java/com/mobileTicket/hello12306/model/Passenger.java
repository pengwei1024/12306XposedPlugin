package com.mobileTicket.hello12306.model;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class Passenger implements Parcelable {
    // 名称
    private String name;
    // 身份证
    private String id;
    // 手机号码
    private String phone;

    public Passenger(String name, String id, String phone) {
        this.name = name;
        this.id = id;
        this.phone = phone;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.name);
        dest.writeString(this.id);
        dest.writeString(this.phone);
    }

    public Passenger() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    protected Passenger(Parcel in) {
        this.name = in.readString();
        this.id = in.readString();
        this.phone = in.readString();
    }

    public JSONObject toJson() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("name", name);
            jsonObject.put("id", id);
            jsonObject.put("phone", phone);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

    @Nullable
    public static Passenger parse(@Nullable JSONObject jsonObject) {
        if (jsonObject == null || jsonObject.length() == 0) {
            return null;
        }
        Passenger user = new Passenger();
        user.setId(jsonObject.optString("id"));
        user.setName(jsonObject.optString("name"));
        user.setPhone(jsonObject.optString("phone"));
        return user;
    }

    public static List<Passenger> parse(String json) {
        if (json == null) {
            return Collections.emptyList();
        }
        try {
            JSONArray jsonArray = new JSONArray(json);
            List<Passenger> users = new ArrayList<>(jsonArray.length());
            for (int i = 0; i < jsonArray.length(); i++) {
                users.add(parse(jsonArray.getJSONObject(i)));
            }
            return users;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return Collections.emptyList();
    }

    public static final Parcelable.Creator<Passenger> CREATOR = new Parcelable.Creator<Passenger>() {
        @Override
        public Passenger createFromParcel(Parcel source) {
            return new Passenger(source);
        }

        @Override
        public Passenger[] newArray(int size) {
            return new Passenger[size];
        }
    };

    @Override
    public String toString() {
        String idEncryption = id.substring(0, 6) + "****" + id.substring(10);
        return name + "(" + idEncryption + ")";
    }
}
