package com.mobileTicket.hello12306.model;


import android.content.ContextWrapper;
import android.support.annotation.NonNull;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import de.robv.android.xposed.XposedHelpers;

public class BaseDTOEntity {

    private Class<?> BaseDTO;
    private Object targetObject;
    private Map<String, String> cacheMap = new ConcurrentHashMap<>();

    public BaseDTOEntity(ClassLoader classLoader, ContextWrapper contextWrapper) {
        try {
            Class<?> TicketNetRequest = classLoader.loadClass("com.MobileTicket.common.rpc.TicketNetRequest");
            BaseDTO = classLoader.loadClass("com.MobileTicket.common.rpc.model.BaseDTO");
            targetObject = XposedHelpers.callStaticMethod(TicketNetRequest, "getBaseDTO",
                    new Class<?>[]{ContextWrapper.class}, contextWrapper);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    @NonNull
    public String getDeviceId() {
        return getField("device_no");
    }

    @NonNull
    public String getProductVersion() {
        return getField("version_no");
    }

    @NonNull
    public String getMobile() {
        return getField("mobile_no");
    }

    @NonNull
    public String getUserName() {
        return getField("user_name");
    }

    @NonNull
    private String getField(String key) {
        String result = cacheMap.get(key);
        if (result == null) {
            try {
                if (BaseDTO != null && targetObject != null) {
                    Field field = BaseDTO.getField(key);
                    Object object = field.get(targetObject);
                    if (object != null) {
                        result = String.valueOf(object);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (result == null) {
                result = "";
            }
            cacheMap.put(key, result);
        }
        return result;
    }
}
