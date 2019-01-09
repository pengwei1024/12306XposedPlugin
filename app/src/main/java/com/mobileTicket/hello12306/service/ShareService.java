package com.mobileTicket.hello12306.service;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.mobileTicket.hello12306.R;
import com.mobileTicket.hello12306.dao.TaskDao;
import com.mobileTicket.hello12306.model.EventCode;
import com.mobileTicket.hello12306.util.MessageClient;
import com.tencent.mmkv.MMKV;

import org.litepal.LitePal;
import org.litepal.crud.callback.FindCallback;

import java.util.ArrayList;

public class ShareService extends Service implements MessageClient.QueryListener {
    private static final String TAG = "ShareService";
    private MessageClient client;
    private MediaPlayer mMediaPlayer;
    //布局参数
    private WindowManager.LayoutParams params;
    //实例化的WindowManager
    private WindowManager windowManager;
    //状态栏高度
    int statusBarHeight = -1;
    private View content;
    private TextView requestText;
    private TextView tipText;
    private boolean isStarted = true;
    private MMKV ticketKV = MMKV.mmkvWithID(EventCode.KEY_TICKET_CACHE, MMKV.MULTI_PROCESS_MODE);

    @Override
    public void onCreate() {
        super.onCreate();
        client = new MessageClient(this, this);
        createPopWindow();
        client.sendToTarget(EventCode.CODE_TASK_CHANGE);
    }

    public ShareService() {

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand " + hashCode());
        notifySwitch();
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onQuery(Message msg, final MessageClient.Response response) {
        switch (msg.what) {
            case EventCode.CODE_PLAY_MUSIC:
                if (mMediaPlayer == null) {
                    mMediaPlayer = MediaPlayer.create(this, R.raw.dudu);
                    mMediaPlayer.setLooping(true);
                    mMediaPlayer.start();
                }
                break;
            case EventCode.CODE_CLOSE_MUSIC:
                if (mMediaPlayer != null) {
                    mMediaPlayer.reset();
                    mMediaPlayer = null;
                }
                break;
            case EventCode.CODE_SHOW_REQUEST:
                if (requestText != null && msg.getData() != null) {
                    String data = msg.getData().getString("data");
                    requestText.setText(String.valueOf(data));
                }
                break;
            case EventCode.CODE_SHOW_PROMPT:
                if (tipText != null && msg.getData() != null) {
                    String data = msg.getData().getString("data");
                    tipText.setText(String.valueOf(data));
                }
                break;
            case EventCode.CODE_QUERY_TASK:
                int selectedId = ticketKV.getInt(EventCode.KEY_TASK_SELECTED_ID, -1);
                if (selectedId < 0) {
                    response.call(Message.obtain(null, 0, -1, 0));
                    return;
                }
                LitePal.where("id=? and status=?", String.valueOf(selectedId), "0")
                        .findFirstAsync(TaskDao.class)
                        .listen(new FindCallback<TaskDao>() {
                            @Override
                            public void onFinish(TaskDao taskDao) {
                                if (taskDao == null) {
                                    response.call(Message.obtain(null, 0, -2, 0));
                                } else {
                                    Message message = new Message();
                                    message.arg1 = 200;
                                    Bundle bundle = new Bundle();
                                    bundle.putString("from", taskDao.getFromStationCode());
                                    bundle.putString("to", taskDao.getToStationCode());
                                    bundle.putStringArrayList("trainDate", new ArrayList<>(taskDao.getTrainDate()));
                                    bundle.putStringArrayList("trainList", new ArrayList<>(taskDao.getTrainList()));
                                    bundle.putString("passenger", taskDao.getPassengerJson());
                                    bundle.putString("uid", taskDao.getUid());
                                    bundle.putString("pwd", taskDao.getPwd());
                                    message.setData(bundle);
                                    response.call(message);
                                }
                            }
                        });
                break;
            default:
                break;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        client.onDestroy();
        if (content != null) {
            windowManager.removeView(content);
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void createPopWindow() {
        params = new WindowManager.LayoutParams();
        windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        params.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        params.format = PixelFormat.RGBA_8888;
        params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        // 设置窗口初始停靠位置.
        params.gravity = Gravity.LEFT | Gravity.TOP;
        params.x = 0;
        params.y = 0;
        params.width = 600;
        params.height = ActionBar.LayoutParams.WRAP_CONTENT;

        LayoutInflater inflater = LayoutInflater.from(this);
        // 获取浮动窗口视图所在布局.
        content = inflater.inflate(R.layout.service_prompt, null);
        requestText = content.findViewById(R.id.requestText);
        tipText = content.findViewById(R.id.tipText);
        CheckBox startCheckbox = content.findViewById(R.id.startCheckbox);
        startCheckbox.setChecked(isStarted);
        windowManager.addView(content, params);
        // 主动计算出当前View的宽高信息.
        content.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        // 用于检测状态栏高度.
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            statusBarHeight = getResources().getDimensionPixelSize(resourceId);
        }
        content.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                params.x = (int) event.getRawX() - 150;
                params.y = (int) event.getRawY() - 150 - statusBarHeight;
                windowManager.updateViewLayout(content, params);
                return false;
            }
        });
        startCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                isStarted = isChecked;
                notifySwitch();
            }
        });
    }

    private void notifySwitch() {
        client.sendToTarget(EventCode.CODE_SWITCH_CHANGE, isStarted ? 1 : 0);
    }
}
