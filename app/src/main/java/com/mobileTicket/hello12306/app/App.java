package com.mobileTicket.hello12306.app;

import android.app.Application;
import android.content.Intent;

import com.mobileTicket.hello12306.service.ShareService;
import com.tencent.mmkv.MMKV;

import org.litepal.LitePal;
import org.litepal.tablemanager.callback.DatabaseListener;

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        MMKV.initialize(this);
        registerDatabaseManager();
        startService(new Intent(this, ShareService.class));
    }

    /**
     * 数据库管理
     */
    private void registerDatabaseManager() {
        LitePal.initialize(this);
        LitePal.registerDatabaseListener(new DatabaseListener() {
            @Override
            public void onCreate() {

            }

            @Override
            public void onUpgrade(int oldVersion, int newVersion) {

            }
        });
    }
}
