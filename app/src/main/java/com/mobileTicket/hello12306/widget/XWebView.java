package com.mobileTicket.hello12306.widget;

import android.graphics.Bitmap;
import android.graphics.Picture;
import android.net.http.SslCertificate;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.ValueCallback;

import java.lang.reflect.Method;
import java.util.Map;

public class XWebView implements APWebView {
    private final Object aPWebView;
    private final Class<?> aPWebViewCls;

    public XWebView(Object aPWebView) {
        this.aPWebView = aPWebView;
        this.aPWebViewCls = aPWebView.getClass();
    }

    @Override
    public void addJavascriptInterface(Object obj, String str) {

    }

    @Override
    public boolean canGoBack() {
        return false;
    }

    @Override
    public boolean canGoBackOrForward(int i) {
        return false;
    }

    @Override
    public boolean canGoForward() {
        return false;
    }

    @Override
    public Picture capturePicture() {
        return null;
    }

    @Override
    public void clearCache(boolean z) {

    }

    @Override
    public void clearFormData() {

    }

    @Override
    public void clearHistory() {

    }

    @Override
    public void clearSslPreferences() {

    }

    @Override
    public void destroy() {

    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent keyEvent) {
        return false;
    }

    @Override
    public void evaluateJavascript(String js, ValueCallback<String> valueCallback) {
        try {
            Method method = aPWebViewCls.getDeclaredMethod("loadUrl", String.class);
            if (method != null) {
                method.invoke(aPWebView, "javascript:" + js);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void flingScroll(int i, int i2) {

    }

    @Override
    public void freeMemory() {

    }

    @Override
    public SslCertificate getCertificate() {
        return null;
    }

    @Override
    public int getContentHeight() {
        return 0;
    }

    @Override
    public int getContentWidth() {
        return 0;
    }

    @Override
    public Bitmap getFavicon() {
        return null;
    }

    @Override
    public String[] getHttpAuthUsernamePassword(String str, String str2) {
        return new String[0];
    }

    @Override
    public String getOriginalUrl() {
        return null;
    }

    @Override
    public int getProgress() {
        return 0;
    }

    @Override
    public int getScrollY() {
        return 0;
    }

    @Override
    public String getTitle() {
        try {
            Method method = aPWebViewCls.getDeclaredMethod("getTitle");
            if (method != null) {
                return (String) method.invoke(aPWebView);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    @Override
    public String getUrl() {
        try {
            Method method = aPWebViewCls.getDeclaredMethod("getUrl");
            if (method != null) {
                return (String) method.invoke(aPWebView);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public String getVersion() {
        return null;
    }

    @Override
    public View getView() {
        return null;
    }

    @Override
    public void goBack() {
        try {
            Method method = aPWebViewCls.getDeclaredMethod("goBack");
            if (method != null) {
                method.invoke(aPWebView);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void goBackOrForward(int i) {

    }

    @Override
    public void goForward() {

    }

    @Override
    public void invokeZoomPicker() {

    }

    @Override
    public boolean isPaused() {
        return false;
    }

    @Override
    public void loadData(String str, String str2, String str3) {

    }

    @Override
    public void loadDataWithBaseURL(String str, String str2, String str3, String str4, String str5) {

    }

    @Override
    public void loadUrl(String str) {

    }

    @Override
    public void loadUrl(String str, Map<String, String> map) {

    }

    @Override
    public void onPause() {

    }

    @Override
    public void onResume() {

    }

    @Override
    public boolean overlayHorizontalScrollbar() {
        return false;
    }

    @Override
    public boolean overlayVerticalScrollbar() {
        return false;
    }

    @Override
    public boolean pageDown(boolean z) {
        return false;
    }

    @Override
    public boolean pageUp(boolean z) {
        return false;
    }

    @Override
    public void postUrl(String str, byte[] bArr) {

    }

    @Override
    public void reload() {

    }

    @Override
    public void removeJavascriptInterface(String str) {

    }

    @Override
    public void savePassword(String str, String str2, String str3) {

    }

    @Override
    public void setHorizontalScrollBarEnabled(boolean z) {

    }

    @Override
    public void setHorizontalScrollbarOverlay(boolean z) {

    }

    @Override
    public void setHttpAuthUsernamePassword(String str, String str2, String str3, String str4) {

    }

    @Override
    public void setInitialScale(int i) {

    }

    @Override
    public void setNetworkAvailable(boolean z) {

    }

    @Override
    public void setVerticalScrollBarEnabled(boolean z) {

    }

    @Override
    public void setVerticalScrollbarOverlay(boolean z) {

    }

    @Override
    public void setWebContentsDebuggingEnabled(boolean z) {
        try {
            Method setWebContentsDebuggingEnabled = aPWebViewCls.getDeclaredMethod("setWebContentsDebuggingEnabled", boolean.class);
            setWebContentsDebuggingEnabled.invoke(aPWebView, z);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void stopLoading() {

    }

    @Override
    public boolean zoomIn() {
        return false;
    }

    @Override
    public boolean zoomOut() {
        return false;
    }

    @Override
    public int hashCode() {
        return aPWebView.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof XWebView)) {
            return false;
        }
        return aPWebView.equals(((XWebView) obj).aPWebView);
    }
}
