package com.mobileTicket.hello12306.util;

import android.app.Activity;
import android.os.Looper;
import android.support.annotation.AnyThread;
import android.support.annotation.Nullable;
import android.util.Log;


import com.mobileTicket.hello12306.model.Page;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class PageManager {
    private static final String TAG = "PageManager";
    private static PageManager singleton;
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
        pages.remove(page);
        Log.d(TAG, "pop Page:" + page);
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
        if (page != null && page.getWebView() != null) {
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    page.getWebView().evaluateJavascript(js, null);
                }
            };
            if (Looper.getMainLooper() != Looper.myLooper()) {
                page.getActivity().runOnUiThread(runnable);
            } else {
                runnable.run();
            }
            Log.d("queryJs", js);
        } else {
            Log.w(TAG, "page== null? or getWebView? " + (page != null));
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
