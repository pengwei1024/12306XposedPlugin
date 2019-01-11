package com.alipay.mobile.nebula.webview;

import android.graphics.Bitmap;
import android.net.http.SslError;
import android.os.Message;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.WebResourceResponse;

import java.util.HashMap;
import java.util.Map;

public interface APWebViewClient {
    void doUpdateVisitedHistory(APWebView aPWebView, String str, boolean z);

    View getEmbedView(int i, int i2, String str, String str2, Map<String, String> map);

    String getJSBridge();

    void onEmbedViewAttachedToWebView(int i, int i2, String str, String str2, Map<String, String> map);

    void onEmbedViewDetachedFromWebView(int i, int i2, String str, String str2, Map<String, String> map);

    void onFirstVisuallyRender(APWebView aPWebView);

    void onFormResubmission(APWebView aPWebView, Message message, Message message2);

    void onLoadResource(APWebView aPWebView, String str);

    void onPageFinished(APWebView aPWebView, String str, long j);

    void onPageStarted(APWebView aPWebView, String str, Bitmap bitmap);

    void onReceivedError(APWebView aPWebView, int i, String str, String str2);

    void onReceivedHttpAuthRequest(APWebView aPWebView, APHttpAuthHandler aPHttpAuthHandler, String str, String str2);

    void onReceivedLoginRequest(APWebView aPWebView, String str, String str2, String str3);

    void onReceivedSslError(APWebView aPWebView, APSslErrorHandler aPSslErrorHandler, SslError sslError);

    void onResourceFinishLoad(APWebView aPWebView, String str, long j);

    void onResourceResponse(APWebView aPWebView, HashMap<String, String> hashMap);

    void onScaleChanged(APWebView aPWebView, float f, float f2);

    void onTooManyRedirects(APWebView aPWebView, Message message, Message message2);

    void onUnhandledKeyEvent(APWebView aPWebView, KeyEvent keyEvent);

    void onWebViewEvent(APWebView aPWebView, int i, Object obj);

    WebResourceResponse shouldInterceptRequest(APWebView aPWebView, APWebResourceRequest aPWebResourceRequest);

    WebResourceResponse shouldInterceptRequest(APWebView aPWebView, String str);

    boolean shouldInterceptResponse(APWebView aPWebView, HashMap<String, String> hashMap);

    boolean shouldOverrideKeyEvent(APWebView aPWebView, KeyEvent keyEvent);

    boolean shouldOverrideUrlLoading(APWebView aPWebView, String str);
}