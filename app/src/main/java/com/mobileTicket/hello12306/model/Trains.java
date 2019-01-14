package com.mobileTicket.hello12306.model;

import android.support.annotation.WorkerThread;
import android.text.TextUtils;

import com.mobileTicket.hello12306.util.Utils;

import org.json.JSONObject;

import java.util.HashMap;

public class Trains {

    public String code;
    public String fromStation;
    public String toStation;
    public String ypInfoCover;
    public String yp_info;
    public String arriveTime;
    public String startTime;
    public String intervalTime;
    public String trainNo;
    public String location_code;
    public String train_date;
    public String from_station_telecode;
    public String to_station_telecode;
    public String start_train_date;
    public String message;
    private HashMap<SeatType, Integer> ticketInfo = new HashMap<>();
    private String dfpStr;
    public JSONObject targetJson;

    @WorkerThread
    public static Trains loads(JSONObject item) {
        Trains trains = new Trains();
        trains.targetJson = item;
        trains.code = item.optString("station_train_code");
        trains.fromStation = item.optString("from_station_name");
        trains.toStation = item.optString("to_station_name");
        trains.ypInfoCover = item.optString("yp_info_cover");
        trains.arriveTime = item.optString("arrive_time");
        trains.yp_info = item.optString("yp_info");
        trains.startTime = item.optString("start_time");
        trains.intervalTime = item.optString("lishi");
        trains.trainNo = item.optString("train_no");
        trains.location_code = item.optString("location_code");
        trains.train_date = item.optString("train_date");
        trains.from_station_telecode = item.optString("from_station_telecode");
        trains.to_station_telecode = item.optString("to_station_telecode");
        trains.start_train_date = item.optString("start_train_date");
        trains.message = item.optString("message");
        // 解析余票
        if (!TextUtils.isEmpty(trains.ypInfoCover)) {
            HashMap<SeatType, Integer> hashMap = trains.getTicketInfo();
            for (int i = 0; i < trains.ypInfoCover.length() / 10; i++) {
                String itemValue = trains.ypInfoCover.substring(i * 10, (i + 1) * 10);
                char flag = itemValue.charAt(0);
                int num = Utils.parseInt(itemValue.substring(itemValue.length() - 2));
                for (SeatType type : SeatType.values()) {
                    if (type.getSign() == flag) {
                        if (hashMap.containsKey(type)) {
                            hashMap.put(SeatType.WZ, hashMap.get(type));
                        }
                        hashMap.put(type, num);
                        break;
                    }
                }
            }
        }
        return trains;
    }

    public String getTicketNumString() {
        HashMap<SeatType, Integer> tickets = getTicketInfo();
        StringBuilder stringBuilder = new StringBuilder();
        for (SeatType seatType : tickets.keySet()) {
            stringBuilder.append(seatType.getName()).append(":").append(tickets.get(seatType)).append(",");
        }
        return stringBuilder.toString();
    }

    @Override
    public String toString() {
        return "Trains{" +
                code + ' ' + getTicketNumString() +
                ", fromStation='" + fromStation + '\'' +
                ", toStation='" + toStation + '\'' +
                ", ypInfoCover='" + ypInfoCover + '\'' +
                ", yp_info='" + yp_info + '\'' +
                ", arriveTime='" + arriveTime + '\'' +
                ", startTime='" + startTime + '\'' +
                ", intervalTime='" + intervalTime + '\'' +
                ", trainNo='" + trainNo + '\'' +
                ", location_code='" + location_code + '\'' +
                ", train_date='" + train_date + '\'' +
                ", start_train_date='" + start_train_date + '\'' +
                ", from_station_telecode='" + from_station_telecode + '\'' +
                ", to_station_telecode='" + to_station_telecode + '\'' +
                '}';
    }

    public int getTicketNum(SeatType seatType) {
        Integer count = getTicketInfo().get(seatType);
        if (count != null) {
            return count;
        }
        return 0;
    }

    /**
     * 是否满足抢票条件
     *
     * @param config Config
     * @return bool
     */
    public boolean isMatch(OrderConfig config) {
        if (config.trains.contains(code)) {
            return getTicketNum(config.seatType) >= config.passenger.length;
        }
        return false;
    }

    private HashMap<SeatType, Integer> getTicketInfo() {
        return ticketInfo;
    }

    public String getDfpStr() {
        return dfpStr;
    }

    public void setDfpStr(String dfpStr) {
        this.dfpStr = dfpStr;
    }
}
