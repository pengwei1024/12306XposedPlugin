package com.mobileTicket.hello12306.util;

import android.app.Activity;
import android.os.Looper;
import android.support.annotation.AnyThread;
import android.support.annotation.Nullable;
import android.util.Log;


import com.mobileTicket.hello12306.model.Page;
import com.mobileTicket.hello12306.widget.XWebView;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class PageManager {
    private static final String TAG = "PageManager";
    private volatile static PageManager singleton;
    private List<Page> pages = new CopyOnWriteArrayList<>();

    public static PageManager getInstance() {
        if (singleton == null) {
            synchronized (PageManager.class) {
                if (singleton == null) {
                    singleton = new PageManager();
                }
            }
        }
        return singleton;
    }

    public void push(Page page) {
        pages.add(page);
        Log.d(TAG, "push Page:" + page);
    }

    public void pop(Page page) {
        boolean result = pages.remove(page);
        Log.d(TAG, "pop Page:" + page + ", result:" + result);
    }

    @Nullable
    public Page getTopPage() {
        if (pages.isEmpty()) {
            return null;
        }
        return pages.get(pages.size() - 1);
    }

    public List<Page> getPages() {
        return pages;
    }

    @AnyThread
    public void runJs(final String js) {
        final Page page = getTopPage();
        if (page == null) {
            Log.w(TAG, "page is Null");
            return;
        }
        final XWebView webView = page.getWebView();
        if (webView != null) {
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    try {
                        webView.evaluateJavascriptWithException(js, null);
                    } catch (Exception e) {
                        Log.e(TAG, "evaluateJavascriptWithException", e);
                        if (e instanceof InvocationTargetException) {
                            Throwable target = ((InvocationTargetException) e).getTargetException();
                            if (target != null && target.getMessage() != null
                                    && target.getMessage().contains("WebView had destroyed")) {
                                Log.i(TAG, "pop webView: " + webView.hashCode());
                                page.popWebView();
                            }
                        }
                    }
                }
            };
            if (Looper.getMainLooper() != Looper.myLooper()) {
                page.getActivity().runOnUiThread(runnable);
            } else {
                runnable.run();
            }
            Log.i(TAG, "webView:" + webView.hashCode() + ", queryJs: " + js);
        } else {
            Log.w(TAG, "getWebView is Null, page=" + page.hashCode() + ", pageSize=" + pages.size()
            + ", activity=" + page.getActivity().getClass());
        }
    }

    @Nullable
    public Activity getContext() {
        Page page = getTopPage();
        if (page != null && page.getActivity() != null) {
            return page.getActivity();
        }
        return null;
    }
}
