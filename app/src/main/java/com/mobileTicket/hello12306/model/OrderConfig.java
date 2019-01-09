package com.mobileTicket.hello12306.model;

import android.support.annotation.NonNull;
import android.util.Pair;

import com.mobileTicket.hello12306.util.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class OrderConfig {
    // 车次
    public List<String> trains;
    // 乘客
    public Passenger[] passenger;
    // 类型
    public SeatType seatType = SeatType.YW;
    // 登录用户
    public String loginUser;
    public String loginPassword;
    // 抢票日期
    public List<String> trainDate;
    // 车站信息
    public Pair<String, String> stationInfo;

    public OrderConfig setTrains(List<String> trains) {
        this.trains = trains;
        return this;
    }

    public OrderConfig setPassenger(Passenger[] passenger) {
        this.passenger = passenger;
        return this;
    }

    public OrderConfig setSeatType(SeatType seatType) {
        this.seatType = seatType;
        return this;
    }

    public OrderConfig setLoginUser(String loginUser) {
        this.loginUser = loginUser;
        return this;
    }

    public OrderConfig setLoginPassword(String loginPassword) {
        this.loginPassword = loginPassword;
        return this;
    }

    public OrderConfig setTrainDate(List<String> trainDate) {
        this.trainDate = trainDate;
        return this;
    }

    public OrderConfig setStationInfo(Pair<String, String> stationInfo) {
        this.stationInfo = stationInfo;
        return this;
    }

    // 默认配置
    private static final OrderConfig DEFAULT = new OrderConfig()
            .setTrainDate(Collections.<String>emptyList())
            .setStationInfo(Pair.create("BJP", "NCG"))
            .setTrains(Collections.<String>emptyList())
            .setPassenger(new Passenger[0])
            .setSeatType(SeatType.YW)
            .setLoginUser("")
            .setLoginPassword("");

    @NonNull
    public volatile static OrderConfig INSTANCE = DEFAULT;

    public String seat_type_codes() {
        return Utils.listToString(fill(INSTANCE.passenger.length,
                String.valueOf(INSTANCE.seatType.getSign())), ",");
    }

    public String ticket_types() {
        return Utils.listToString(fill(INSTANCE.passenger.length, "1"), ",");
    }

    public String passenger_id_types() {
        return Utils.listToString(fill(INSTANCE.passenger.length, "1"), ",");
    }

    public String passenger_id_nos() {
        List<String> seats = new ArrayList<>(INSTANCE.passenger.length);
        for (Passenger user : INSTANCE.passenger) {
            seats.add(user.getId());
        }
        return Utils.listToString(seats, ",");
    }

    public String passenger_names() {
        List<String> seats = new ArrayList<>(INSTANCE.passenger.length);
        for (Passenger user : INSTANCE.passenger) {
            seats.add(user.getName());
        }
        return Utils.listToString(seats, ",");
    }

    public String mobile_nos() {
        List<String> seats = new ArrayList<>(INSTANCE.passenger.length);
        for (Passenger user : INSTANCE.passenger) {
            seats.add(user.getPhone());
        }
        return Utils.listToString(seats, ",");
    }

    public String save_passenger_flag() {
        return Utils.listToString(fill(INSTANCE.passenger.length, "Y"), ",");
    }

    private <T> List<T> fill(int size, T t) {
        List<T> dataList = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            dataList.add(t);
        }
        return dataList;
    }

    @Override
    public String toString() {
        return INSTANCE.stationInfo.first + "->" + INSTANCE.stationInfo.second + "; "
                + passenger_names() + "; " + INSTANCE.seatType.getName();
    }
}
