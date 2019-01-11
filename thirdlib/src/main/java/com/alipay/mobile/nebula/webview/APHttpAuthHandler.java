package com.alipay.mobile.nebula.webview;

public interface APHttpAuthHandler {
    void cancel();

    void proceed(String str, String str2);

    boolean useHttpAuthUsernamePassword();
}