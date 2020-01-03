package com.mobileTicket.hello12306.hook;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Message;
import android.os.Vibrator;
import android.support.annotation.AnyThread;
import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.view.WindowManager;

import com.mobileTicket.hello12306.model.ActionRunnable;
import com.mobileTicket.hello12306.model.BaseDTOEntity;
import com.mobileTicket.hello12306.model.EventCode;
import com.mobileTicket.hello12306.model.OrderConfig;
import com.mobileTicket.hello12306.model.Page;
import com.mobileTicket.hello12306.model.Passenger;
import com.mobileTicket.hello12306.model.RequestResult;
import com.mobileTicket.hello12306.model.SeatType;
import com.mobileTicket.hello12306.util.SecKill;
import com.mobileTicket.hello12306.model.Trains;
import com.mobileTicket.hello12306.rpc.CheckOrderInfo;
import com.mobileTicket.hello12306.rpc.ConfirmPassengerInfoSingle;
import com.mobileTicket.hello12306.rpc.GetWaitTime;
import com.mobileTicket.hello12306.rpc.QueryLeftTicketZ;
import com.mobileTicket.hello12306.rpc.QueryPassenger;
import com.mobileTicket.hello12306.rpc.QueryTicketString;
import com.mobileTicket.hello12306.rpc.RpcRequest;
import com.mobileTicket.hello12306.util.LogUtil;
import com.mobileTicket.hello12306.util.Md5Util;
import com.mobileTicket.hello12306.util.MessageClient;
import com.mobileTicket.hello12306.util.MessageUtil;
import com.mobileTicket.hello12306.util.PageManager;
import com.mobileTicket.hello12306.util.Utils;
import com.mobileTicket.hello12306.widget.XWebView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class Hook12306Impl2 implements IXposedHookLoadPackage {
    private static final String TAG = "Hook12306";
    private ExecutorService executorService = Executors.newCachedThreadPool();
    private ExecutorService ticketService = Executors.newCachedThreadPool();
    private Future timeTickFuture;
    private MessageClient messageClient;
    private Vibrator vibrator;
    private volatile String dfpValue = null;
    private AtomicBoolean getDfpSign = new AtomicBoolean(false);
    // Trains 缓存
    private AtomicBoolean isStarted = new AtomicBoolean(false);
    private AtomicBoolean isProcessing = new AtomicBoolean(false);
    private AtomicInteger fetchCount = new AtomicInteger(0);
    private Method rpcCallMethod;
    private Method getResponseMethod;
    private Timer waitTimer;
    private volatile BaseDTOEntity baseDTOEntity;

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {
        final String packageName = loadPackageParam.packageName;
        if (!"com.MobileTicket".equals(packageName)) {
            return;
        }
        // 首页 MainActivity生命周期
        final Class<?> MainActivity = loadPackageParam.classLoader.loadClass("com.MobileTicket.ui.activity.MainActivity");
        XposedHelpers.findAndHookMethod(MainActivity, "onCreate", Bundle.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                XposedBridge.log("Hook12306 hook MainActivity");
                super.beforeHookedMethod(param);
                PageManager.getInstance().push(new Page((Activity) param.thisObject));
                Activity main = (Activity) param.thisObject;
                baseDTOEntity = new BaseDTOEntity(loadPackageParam.classLoader, main);
                messageClient = new MessageClient(main, new MessageClient.QueryListener() {
                    @Override
                    public void onQuery(final Message msg, final MessageClient.Response response) {
                        switch (msg.what) {
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
                                executorService.execute(new Runnable() {
                                    @Override
                                    public void run() {
                                        Message message = new Message();
                                        Bundle bundle = new Bundle();
                                        bundle.putString("data", rpcWithBaseDTO(new QueryPassenger()));
                                        message.setData(bundle);
                                        response.call(message);
                                    }
                                });
                                break;
                            case EventCode.CODE_SELECT_TRAIN_LIST:
                                executorService.execute(new Runnable() {
                                    @Override
                                    public void run() {
                                        Bundle data = msg.getData();
                                        if (data == null) {
                                            return;
                                        }
                                        Message message = new Message();
                                        Bundle bundle = new Bundle();
                                        bundle.putString("data", rpcWithBaseDTO(
                                                new QueryTicketString(data.getString("trainDate"),
                                                        data.getString("from"), data.getString("to"))));
                                        message.setData(bundle);
                                        response.call(message);
                                    }
                                });
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
                messageClient.getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                startLoop();
            }
        });
        XposedHelpers.findAndHookMethod(MainActivity, "onDestroy", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);
                LogUtil.print("MainActivity onDestroy");
                PageManager.getInstance().pop(new Page((Activity) param.thisObject));
                if (messageClient != null) {
                    messageClient.getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    messageClient.onDestroy();
                }
            }
        });
        // 日志输出
        XposedHelpers.findAndHookMethod("com.alipay.mobile.common.logging.util.LoggingUtil", loadPackageParam.classLoader, "isDebuggable",
                Context.class, new XC_MethodReplacement() {
                    @Override
                    protected Object replaceHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) throws Throwable {
                        return true;
                    }
                });
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
                    Log.i(TAG, stringBuilder.toString());
                }
            });
        }
        // WebViewClient事件
        final Class<?> aPWebView = loadPackageParam.classLoader.loadClass("com.alipay.mobile.nebula.webview.APWebView");
        final Class<?> H5WebViewClient = loadPackageParam.classLoader.loadClass("com.alipay.mobile.nebulacore.web.H5WebViewClient");
        XposedHelpers.findAndHookMethod(H5WebViewClient, "onPageStarted", aPWebView, String.class, Bitmap.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);
                XWebView webView = new XWebView(param.args[0]);
                webView.setWebContentsDebuggingEnabled(true);
            }
        });
        XposedHelpers.findAndHookMethod(H5WebViewClient, "onPageFinished", aPWebView, String.class, long.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);
                XWebView webView = new XWebView(param.args[0]);
                if (TextUtils.isEmpty(dfpValue)) {
                    webView.evaluateJavascript("AlipayJSBridge.call('getDfp',{})", null);
                }
            }
        });

        // 通知前端事件
        final Class<?> H5BridgeImpl = loadPackageParam.classLoader.loadClass("com.alipay.mobile.nebulacore.bridge.H5BridgeImpl");
        final Class<?> H5Event = loadPackageParam.classLoader.loadClass("com.alipay.mobile.h5container.api.H5Event");
        final Class<?> H5BridgeContext = loadPackageParam.classLoader.loadClass("com.alipay.mobile.h5container.api.H5BridgeContext");
        final Method getParamMethod = H5Event.getMethod("getParam");
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
                    case "getDfp":
                        getDfpSign.set(true);
                        break;
                    default:
                        break;
                }
                Log.d(TAG, "action=" + action + ", params=" + reqParams);
            }
        });
        final Class<?> H5BaseBridgeContext = loadPackageParam.classLoader.loadClass("com.alipay.mobile.nebula.basebridge.H5BaseBridgeContext");
        final Class<?> FastJSONObject = loadPackageParam.classLoader.loadClass("com.alibaba.fastjson.JSONObject");
        XposedHelpers.findAndHookMethod(H5BaseBridgeContext, "sendBridgeResult", FastJSONObject, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                String result = String.valueOf(param.args[0]);
                Log.d(TAG, "sendBridgeResult=" + result);
                if (result.contains("\"result\"") && getDfpSign.compareAndSet(true, false)) {
                    JSONObject dpfJson = new JSONObject(result);
                    String dpfResult = dpfJson.optString("result");
                    if (!TextUtils.isEmpty(dpfResult)) {
                        dfpValue = dpfResult;
                        LogUtil.print("getDfpStr=" + dfpValue);
                    }
                }
                super.beforeHookedMethod(param);
            }
        });
        // H5RpcUtil
        final Class<?> H5RpcUtil = XposedHelpers.findClass("com.mpaas.nebula.rpc.H5RpcUtil",
                loadPackageParam.classLoader);
        final Class<?> H5Page = XposedHelpers.findClass("com.alipay.mobile.h5container.api.H5Page",
                loadPackageParam.classLoader);
        final Class<?> H5Response = XposedHelpers.findClass("com.mpaas.nebula.rpc.H5Response",
                loadPackageParam.classLoader);
        getResponseMethod = H5Response.getMethod("getResponse");
        // String operationType, String requestData, String gateway, boolean compress, JSONObject joHeaders, String appKey, boolean retryable, H5Page h5Page, int timeout, String type, boolean isHttpGet, int signType
        XposedHelpers.findAndHookMethod(H5RpcUtil, "rpcCall",
                String.class, String.class, String.class, boolean.class, FastJSONObject, String.class,
                boolean.class, H5Page, int.class, String.class, boolean.class, int.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        super.beforeHookedMethod(param);
                        StringBuilder stringBuilder = new StringBuilder();
                        for (int i = 0; i < param.args.length; i++) {
                            stringBuilder.append(i).append("=>").append(param.args[i]).append("、");
                        }
                        LogUtil.print("rpcCallRequest: " + stringBuilder.toString()
                                + " thread=" + Thread.currentThread().getName());
                    }

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        super.afterHookedMethod(param);
                        if (param.hasThrowable()) {
                            LogUtil.print("rpcCallResponse: " + Log.getStackTraceString(param.getThrowable()));
                        } else {
                            Object result = param.getResult();
                            String response = (String) getResponseMethod.invoke(result);
                            if (response.contains("\"ticketResult\"")) {
                                Log.i(TAG, "rpcCallResponse: " + response);
                                return;
                            }
                            LogUtil.print("rpcCallResponse: " + response
                                    + " thread=" + Thread.currentThread().getName());
                        }
                    }
                });
        rpcCallMethod = H5RpcUtil.getMethod("rpcCall", String.class, String.class, String.class,
                boolean.class, FastJSONObject, String.class,
                boolean.class, H5Page, int.class, String.class, boolean.class, int.class);
    }

    /**
     * 轮询机制
     */
    private synchronized void startLoop() {
        if (timeTickFuture != null) {
            timeTickFuture.cancel(true);
        }
        timeTickFuture = Executors.newSingleThreadExecutor().submit(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
                    if (hour >= 23 || hour < 6) {
                        return;
                    }
                    int sleepTime = 1000 + new Random().nextInt(1500);
                    Log.d(TAG, "count=" + fetchCount.get() + ", sleep=" + sleepTime);
                    try {
                        Thread.sleep(sleepTime);
                        if (isStarted.get()) {
                            queryLeftTicket();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    /**
     * 发起网络请求
     *
     * @param request RpcRequest
     * @param <T>     T
     * @return response
     */
    @WorkerThread
    private <T> T rpcWithBaseDTO(RpcRequest<T> request) {
        try {
            JSONObject requestData = request.requestData();
            JSONObject baseDTO = createBaseDTO();
            if (requestData.has("_requestBody")) {
                requestData.optJSONObject("_requestBody").put("baseDTO", baseDTO);
            } else {
                requestData.put("baseDTO", baseDTO);
            }
            JSONArray jsonArray = new JSONArray();
            jsonArray.put(requestData);
            Object result = rpcCallMethod.invoke(null, request.operationType(),
                    jsonArray.toString(), "", true, null, null, false, null, 0, "", request.isHttpGet(),
                    request.signType());
            String response = (String) getResponseMethod.invoke(result);
            return request.onResponse(response);
        } catch (Exception e) {
            Log.e(TAG, "rpcWithBaseDTO", e);
        }
        return null;
    }

    /**
     * 查询余票
     */
    @AnyThread
    private void queryLeftTicket() {
        if (isProcessing.get()) {
            return;
        }
        ticketService.execute(new Runnable() {
            @Override
            public void run() {
                int length = OrderConfig.INSTANCE.trainDate.size();
                int select = 0;
                int count = fetchCount.getAndIncrement();
                for (int i = length; i > 0; i--) {
                    if (count % i == 0) {
                        select = i - 1;
                        break;
                    }
                }
                String trainDate = OrderConfig.INSTANCE.trainDate.get(select);
                Trains trains = rpcWithBaseDTO(new QueryLeftTicketZ(OrderConfig.INSTANCE.trainDate.get(select),
                        OrderConfig.INSTANCE.stationInfo.first, OrderConfig.INSTANCE.stationInfo.second,
                        showTicketInfo));
                if (trains != null) {
                    if (TextUtils.isEmpty(trains.location_code)) {
                        showPrompt(trains.message);
                        SecKill.getInstance().addTask(trains.message, new Runnable() {
                            @Override
                            public void run() {
                                LogUtil.print("秒杀请求!!");
                                queryLeftTicket();
                            }
                        });
                        return;
                    }
                    if (!isProcessing.compareAndSet(false, true)) {
                        LogUtil.print("wait CheckOrderInfo");
                        return;
                    }
                    trains.setDfpStr(dfpValue);
                    trains.train_date = trainDate;
                    LogUtil.print(trains.toString());
                    trains = rpcWithBaseDTO(new CheckOrderInfo(trains));
                    if (trains != null) {
                        RequestResult result = rpcWithBaseDTO(new ConfirmPassengerInfoSingle(trains));
                        if (result != null) {
                            showPrompt(result.getShowMsg());
                            if (result.isSuccess()) {
                                queryWaitTime(trains, showPromptAction);
                            } else {
                                isProcessing.set(false);
                                if (result.getErrorMsg().contains("目前您还有未处理的订单")) {
                                    ticketSuccess(trains);
                                } else if (result.getErrorMsg().contains("目前排队人数已经超过余票张数")) {
                                    QueryLeftTicketZ.addBlackList(trains.code);
                                } else if (result.getErrorMsg().contains("余票不足,请重新查询车票信息")
                                        && !SecKill.getInstance().isSecKillPeriod()) {
                                    QueryLeftTicketZ.addBlackList(trains.code);
                                }
                            }
                        } else {
                            isProcessing.set(false);
                        }
                    } else {
                        isProcessing.set(false);
                    }
                }
            }
        });
    }

    /**
     * 查询等待时间
     *
     * @param trains   车次
     * @param callback 返回内容回调
     */
    @WorkerThread
    private void queryWaitTime(final Trains trains, final ActionRunnable<String> callback) {
        Pair<Boolean, Integer> waitTimePair = rpcWithBaseDTO(new GetWaitTime(callback));
        if (waitTimePair == null) {
            return;
        }
        if (waitTimePair.first) {
            ticketSuccess(trains);
        } else {
            TimerTask timerTask = new TimerTask() {
                @Override
                public void run() {
                    queryWaitTime(trains, callback);
                }
            };
            if (waitTimePair.second > 0) {
                if (waitTimer != null) {
                    waitTimer.cancel();
                }
                waitTimer = new Timer();
                waitTimer.schedule(timerTask, waitTimePair.second * 1000);
            } else {
                QueryLeftTicketZ.addBlackList(trains.code);
                isProcessing.set(false);
            }
        }
    }

    @SuppressLint("SimpleDateFormat")
    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");

    private JSONObject createBaseDTO() throws JSONException {
        String device_no = baseDTOEntity == null ? "K8X1OCut+DQDADjmL69ilAXv" : baseDTOEntity.getDeviceId();
        String time_str = simpleDateFormat.format(new Date());
        String check_code = Md5Util.encrypt("F" + time_str + device_no).toLowerCase();
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("version_no", baseDTOEntity == null ? "4.1.9" : baseDTOEntity.getProductVersion());
        jsonObject.put("time_str", time_str);
        jsonObject.put("os_type", "a");
        jsonObject.put("user_name", baseDTOEntity == null ? "" : baseDTOEntity.getUserName());
        jsonObject.put("mobile_no", baseDTOEntity == null ? "" : baseDTOEntity.getMobile());
        jsonObject.put("check_code", check_code);
        jsonObject.put("device_no", device_no);
        return jsonObject;
    }

    /**
     * 抢票成功
     */
    @AnyThread
    private void ticketSuccess(@NonNull Trains trains) {
        isStarted.set(false);
        playMusic();
        messageClient.sendToTarget(EventCode.CODE_TICKET_SUCCESS);
        MessageUtil.sendToHi(trains);
    }

    @AnyThread
    private void playMusic() {
        if (messageClient != null) {
            bindService();
            messageClient.sendToTarget(EventCode.CODE_PLAY_MUSIC);
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

    private ActionRunnable<String> showPromptAction = new ActionRunnable<String>() {
        @Override
        public void run(final String string) {
            showPrompt(string);
        }
    };

    /**
     * 显示车票信息
     */
    private ActionRunnable<JSONArray> showTicketInfo = new ActionRunnable<JSONArray>() {
        @Override
        public void run(final JSONArray array) {
            executorService.execute(new Runnable() {
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
                    stringBuilder.append(startTrainDate);
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
    };


    /**
     * 界面上显示提醒
     *
     * @param msg
     */
    private void showPrompt(final String msg) {
        LogUtil.print(msg);
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

    /**
     * 查询配置信息
     */
    private void configQuery() {
        messageClient.sendToTarget(Message.obtain(null, EventCode.CODE_QUERY_TASK), new MessageClient.Callback() {
            @Override
            public void onResponse(Message message) {
                Log.i("AddTaskActivity", "#" + message.arg1);
                if (message.arg1 == 200) {
                    Bundle bundle = message.getData();
                    if (bundle != null) {
                        List<String> seatStringList = Utils.parseToList(bundle.getString("type", null));
                        Set<SeatType> seatTypeList = new HashSet<>();
                        for (String type : seatStringList) {
                            seatTypeList.add(SeatType.getSeatTypeByName(type));
                        }
                        if (seatTypeList.isEmpty()) {
                            seatTypeList.add(SeatType.YW);
                        }
                        String users = bundle.getString("passenger");
                        Log.i("AddTaskActivity", message.arg1 + ", #" + bundle.keySet().size());
                        OrderConfig.INSTANCE = new OrderConfig()
                                .setTrainDate(bundle.getStringArrayList("trainDate"))
                                .setStationInfo(Pair.create(bundle.getString("from"),
                                        bundle.getString("to")))
                                .setTrains(bundle.getStringArrayList("trainList"))
                                .setPassenger(Passenger.parse(users).toArray(new Passenger[0]))
                                .setSeatType(seatTypeList.toArray(new SeatType[0]))
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
