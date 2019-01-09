package com.mobileTicket.hello12306.dao;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;


import com.mobileTicket.hello12306.model.Passenger;

import org.json.JSONArray;
import org.json.JSONException;
import org.litepal.annotation.Column;
import org.litepal.crud.LitePalSupport;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 抢票任务记录实例
 */
public class TaskDao extends LitePalSupport implements Parcelable {
    private int id;
    @Column(nullable = false)
    private String fromStationName;
    @Column(nullable = false)
    private String fromStationCode;
    @Column(nullable = false)
    private String toStationName;
    @Column(nullable = false)
    private String toStationCode;
    @Column(nullable = false)
    private List<String> trainDate;
    @Column(nullable = false)
    private List<String> trainList;
    @Column(nullable = false)
    private String passengerList;
    @Column(defaultValue = "0", nullable = false)
    private int status;
    private String type;
    private String uid;
    private String pwd;

    public String getFromStationName() {
        return fromStationName;
    }

    public void setFromStationName(String fromStationName) {
        this.fromStationName = fromStationName;
    }

    public String getFromStationCode() {
        return fromStationCode;
    }

    public void setFromStationCode(String fromStationCode) {
        this.fromStationCode = fromStationCode;
    }

    public String getToStationName() {
        return toStationName;
    }

    public void setToStationName(String toStationName) {
        this.toStationName = toStationName;
    }

    public String getToStationCode() {
        return toStationCode;
    }

    public void setToStationCode(String toStationCode) {
        this.toStationCode = toStationCode;
    }

    public List<String> getTrainDate() {
        return trainDate;
    }

    public void setTrainDate(List<String> trainDate) {
        this.trainDate = trainDate;
    }

    public List<String> getTrainList() {
        return trainList;
    }

    public void setTrainList(List<String> trainList) {
        this.trainList = trainList;
    }

    public String getPassengerJson() {
        return passengerList;
    }

    public List<Passenger> getPassengerList() {
        if (TextUtils.isEmpty(this.passengerList)) {
            return Collections.emptyList();
        }
        List<Passenger> userList = new ArrayList<>();
        try {
            JSONArray jsonArray = new JSONArray(this.passengerList);
            for (int i = 0; i < jsonArray.length(); i++) {
                Passenger user = Passenger.parse(jsonArray.optJSONObject(i));
                if (user != null) {
                    userList.add(user);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return userList;
    }

    public void setPassengerList(List<Passenger> passengerList) {
        JSONArray jsonArray = new JSONArray();
        for (Passenger user : passengerList) {
            jsonArray.put(user.toJson());
        }
        this.passengerList = jsonArray.toString();
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getPwd() {
        return pwd;
    }

    public void setPwd(String pwd) {
        this.pwd = pwd;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public int getId() {
        return id;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.fromStationName);
        dest.writeString(this.fromStationCode);
        dest.writeString(this.toStationName);
        dest.writeString(this.toStationCode);
        dest.writeStringList(this.trainDate);
        dest.writeStringList(this.trainList);
        dest.writeString(this.passengerList);
        dest.writeInt(this.status);
        dest.writeString(this.uid);
        dest.writeString(this.pwd);
    }

    public TaskDao() {
    }

    protected TaskDao(Parcel in) {
        this.fromStationName = in.readString();
        this.fromStationCode = in.readString();
        this.toStationName = in.readString();
        this.toStationCode = in.readString();
        this.trainDate = in.createStringArrayList();
        this.trainList = in.createStringArrayList();
        this.passengerList = in.readString();
        this.status = in.readInt();
        this.uid = in.readString();
        this.pwd = in.readString();
    }

    public static final Parcelable.Creator<TaskDao> CREATOR = new Parcelable.Creator<TaskDao>() {
        @Override
        public TaskDao createFromParcel(Parcel source) {
            return new TaskDao(source);
        }

        @Override
        public TaskDao[] newArray(int size) {
            return new TaskDao[size];
        }
    };
}
