package com.mobileTicket.hello12306.widget;

import android.graphics.Bitmap;
import android.graphics.Picture;
import android.net.http.SslCertificate;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.ValueCallback;

import java.util.Map;

public interface APWebView {
    void addJavascriptInterface(Object obj, String str);

    boolean canGoBack();

    boolean canGoBackOrForward(int i);

    boolean canGoForward();

    Picture capturePicture();

    void clearCache(boolean z);

    void clearFormData();

    void clearHistory();

    void clearSslPreferences();

    void destroy();

    boolean dispatchKeyEvent(KeyEvent keyEvent);

    void evaluateJavascript(String str, ValueCallback<String> valueCallback);

    void flingScroll(int i, int i2);

    void freeMemory();

    SslCertificate getCertificate();

    int getContentHeight();

    int getContentWidth();

    Bitmap getFavicon();


    String[] getHttpAuthUsernamePassword(String str, String str2);

    String getOriginalUrl();

    int getProgress();

    int getScrollY();

    String getTitle();


    String getUrl();

    String getVersion();

    View getView();

    void goBack();

    void goBackOrForward(int i);

    void goForward();

    void invokeZoomPicker();

    boolean isPaused();

    void loadData(String str, String str2, String str3);

    void loadDataWithBaseURL(String str, String str2, String str3, String str4, String str5);

    void loadUrl(String str);

    void loadUrl(String str, Map<String, String> map);

    void onPause();

    void onResume();

    boolean overlayHorizontalScrollbar();

    boolean overlayVerticalScrollbar();

    boolean pageDown(boolean z);

    boolean pageUp(boolean z);

    void postUrl(String str, byte[] bArr);

    void reload();

    void removeJavascriptInterface(String str);


    void savePassword(String str, String str2, String str3);

    void setHorizontalScrollBarEnabled(boolean z);

    void setHorizontalScrollbarOverlay(boolean z);

    void setHttpAuthUsernamePassword(String str, String str2, String str3, String str4);

    void setInitialScale(int i);

    void setNetworkAvailable(boolean z);


    void setVerticalScrollBarEnabled(boolean z);

    void setVerticalScrollbarOverlay(boolean z);


    void setWebContentsDebuggingEnabled(boolean z);


    void stopLoading();

    boolean zoomIn();

    boolean zoomOut();
}