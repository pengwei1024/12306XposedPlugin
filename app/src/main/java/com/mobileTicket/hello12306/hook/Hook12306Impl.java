package com.mobileTicket.hello12306.hook;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Instrumentation;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Vibrator;
import android.support.annotation.AnyThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;

import com.mobileTicket.hello12306.model.EventCode;
import com.mobileTicket.hello12306.model.OrderConfig;
import com.mobileTicket.hello12306.model.Page;
import com.mobileTicket.hello12306.model.Passenger;
import com.mobileTicket.hello12306.model.SeatType;
import com.mobileTicket.hello12306.model.Trains;
import com.mobileTicket.hello12306.util.MessageClient;
import com.mobileTicket.hello12306.util.MessageUtil;
import com.mobileTicket.hello12306.util.PageManager;
import com.mobileTicket.hello12306.util.Utils;
import com.mobileTicket.hello12306.widget.XWebView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.Random;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class Hook12306Impl implements IXposedHookLoadPackage {
    private static final String TAG = "Hook12306";
    private ExecutorService executorService = Executors.newCachedThreadPool();
    private String newestTicket = null;
    private MessageClient messageClient;
    private Vibrator vibrator;
    private volatile String lastDfpValue = null;
    private AtomicBoolean getDfpSign = new AtomicBoolean(false);
    // Trains 缓存
    private Map<String, Trains> trainsMap = new ConcurrentHashMap<>();
    private AtomicBoolean isStarted = new AtomicBoolean(false);
    private AtomicBoolean isProcessing = new AtomicBoolean(false);
    private AtomicInteger fetchCount = new AtomicInteger(0);
    private volatile MessageClient.Response passengerResponse;
    private volatile MessageClient.Response trainListResponse;
    // location 截取长度
    private static final int LOCATION_CUT_OUT_COUNT = 150;

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {
        final String packageName = loadPackageParam.packageName;
        if (!"com.MobileTicket".equals(packageName)) {
            return;
        }
        // 每分钟整点提醒的广播
        final BroadcastReceiver timeTickReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int min = Calendar.getInstance().get(Calendar.MINUTE);
                Log.i(TAG, "timeTick minute=" + min + ", time=" + System.currentTimeMillis());
                if (min == 0 || min == 30) {
                    queryLeftTicketZ(OrderConfig.INSTANCE.trainDate.get(0),
                            OrderConfig.INSTANCE.stationInfo.first,
                            OrderConfig.INSTANCE.stationInfo.second, null);
                } else if (min == 45 || min == 15) {
                    PageManager.getInstance().runJs("javascript:var menu=document.getElementsByClassName" +
                            "('vmc-menu-item');if(menu.length > 0){menu[0].click()}");
                }
            }
        };
        final Handler mainHandler = new Handler(Looper.getMainLooper());
        // 首页 MainActivity生命周期
        final Class<?> MainActivity = loadPackageParam.classLoader.loadClass("com.MobileTicket.ui.activity.MainActivity");
        XposedHelpers.findAndHookMethod(MainActivity, "onCreate", Bundle.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);
                PageManager.getInstance().push(new Page((Activity) param.thisObject));
                Activity main = (Activity) param.thisObject;
                messageClient = new MessageClient(main, new MessageClient.QueryListener() {
                    @Override
                    public void onQuery(Message msg, MessageClient.Response response) {
                        switch (msg.what) {
                            case EventCode.CODE_QUERY_TICKET:
                                Message msg1 = Message.obtain();
                                Bundle bundle1 = new Bundle();
                                bundle1.putString("data", newestTicket);
                                msg1.setData(bundle1);
                                response.call(msg1);
                                break;
                            case EventCode.CODE_SELECT_TRANS_CHANGE:
                                break;
                            case EventCode.CODE_CLOSE_MUSIC:
                                if (vibrator != null) {
                                    vibrator.cancel();
                                }
                                break;
                            case EventCode.CODE_TASK_CHANGE:
                                isProcessing.compareAndSet(true, false);
                                configQuery();
                                break;
                            case EventCode.CODE_SWITCH_CHANGE:
                                isStarted.set(msg.arg1 == 1);
                                break;
                            case EventCode.CODE_SELECT_PASSENGER:
                                passengerResponse = response;
                                queryPassenger();
                                break;
                            case EventCode.CODE_SELECT_TRAIN_LIST:
                                trainListResponse = response;
                                Bundle bundle = msg.getData();
                                if (bundle != null) {
                                    queryLeftTicketZ(bundle.getString("trainDate"),
                                            bundle.getString("from"), bundle.getString("to"),
                                            "queryTicket4Task");
                                }
                                break;
                            default:
                                break;
                        }
                    }
                });
                bindService();
                messageClient.sendToTarget(Message.obtain(null, EventCode.CODE_QUERY_SELECT_TRANS), null);
                IntentFilter timeFilter = new IntentFilter();
                timeFilter.addAction(Intent.ACTION_TIME_TICK);
                messageClient.getActivity().registerReceiver(timeTickReceiver, timeFilter);
                messageClient.getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }
        });
        XposedHelpers.findAndHookMethod(MainActivity, "onDestroy", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);
                printLog("MainActivity onDestroy");
                PageManager.getInstance().pop(new Page((Activity) param.thisObject));
                if (messageClient != null) {
                    messageClient.getActivity().unregisterReceiver(timeTickReceiver);
                    messageClient.getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    messageClient.onDestroy();
                }
            }
        });
        // H5Activity生命周期
        final Class<?> H5Activity = loadPackageParam.classLoader.loadClass("com.alipay.mobile.nebulacore.ui.H5Activity");
        XposedHelpers.findAndHookMethod(H5Activity, "onCreate", Bundle.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);
                PageManager.getInstance().push(new Page((Activity) param.thisObject));
            }
        });
        XposedHelpers.findAndHookMethod(H5Activity, "finish", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);
                PageManager.getInstance().pop(new Page((Activity) param.thisObject));
            }
        });
        // 定时器
        final Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (OrderConfig.INSTANCE.trains.isEmpty()) {
                    sendUIMessage(EventCode.CODE_TICKET_CONFIG, OrderConfig.INSTANCE.toString());
                    return;
                }
                int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
                if (hour >= 23 || hour < 6) {
                    return;
                }
                int sleepTime = new Random().nextInt(1200);
                Log.d(TAG, "count=" + fetchCount.incrementAndGet() + ", sleep=" + sleepTime);
                try {
                    if (isStarted.get()) {
                        int length = OrderConfig.INSTANCE.trainDate.size();
                        int select = fetchCount.get() % length == 0 ? length - 1 : 0;
                        queryLeftTicketZ(OrderConfig.INSTANCE.trainDate.get(select),
                                OrderConfig.INSTANCE.stationInfo.first,
                                OrderConfig.INSTANCE.stationInfo.second, null);
                    }
                    Thread.sleep(sleepTime);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, 5000, 2000);
        // 日志输出
        XposedBridge.log(TAG + " hook " + packageName);
        final Class<?> logClass = loadPackageParam.classLoader.loadClass("com.alipay.mobile.nebula.util.H5Log");
        final String[] tags = {"d", "w", "e", "debug", "i"};
        for (final String tag : tags) {
            XposedBridge.hookAllMethods(logClass, tag, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    StringBuilder stringBuilder = new StringBuilder();
                    for (Object args : param.args) {
                        if (args == null) {
                            continue;
                        }
                        stringBuilder.append(args.toString()).append("、");
                    }
                    if (param.args.length == 2) {
                        // 检测页面返回
                        if ("H5UrlInterceptPlugin".equals(param.args[0]) && ("H5_TOOLBAR_BACK".equals(param.args[1])
                                || "H5_PAGE_PHYSICAL_BACK".equals(param.args[1]) || "onRelease".equals(param.args[1]))) {
                            mainHandler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    Page page = PageManager.getInstance().getTopPage();
                                    if (page != null && page.getWebView() != null) {
                                        String url = page.getWebView().getUrl();
                                        Log.d(TAG, "goBack=" + url + ", title=" + page.getActivity().getTitle());
                                        page.setList(url != null && url.contains("list.html"));
                                    }
                                }
                            }, 5000);
                        }
                    }
                    XposedBridge.log(TAG + " 【" + tag + "】" + stringBuilder.toString());
                }
            });
        }
        // UCWebView
        final Class<?> UCWebView = loadPackageParam.classLoader.loadClass("com.alipay.mobile.nebulauc.impl.UCWebView");
        XposedHelpers.findAndHookMethod(UCWebView, "goBack", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                XposedBridge.log(TAG + " 【" + "goBack" + "】");
            }
        });
        // WebViewClient事件
        final Class<?> aPWebView = loadPackageParam.classLoader.loadClass("com.alipay.mobile.nebula.webview.APWebView");
        final Class<?> H5WebViewClient = loadPackageParam.classLoader.loadClass("com.alipay.mobile.nebulacore.web.H5WebViewClient");
        XposedHelpers.findAndHookMethod(H5WebViewClient, "onPageStarted", aPWebView, String.class, Bitmap.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);
                XposedBridge.log(TAG + " 【" + "onPageStarted" + "】" + param.args[0]);
                XWebView webView = new XWebView(param.args[0]);
                webView.setWebContentsDebuggingEnabled(true);
            }
        });
        XposedHelpers.findAndHookMethod(H5WebViewClient, "onPageFinished", aPWebView, String.class, long.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);
                if (OrderConfig.INSTANCE.passenger.length == 0) {
                    return;
                }
                Page page = PageManager.getInstance().getTopPage();
                String url = (String) param.args[1];
                if (page != null) {
                    page.pushWebView(new XWebView(param.args[0]));
                    page.setList(url.contains("list.html"));
                }
                XposedBridge.log(TAG + " 【" + "onPageFinished" + "】" + param.args[0]);
                String checkDomExistCallback = "if(!window.checkDomExistCallback){window.checkDomExistCallback=function(cls,callback){var count=0;var checkDomTimer=setInterval(function(){if(++count>30){clearInterval(checkDomTimer);return}var findNode=document.getElementsByClassName(cls);if(findNode.length>0){clearInterval(checkDomTimer);console.log(\"find Node count:\"+count);callback(findNode)}},200)}};";
                if (url.contains("login.html")) {
                    if (OrderConfig.INSTANCE.autoLogin) {
                        String user = OrderConfig.INSTANCE.loginUser;
                        PageManager.getInstance().runJs(checkDomExistCallback + "checkDomExistCallback('loginInput',function(){document.getElementsByClassName('loginInput')[0].value='" + user + "';document.getElementsByClassName('passInput')[0].focus();AlipayJSBridge.call('x_user_pwd',{})});");
                    }
                } else if (url.contains("passenger.html")) {
                    if (OrderConfig.INSTANCE.autoLogin) {
                        StringBuilder userName = new StringBuilder();
                        for (int i = 0; i < OrderConfig.INSTANCE.passenger.length; i++) {
                            userName.append('\'').append(OrderConfig.INSTANCE.passenger[i].getName()).append('\'');
                            if (i < OrderConfig.INSTANCE.passenger.length - 1) {
                                userName.append(',');
                            }
                        }
                        PageManager.getInstance().runJs(checkDomExistCallback + "checkDomExistCallback('order-name',function(){var users=document.getElementsByClassName('order-name');var array=[" + userName + "];for(var i=0;i<users.length;i++){if(array.indexOf(users[i].innerHTML)>=0){if(!users[i].previousElementSibling['checked']){users[i].parentElement.click()}}else{if(users[i].previousElementSibling['checked']){users[i].parentElement.click()}}};AlipayJSBridge.call('x_passenger_click', {})});");
                    }
                } else if (url.contains("order.html")) {
//                    String selectSeat = Config.INSTANCE.seatType.getName();
//                    String orderJs = checkDomExistCallback + "checkDomExistCallback('train-num-time', function(){var train=document.getElementsByClassName('train-num-time')[0].firstChild.innerText;if(" + Utils.toJsArray(Config.INSTANCE.trains) + ".indexOf(train)<0){alert('train Error '+train);return}var train_seats=document.getElementsByClassName(\"train-seats\")[0];var seats=train_seats.getElementsByTagName(\"li\");console.log(seats);for(var i=0;i<seats.length;i++){if(seats[i].innerText.indexOf('" + selectSeat + "')>=0&&seats[i].getAttribute('class').indexOf('seat-selected')>=0){console.log('有票!!!', seats[i]);setTimeout(function(){document.getElementsByClassName(\"confirm-button\")[0].click()},2000);return}}AlipayJSBridge.call('x_order_exit')});";
//                    PageManager.getInstance().runJs(orderJs);
                } else if (url.contains("order-detail-unpay.html")) {
//                    playMusic();
                }
            }
        });

        // 通知前端事件
        final Class<?> H5BridgeImpl = loadPackageParam.classLoader.loadClass("com.alipay.mobile.nebulacore.bridge.H5BridgeImpl");
        final Class<?> H5Event = loadPackageParam.classLoader.loadClass("com.alipay.mobile.h5container.api.H5Event");
        final Class<?> H5BridgeContext = loadPackageParam.classLoader.loadClass("com.alipay.mobile.h5container.api.H5BridgeContext");
        XposedHelpers.findAndHookMethod(H5BridgeImpl, "b", H5Event, H5BridgeContext, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);
                Method getParamMethod = H5Event.getMethod("getParam");
                Method getAction = H5Event.getMethod("getAction");
                final Object params = getParamMethod.invoke(param.args[0]);
                final Object action = getAction.invoke(param.args[0]);
                if (params == null || action == null) {
                    return;
                }
                final JSONObject reqParams = new JSONObject(params.toString());
                switch (String.valueOf(action)) {
                    case "x_passenger_click":
                        Page page1 = PageManager.getInstance().getTopPage();
                        if (page1 != null) {
                            @SuppressLint("ResourceType") final View menuBtn = page1.getActivity().findViewById(1275592741);
                            if (menuBtn != null) {
                                mainHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        menuBtn.performClick();
                                    }
                                });
                                Log.d(TAG, "menuBtn  Found and Click: " + menuBtn);
                            } else {
                                Log.d(TAG, "menuBtn Not Found");
                            }
                        }
                        break;
                    case "x_user_pwd":
                        executorService.execute(new Runnable() {
                            @Override
                            public void run() {
                                if (OrderConfig.INSTANCE.loginPassword != null) {
                                    Instrumentation inst = new Instrumentation();
                                    String pwd = OrderConfig.INSTANCE.loginPassword;
                                    for (int i = 0; i < pwd.length(); i++) {
                                        inst.sendStringSync(String.valueOf(pwd.charAt(i)));
                                    }
                                    mainHandler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            PageManager.getInstance().runJs("document.getElementsByClassName('loginBtn')[0].click()");
                                        }
                                    });
                                }
                            }
                        });
                        break;
                    case "x_order_exit":
                        executorService.execute(new Runnable() {
                            @Override
                            public void run() {
                                Instrumentation inst = new Instrumentation();
                                inst.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);
                            }
                        });
                        break;
                    case "getDfp":
                        getDfpSign.set(true);
                        break;
                    case "submitTrainResult":
                        String success = reqParams.optString("succ_flag");
                        if ("1".equals(success)) {
                            isProcessing.set(true);
                            getWaitTime();
                        } else if ("0".equals(success)) {
                            String error_msg = reqParams.optString("error_msg", "");
                            if (error_msg.contains("目前您还有未处理的订单")) {
                                playMusic();
                                isProcessing.set(true);
                                isStarted.set(false);
                            }
                        }
                        printLog("submitTrainResult: " + reqParams.toString() + ", isProcessing=" + isProcessing.get());
                        showPrompt(reqParams.toString());
                        break;
                    case "checkOrderInfoResult":
                        String location = reqParams.optString("location_code");
                        printLog("checkOrderInfoResult: " + reqParams.toString()
                                + ", isProcessing=" + isProcessing.get() + ", location=" + location);
                        if (location == null || location.length() < LOCATION_CUT_OUT_COUNT) {
                            return;
                        }
                        Trains current = trainsMap.get(location.substring(0, LOCATION_CUT_OUT_COUNT));
                        if (current == null) {
                            showPrompt("current Trains Null");
                            printLog("current Trains Null: " + Utils.listToString(new ArrayList<>(trainsMap.keySet())));
                            return;
                        }
                        if ("1".equals(reqParams.optString("succ_flag"))) {
                            current.location_code = location;
                            if (!isProcessing.get()) {
                                submitTrain(current);
                            } else {
                                printLog("wait Processing 1");
                            }
                        }
                        showPrompt(reqParams.toString());
                        break;
                    case "getWaitTimeResult":
                        printLog("getWaitTimeResult: " + reqParams.toString());
                        showPrompt(reqParams.toString());
                        if ("1".equals(reqParams.optString("succ_flag")) && reqParams.has("waitTime")) {
                            final int waitTime = Utils.parseInt(reqParams.optString("waitTime"));
                            String orderId = reqParams.optString("orderId");
                            if (!TextUtils.isEmpty(orderId)) {
                                playMusic();
                                isStarted.set(false);
                                return;
                            }
                            if (waitTime <= 0) {
                                isProcessing.compareAndSet(true, false);
                                return;
                            }
                            new Timer().schedule(new TimerTask() {
                                @Override
                                public void run() {
                                    getWaitTime();
                                }
                            }, waitTime * 1000);
                        } else {
                            isProcessing.compareAndSet(true, false);
                        }
                        break;
                    case "queryPassengerResult":
                        if (passengerResponse != null) {
                            Message message = new Message();
                            Bundle bundle = new Bundle();
                            bundle.putString("data", reqParams.optString("passengerResult"));
                            message.setData(bundle);
                            passengerResponse.call(message);
                        }
                        break;
                    case "rpcWithBaseDTO":
                        printLog(reqParams.toString());
                        break;
                    case "queryTicket4Task":
                        if (trainListResponse != null) {
                            Message message = new Message();
                            Bundle bundle = new Bundle();
                            bundle.putString("data", reqParams.toString());
                            message.setData(bundle);
                            trainListResponse.call(message);
                            trainListResponse = null;
                        }
                        break;
                    default:
                        break;
                }
                Log.d(TAG, "action=" + action + ", params=" + reqParams);
            }
        });
        XposedHelpers.findAndHookMethod(H5BridgeImpl, "b", H5Event, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);
                Method getParamMethod = H5Event.getMethod("getParam");
                final Object params = getParamMethod.invoke(param.args[0]);
                if (params == null) {
                    return;
                }
                Log.w(TAG, "ticketResult=" + params);
                if (params.toString().contains("ticketResult")) {
                    executorService.execute(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                JSONObject jsonObject = new JSONObject(params.toString());
                                JSONArray array = jsonObject.optJSONArray("ticketResult");
                                if (array != null) {
                                    for (int i = 0; i < array.length(); i++) {
                                        JSONObject item = array.optJSONObject(i);
                                        Log.d("itemTicketInfo", item.toString());
                                        Trains trains = Trains.loads(item);
                                        Log.d("AllTicketInfo", trains.toString());
                                        if (trains.isMatch(OrderConfig.INSTANCE)) {
                                            Log.d("SelectedTicketInfo", trains.toString());
                                            if (!isProcessing.get()) {
                                                if (trains.location_code != null && trains.location_code.length()
                                                        > LOCATION_CUT_OUT_COUNT) {
                                                    trainsMap.put(trains.location_code.substring(0,
                                                            LOCATION_CUT_OUT_COUNT), trains);
                                                }
                                                checkOrderInfo(trains);
                                            } else {
                                                printLog("wait Processing 2");
                                            }
                                            printLog(trains.toString() + ", json=" + item.toString());
                                            break;
                                        }
                                    }
                                    showRequest(array);
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                    newestTicket = params.toString();
                }
            }
        });
        final Class<?> H5PageData = loadPackageParam.classLoader.loadClass("com.alipay.mobile.h5container.api.H5PageData");
        XposedHelpers.findAndHookMethod(H5PageData, "onPageStarted", String.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);
            }
        });
        XposedHelpers.findAndHookMethod(H5PageData, "onPageEnded", String.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);
                String type = (String) param.args[0];
                if (TextUtils.isEmpty(type)) {
                    return;
                }
                switch (type) {
                    case "close":
                        Page page = PageManager.getInstance().getTopPage();
                        if (page != null) {
                            page.popWebView();
                            String url = page.getWebView().getUrl();
                            Log.d("XPage", "onPageEnded url=" + url);
                            page.setList(url != null && url.contains("list.html"));
                        }
                        break;
                    default:
                        break;
                }
            }
        });
        final Class<?> H5BaseBridgeContext = loadPackageParam.classLoader.loadClass("com.alipay.mobile.nebula.basebridge.H5BaseBridgeContext");
        final Class<?> JSONObject = loadPackageParam.classLoader.loadClass("com.alibaba.fastjson.JSONObject");
        XposedHelpers.findAndHookMethod(H5BaseBridgeContext, "sendBridgeResult", JSONObject, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Object jsonObject = param.args[0];
                Log.d(TAG, "jsonObject=" + jsonObject);
                if (getDfpSign.compareAndSet(true, false)) {
                    org.json.JSONObject dpfJson = new JSONObject(jsonObject.toString());
                    String dpfResult = dpfJson.optString("result");
                    if (!TextUtils.isEmpty(dpfResult)) {
                        lastDfpValue = dpfResult;
                        printLog("native getDfp=" + lastDfpValue);
                    }
                }
                if (OrderConfig.INSTANCE.loginUser != null && jsonObject.toString().contains("\\\"userName\\\"") && !jsonObject.toString().contains(OrderConfig.INSTANCE.loginUser)) {
                    Method putMethod = JSONObject.getDeclaredMethod("put", String.class, Object.class);
                    Method getJSONObject = JSONObject.getDeclaredMethod("getJSONObject", String.class);
                    Object data = getJSONObject.invoke(jsonObject, "data");
                    putMethod.invoke(data, "userName", OrderConfig.INSTANCE.loginUser);
                    putMethod.invoke(jsonObject, "data", data);
                    param.args[0] = jsonObject;
                    Log.d(TAG, "login User=" + jsonObject);
                }
                super.beforeHookedMethod(param);
            }
        });
    }

    @AnyThread
    private void playMusic() {
        if (messageClient != null) {
            bindService();
            messageClient.sendToTarget(EventCode.CODE_PLAY_MUSIC);
            MessageUtil.sendToHi("抢到票了快处理!!!!");
            messageClient.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (vibrator != null) {
                        vibrator.cancel();
                    }
                    vibrator = (Vibrator) messageClient.getActivity().getSystemService(Context.VIBRATOR_SERVICE);
                    long[] patter = {500, 3000, 500, 3000};
                    assert vibrator != null;
                    vibrator.vibrate(patter, 0);
                }
            });
        }
    }

    /**
     * 绑定service
     */
    private void bindService() {
        if (messageClient == null) {
            return;
        }
        Context context = messageClient.getActivity();
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("com.mobileTicket.hello12306", "com.mobileTicket.hello12306.service.ShareService"));
        context.startService(intent);
    }

    /**
     * 提交订票信息
     *
     * @param train 车次信息
     * @throws JSONException
     */
    @AnyThread
    private void submitTrain(@NonNull Trains train) throws JSONException {
        if (lastDfpValue == null) {
            return;
        }
        JSONObject jsonObject = new JSONObject();
        JSONObject header = new JSONObject();
        header.put("needLogin", "true");
        jsonObject.put("headers", header);
        jsonObject.put("httpGet", false);
        jsonObject.put("signType", 10);
        jsonObject.put("operationType", "com.cars.otsmobile.confirmPassengerInfoSingle");
        JSONArray requestData = new JSONArray();
        JSONObject jsonArrayItem = new JSONObject();
        JSONObject _requestBody = new JSONObject();
        _requestBody.put("train_no", train.trainNo);
        _requestBody.put("start_time", train.startTime);
        _requestBody.put("station_train_code", train.code);
        _requestBody.put("from_station_telecode", train.from_station_telecode);
        _requestBody.put("to_station_telecode", train.to_station_telecode);
        _requestBody.put("yp_info", train.yp_info);
        _requestBody.put("from_station", train.from_station_telecode);
        _requestBody.put("to_station", train.to_station_telecode);
        _requestBody.put("train_date", train.start_train_date);
        _requestBody.put("seat_type_codes", OrderConfig.INSTANCE.seat_type_codes());
        _requestBody.put("ticket_types", OrderConfig.INSTANCE.ticket_types());
        _requestBody.put("ticket_type_order_num", String.valueOf(OrderConfig.INSTANCE.passenger.length));
        _requestBody.put("passenger_id_types", OrderConfig.INSTANCE.passenger_id_types());
        _requestBody.put("passenger_id_nos", OrderConfig.INSTANCE.passenger_id_nos());
        _requestBody.put("passenger_names", OrderConfig.INSTANCE.passenger_names());
        _requestBody.put("location_code", train.location_code);
        _requestBody.put("choose_seats", "");
        _requestBody.put("p_str", "");
        _requestBody.put("tour_flag", "dc");
        _requestBody.put("purpose_codes", "00");
        _requestBody.put("dynamicProp", "0+0+0+0");
        _requestBody.put("bed_level_order_num", "0");
        _requestBody.put("bed_level", "");
        _requestBody.put("mobile_nos", OrderConfig.INSTANCE.mobile_nos());
        _requestBody.put("save_passenger_flag", OrderConfig.INSTANCE.save_passenger_flag());
        _requestBody.put("passenger_flag", "1");
        _requestBody.put("dfpStr", lastDfpValue);
        jsonArrayItem.put("_requestBody", _requestBody);
        requestData.put(jsonArrayItem);
        jsonObject.put("requestData", requestData);
        String ret = jsonObject.toString().replaceAll("\\\\/", "/");
        Log.d(TAG, "AlipayJSBridge.call('rpcWithBaseDTO',"
                + ret + ", function(res){AlipayJSBridge.call('submitTrainResult',res)})");
        PageManager.getInstance().runJs("AlipayJSBridge.call('rpcWithBaseDTO',"
                + ret + ", function(res){AlipayJSBridge.call('submitTrainResult',res)})");
    }

    /**
     * 检查订单，交换location_code
     *
     * @param trains 车次信息
     * @throws JSONException JSONException
     */
    private void checkOrderInfo(@NonNull Trains trains) throws JSONException {
        if (TextUtils.isEmpty(trains.location_code)) {
            printLog("checkOrderInfo: location_code Empty, message=" + trains.message);
            showPrompt("checkOrderInfo message=" + trains.message);
            return;
        }
        JSONObject jsonObject = new JSONObject();
        JSONObject header = new JSONObject();
        header.put("needLogin", "true");
        jsonObject.put("headers", header);
        jsonObject.put("httpGet", false);
        jsonObject.put("signType", -1);
        jsonObject.put("operationType", "com.cars.otsmobile.checkOrderInfo");
        JSONArray requestData = new JSONArray();
        JSONObject jsonArrayItem = new JSONObject();
        JSONObject _requestBody = new JSONObject();
        _requestBody.put("tour_flag", "dc");
        _requestBody.put("secret_str", trains.location_code);
        jsonArrayItem.put("_requestBody", _requestBody);
        requestData.put(jsonArrayItem);
        jsonObject.put("requestData", requestData);
        String ret = jsonObject.toString().replaceAll("\\\\/", "/");
        PageManager.getInstance().runJs("AlipayJSBridge.call('rpcWithBaseDTO',"
                + ret + ", function(res){AlipayJSBridge.call('checkOrderInfoResult',res)})");
    }

    /**
     * 检查余票信息
     *
     * @param trainDate 开车日期
     * @param from      起始车站
     * @param to        结束车站
     */
    private void queryLeftTicketZ(@Nullable String trainDate, @Nullable String from,
                                  @Nullable String to, @Nullable String callbackName) {
        if (trainDate == null || from == null || to == null) {
            return;
        }
        if (OrderConfig.INSTANCE.trainDate.isEmpty()) {
            return;
        }
        try {
            JSONObject jsonObject = new JSONObject();
            JSONObject header = new JSONObject();
            jsonObject.put("headers", header);
            jsonObject.put("httpGet", true);
            jsonObject.put("signType", -1);
            jsonObject.put("operationType", "com.cars.otsmobile.queryLeftTicketZ");
            JSONArray requestData = new JSONArray();
            JSONObject jsonArrayItem = new JSONObject();
            jsonArrayItem.put("train_date", trainDate);
            jsonArrayItem.put("from_station", from);
            jsonArrayItem.put("to_station", to);
            jsonArrayItem.put("station_train_code", "");
            jsonArrayItem.put("train_headers", "QB#");
            jsonArrayItem.put("train_flag", "");
            jsonArrayItem.put("start_time_begin", "0000");
            jsonArrayItem.put("start_time_end", "2400");
            jsonArrayItem.put("seat_type", "0");
            jsonArrayItem.put("ticket_num", "");
            jsonArrayItem.put("seatBack_Type", "");
            jsonArrayItem.put("dfpStr", "");
            jsonArrayItem.put("purpose_codes", "00");
            JSONArray cacheDataKeys = new JSONArray();
            cacheDataKeys.put("train_date");
            cacheDataKeys.put("purpose_codes");
            cacheDataKeys.put("from_station");
            cacheDataKeys.put("to_station");
            cacheDataKeys.put("station_train_code");
            cacheDataKeys.put("start_time_begin");
            cacheDataKeys.put("start_time_end");
            cacheDataKeys.put("train_headers");
            cacheDataKeys.put("train_flag");
            cacheDataKeys.put("seat_type");
            cacheDataKeys.put("seatBack_Type");
            cacheDataKeys.put("ticket_num");
            cacheDataKeys.put("dfpStr");
            jsonObject.put("cacheDataKeys", cacheDataKeys);
            requestData.put(jsonArrayItem);
            jsonObject.put("requestData", requestData);
            String ret = jsonObject.toString().replaceAll("\\\\/", "/");
            if (TextUtils.isEmpty(callbackName)) {
                PageManager.getInstance().runJs("AlipayJSBridge.call('rpcWithBaseDTO',"
                        + ret + ")");
            } else {
                PageManager.getInstance().runJs("AlipayJSBridge.call('rpcWithBaseDTO',"
                        + ret + ", function(res){AlipayJSBridge.call('" + callbackName + "',res)})");
            }
        } catch (Exception e) {
            Log.e(TAG, "queryLeftTicketZ", e);
        }
    }

    /**
     * 获取等待时间
     *
     * @throws JSONException
     */
    private void getWaitTime() {
        try {
            JSONObject jsonObject = new JSONObject();
            JSONObject header = new JSONObject();
            header.put("needLogin", "true");
            jsonObject.put("headers", header);
            jsonObject.put("httpGet", false);
            jsonObject.put("signType", -1);
            jsonObject.put("operationType", "com.cars.otsmobile.getWaitTime");
            JSONArray requestData = new JSONArray();
            JSONObject jsonArrayItem = new JSONObject();
            JSONObject _requestBody = new JSONObject();
            _requestBody.put("tourFlag", "dc");
            jsonArrayItem.put("_requestBody", _requestBody);
            requestData.put(jsonArrayItem);
            jsonObject.put("requestData", requestData);
            String ret = jsonObject.toString().replaceAll("\\\\/", "/");
            PageManager.getInstance().runJs("AlipayJSBridge.call('rpcWithBaseDTO',"
                    + ret + ", function(res){AlipayJSBridge.call('getWaitTimeResult',res)})");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    /**
     * 获取联系人
     *
     * @throws JSONException
     */
    private void queryPassenger() {
        try {
            JSONObject jsonObject = new JSONObject();
            JSONObject header = new JSONObject();
            header.put("needLogin", "true");
            jsonObject.put("headers", header);
            jsonObject.put("httpGet", false);
            jsonObject.put("signType", -1);
            jsonObject.put("operationType", "com.cars.otsmobile.queryPassenger");
            JSONArray requestData = new JSONArray();
            JSONObject jsonArrayItem = new JSONObject();
            JSONObject _requestBody = new JSONObject();
            jsonArrayItem.put("_requestBody", _requestBody);
            requestData.put(jsonArrayItem);
            jsonObject.put("requestData", requestData);
            String ret = jsonObject.toString().replaceAll("\\\\/", "/");
            PageManager.getInstance().runJs("AlipayJSBridge.call('rpcWithBaseDTO',"
                    + ret + ", function(res){AlipayJSBridge.call('queryPassengerResult',res)})");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private PrintStream printStream = null;
    @SuppressLint("SimpleDateFormat")
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MM-dd HH:mm:ss:SSS");
    private TimeZone gmt = TimeZone.getTimeZone("Asia/Shanghai");

    /**
     * 打印日志到文件
     *
     * @param log 日志内容
     */
    @SuppressLint("SdCardPath")
    private synchronized void printLog(final String log) {
        if (log == null) {
            return;
        }
        Log.d(TAG, "printLog:" + log);
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                if (printStream == null || printStream.checkError()) {
                    try {
                        printStream = new PrintStream(new File("/sdcard/12306Hook.txt"));
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }
                dateFormat.setTimeZone(gmt);
                String time = dateFormat.format(new Date());
                if (printStream != null) {
                    printStream.println(String.format("[%s][%s] ", time,
                            Thread.currentThread().getName()) + log);
                }
            }
        });
    }

    /**
     * 显示车票信息
     *
     * @param array JSONArray
     */
    private void showRequest(final @NonNull JSONArray array) {
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                StringBuilder stringBuilder = new StringBuilder();
                Calendar calendar = Calendar.getInstance();
                stringBuilder.append(calendar.get(Calendar.MINUTE)).append(":")
                        .append(calendar.get(Calendar.SECOND)).append("\n");
                String startTrainDate = null;
                for (int i = 0; i < array.length(); i++) {
                    JSONObject item = array.optJSONObject(i);
                    Trains trains = Trains.loads(item);
                    if (OrderConfig.INSTANCE.trains.contains(trains.code)) {
                        if (startTrainDate == null) {
                            startTrainDate = trains.start_train_date;
                        }
                        stringBuilder.append(trains.code).append(trains.getTicketNumString()).append("\n");
                    }
                }
                stringBuilder.append(startTrainDate).append("\n");
                if (messageClient != null) {
                    Message message = Message.obtain(null, EventCode.CODE_SHOW_REQUEST);
                    Bundle bundle = new Bundle();
                    bundle.putString("data", stringBuilder.toString());
                    message.setData(bundle);
                    messageClient.sendToTarget(message, null);
                }
            }
        });
    }

    /**
     * 界面上显示提醒
     *
     * @param msg
     */
    private void showPrompt(final String msg) {
        sendUIMessage(EventCode.CODE_SHOW_PROMPT, msg);
    }

    private void sendUIMessage(final int eventCode, final String msg) {
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                if (messageClient != null) {
                    Message message = Message.obtain(null, eventCode);
                    Bundle bundle = new Bundle();
                    bundle.putString("data", msg);
                    message.setData(bundle);
                    messageClient.sendToTarget(message, null);
                }
            }
        });
    }

    private void configQuery() {
        messageClient.sendToTarget(Message.obtain(null, EventCode.CODE_QUERY_TASK), new MessageClient.Callback() {
            @Override
            public void onResponse(Message message) {
                Log.i("AddTaskActivity", "#" + message.arg1);
                if (message.arg1 == 200) {
                    Bundle bundle = message.getData();
                    if (bundle != null) {
                        String users = bundle.getString("passenger");
                        Log.i("AddTaskActivity", message.arg1 + ", #" + bundle.keySet().size());
                        OrderConfig.INSTANCE = new OrderConfig()
                                .setTrainDate(bundle.getStringArrayList("trainDate"))
                                .setStationInfo(Pair.create(bundle.getString("from"),
                                        bundle.getString("to")))
                                .setTrains(bundle.getStringArrayList("trainList"))
                                .setPassenger(Passenger.parse(users).toArray(new Passenger[0]))
                                .setSeatType(SeatType.parse(bundle.getString("type")))
                                .setLoginUser(bundle.getString("uid"))
                                .setLoginPassword(bundle.getString("pwd"))
                                .setAutoLogin(bundle.getBoolean("autoLogin", true));
                        sendUIMessage(EventCode.CODE_TICKET_CONFIG, OrderConfig.INSTANCE.toString());
                    }
                } else if (message.arg1 == -1) {
                    sendUIMessage(EventCode.CODE_TICKET_CONFIG, "不存在配置文件");
                } else if (message.arg1 == -2) {
                    sendUIMessage(EventCode.CODE_TICKET_CONFIG, "配置文件被删除");
                } else {
                    sendUIMessage(EventCode.CODE_TICKET_CONFIG, "未知异常:" + message.arg1 + ", "
                    + OrderConfig.INSTANCE.toString());
                }
            }
        });
    }
}
