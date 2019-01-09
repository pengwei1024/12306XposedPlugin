package com.mobileTicket.hello12306.model;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.mobileTicket.hello12306.widget.XWebView;

import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

/**
 * H5Activity 的实例
 */
public class Page {
    private static final String TAG = "XPage";
    private Activity activity;
    private boolean isList;
    private Stack<XWebView> webViewHistory = new Stack<>();
    private Set<Integer> exitWebViewList = new HashSet<>();

    public Page(Activity activity) {
        this.activity = activity;
    }

    public XWebView getWebView() {
        return peekWebView();
    }

    public boolean isList() {
        return isList;
    }

    public void setList(boolean list) {
        isList = list;
    }

    public Activity getActivity() {
        return activity;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Page)) {
            return false;
        }
        return activity.equals(((Page) obj).activity);
    }

    @Override
    public int hashCode() {
        return activity.hashCode();
    }

    public void pushWebView(@NonNull XWebView webView) {
        if (webViewHistory.isEmpty() || !webViewHistory.peek().equals(webView)) {
            boolean exist = exitWebViewList.contains(webView.hashCode());
            Log.d(TAG, "pushWebView " + webView.hashCode() + " exist=" + exist);
            if (exist) {
                return;
            }
            webViewHistory.push(webView);
        }
    }

    public XWebView popWebView() {
        XWebView pop = webViewHistory.pop();
        if (pop != null) {
            exitWebViewList.add(pop.hashCode());
            Log.d(TAG, "popWebView " + pop.hashCode());
        }
        return pop;
    }

    @Nullable
    public XWebView peekWebView() {
        if (webViewHistory.isEmpty()) {
            return null;
        }
        return webViewHistory.peek();
    }
}
