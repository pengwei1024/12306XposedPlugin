package com.alipay.mobile.nebula.webview;

import android.content.Context;

public interface APWebSettings {
    public static final int LOAD_CACHE_ELSE_NETWORK = 1;
    public static final int LOAD_CACHE_ONLY = 3;
    public static final int LOAD_DEFAULT = -1;
    @Deprecated
    public static final int LOAD_NORMAL = 0;
    public static final int LOAD_NO_CACHE = 2;

    public enum LayoutAlgorithm {
        NORMAL,
        SINGLE_COLUMN,
        NARROW_COLUMNS,
        TEXT_AUTOSIZING
    }

    public enum PluginState {
        ON,
        ON_DEMAND,
        OFF
    }

    public enum RenderPriority {
        NORMAL,
        HIGH,
        LOW
    }

    public enum TextSize {
        SMALLEST(50),
        SMALLER(75),
        NORMAL(100),
        LARGER(150),
        LARGEST(200);

        int value;

        private TextSize(int size) {
            this.value = size;
        }
    }

    public enum ZoomDensity {
        FAR(150),
        MEDIUM(100),
        CLOSE(75);

        int value;

        private ZoomDensity(int size) {
            this.value = size;
        }

        public final int getValue() {
            return this.value;
        }
    }

    boolean getAllowContentAccess();

    boolean getAllowFileAccess();

    boolean getAllowFileAccessFromFileURLs();

    boolean getAllowUniversalAccessFromFileURLs();

    boolean getBlockNetworkImage();

    boolean getBuiltInZoomControls();

    int getCacheMode();

    String getCursiveFontFamily();

    boolean getDatabaseEnabled();

    String getDatabasePath();

    int getDefaultFixedFontSize();

    int getDefaultFontSize();

    String getDefaultTextEncodingName();

    String getDefaultUserAgent(Context context);

    ZoomDensity getDefaultZoom();

    boolean getDisplayZoomControls();

    boolean getDomStorageEnabled();

    String getFantasyFontFamily();

    String getFixedFontFamily();

    boolean getJavaScriptCanOpenWindowsAutomatically();

    boolean getJavaScriptEnabled();

    LayoutAlgorithm getLayoutAlgorithm();

    boolean getLoadWithOverviewMode();

    boolean getLoadsImagesAutomatically();

    boolean getMediaPlaybackRequiresUserGesture();

    int getMinimumFontSize();

    int getMinimumLogicalFontSize();

    PluginState getPluginState();

    String getSansSerifFontFamily();

    boolean getSaveFormData();

    boolean getSavePassword();

    String getSerifFontFamily();

    String getStandardFontFamily();

    TextSize getTextSize();

    int getTextZoom();

    boolean getUseWideViewPort();

    String getUserAgentString();

    void setAllowContentAccess(boolean z);

    void setAllowFileAccess(boolean z);

    void setAllowFileAccessFromFileURLs(boolean z);

    void setAllowUniversalAccessFromFileURLs(boolean z);

    void setAppCacheEnabled(boolean z);

    void setAppCachePath(String str);

    void setBlockNetworkImage(boolean z);

    void setBuiltInZoomControls(boolean z);

    void setCacheMode(int i);

    void setCursiveFontFamily(String str);

    void setDatabaseEnabled(boolean z);

    void setDatabasePath(String str);

    void setDefaultFixedFontSize(int i);

    void setDefaultFontSize(int i);

    void setDefaultTextEncodingName(String str);

    void setDisplayZoomControls(boolean z);

    void setDomStorageEnabled(boolean z);

    void setEnableFastScroller(boolean z);

    void setFantasyFontFamily(String str);

    void setFixedFontFamily(String str);

    void setGeolocationDatabasePath(String str);

    void setGeolocationEnabled(boolean z);

    void setJavaScriptCanOpenWindowsAutomatically(boolean z);

    void setJavaScriptEnabled(boolean z);

    void setLayoutAlgorithm(LayoutAlgorithm layoutAlgorithm);

    void setLoadWithOverviewMode(boolean z);

    void setLoadsImagesAutomatically(boolean z);

    void setMediaPlaybackRequiresUserGesture(boolean z);

    void setMinimumFontSize(int i);

    void setMinimumLogicalFontSize(int i);

    void setNeedInitialFocus(boolean z);

    void setPageCacheCapacity(int i);

    void setPluginState(PluginState pluginState);

    void setSansSerifFontFamily(String str);

    void setSaveFormData(boolean z);

    void setSavePassword(boolean z);

    void setSerifFontFamily(String str);

    void setStandardFontFamily(String str);

    void setSupportMultipleWindows(boolean z);

    void setSupportZoom(boolean z);

    void setTextSize(TextSize textSize);

    void setTextZoom(int i);

    void setUseWideViewPort(boolean z);

    void setUserAgentString(String str);

    boolean supportMultipleWindows();

    boolean supportZoom();
}