package com.alipay.mobile.nebula.webview;

public interface APWebViewListener {
    void onDetachedFromWindow();

    boolean overScrollBy(int i, int i2, int i3, int i4);
}