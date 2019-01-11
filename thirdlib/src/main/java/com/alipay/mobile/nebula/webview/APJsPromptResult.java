package com.alipay.mobile.nebula.webview;

public interface APJsPromptResult {
    void cancel();

    void confirm(String str);
}