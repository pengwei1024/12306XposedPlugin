package com.alipay.mobile.nebula.webview;

import android.graphics.Bitmap;

public interface APWebHistoryItem {
    Bitmap getFavicon();

    String getOriginalUrl();

    String getTitle();

    String getUrl();
}