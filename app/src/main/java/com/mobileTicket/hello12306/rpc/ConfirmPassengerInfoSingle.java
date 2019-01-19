package com.mobileTicket.hello12306.rpc;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.mobileTicket.hello12306.model.OrderConfig;
import com.mobileTicket.hello12306.model.RequestResult;
import com.mobileTicket.hello12306.model.Trains;

import org.json.JSONException;
import org.json.JSONObject;

public class ConfirmPassengerInfoSingle extends RpcRequest<RequestResult> {

    private Trains train;

    public ConfirmPassengerInfoSingle(Trains train) {
        this.train = train;
    }

    @NonNull
    @Override
    public String operationType() {
        return "com.cars.otsmobile.confirmPassengerInfoSingle";
    }

    @NonNull
    @Override
    public JSONObject requestData() throws JSONException {
        JSONObject jsonArrayItem = new JSONObject();
        JSONObject _requestBody = new JSONObject();
        _requestBody.put("train_no", train.trainNo);
        _requestBody.put("start_time", train.startTime);
        _requestBody.put("station_train_code", train.code);
        _requestBody.put("from_station_telecode", train.from_station_telecode);
        _requestBody.put("to_station_telecode", train.to_station_telecode);
        _requestBody.put("yp_info", train.yp_info);
        _requestBody.put("from_station", train.from_station_telecode);
        _requestBody.put("to_station", train.to_station_telecode);
        _requestBody.put("train_date", train.train_date);
        _requestBody.put("seat_type_codes", OrderConfig.INSTANCE.seat_type_codes(train));
        _requestBody.put("ticket_types", OrderConfig.INSTANCE.ticket_types());
        _requestBody.put("ticket_type_order_num", String.valueOf(OrderConfig.INSTANCE.passenger.length));
        _requestBody.put("passenger_id_types", OrderConfig.INSTANCE.passenger_id_types());
        _requestBody.put("passenger_id_nos", OrderConfig.INSTANCE.passenger_id_nos());
        _requestBody.put("passenger_names", OrderConfig.INSTANCE.passenger_names());
        _requestBody.put("location_code", train.location_code);
        _requestBody.put("choose_seats", "");
        _requestBody.put("p_str", "");
        _requestBody.put("tour_flag", "dc");
        _requestBody.put("purpose_codes", "00");
        _requestBody.put("dynamicProp", "0+0+0+0");
        _requestBody.put("bed_level_order_num", "0");
        _requestBody.put("bed_level", "");
        _requestBody.put("mobile_nos", OrderConfig.INSTANCE.mobile_nos());
        _requestBody.put("save_passenger_flag", OrderConfig.INSTANCE.save_passenger_flag());
        _requestBody.put("passenger_flag", "1");
        _requestBody.put("dfpStr", train.getDfpStr());
        jsonArrayItem.put("_requestBody", _requestBody);
        return jsonArrayItem;
    }

    @Nullable
    @Override
    public RequestResult onResponse(@NonNull String response) {
        return RequestResult.parseJson(response);
    }

    @Override
    public int signType() {
        return 10;
    }
}
