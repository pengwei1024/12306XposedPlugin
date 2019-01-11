package com.alipay.mobile.nebula.webview;

import android.graphics.Bitmap;
import android.os.Message;
import android.view.View;
import android.webkit.ConsoleMessage;
import android.webkit.GeolocationPermissions.Callback;
import android.webkit.ValueCallback;

public interface APWebChromeClient {

    public interface CustomViewCallback {
        void onCustomViewHidden();
    }

    Bitmap getDefaultVideoPoster();

    View getVideoLoadingProgressView();

    void getVisitedHistory(ValueCallback<String[]> valueCallback);

    void onCloseWindow(APWebView aPWebView);

    boolean onConsoleMessage(ConsoleMessage consoleMessage);

    boolean onCreateWindow(APWebView aPWebView, boolean z, boolean z2, Message message);

    void onGeolocationPermissionsHidePrompt();

    void onGeolocationPermissionsShowPrompt(String str, Callback callback);

    void onHideCustomView();

    boolean onJsAlert(APWebView aPWebView, String str, String str2, APJsResult aPJsResult);

    boolean onJsBeforeUnload(APWebView aPWebView, String str, String str2, APJsResult aPJsResult);

    boolean onJsConfirm(APWebView aPWebView, String str, String str2, APJsResult aPJsResult);

    boolean onJsPrompt(APWebView aPWebView, String str, String str2, String str3, APJsPromptResult aPJsPromptResult);

    void onProgressChanged(APWebView aPWebView, int i);

    void onReceivedIcon(APWebView aPWebView, Bitmap bitmap);

    void onReceivedTitle(APWebView aPWebView, String str);

    void onReceivedTouchIconUrl(APWebView aPWebView, String str, boolean z);

    void onRequestFocus(APWebView aPWebView);

    void onShowCustomView(View view, CustomViewCallback customViewCallback);

    void openFileChooser(ValueCallback valueCallback, boolean z);
}