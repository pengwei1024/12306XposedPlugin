package com.mobileTicket.hello12306.ui;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatAutoCompleteTextView;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;


import com.mobileTicket.hello12306.R;
import com.mobileTicket.hello12306.dao.TaskDao;
import com.mobileTicket.hello12306.model.EventCode;
import com.mobileTicket.hello12306.model.Passenger;
import com.mobileTicket.hello12306.model.SeatType;
import com.mobileTicket.hello12306.model.Trains;
import com.mobileTicket.hello12306.util.MessageClient;
import com.mobileTicket.hello12306.util.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.litepal.LitePal;
import org.litepal.crud.callback.FindCallback;
import org.litepal.crud.callback.SaveCallback;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AddTaskActivity extends AppCompatActivity implements MessageClient.QueryListener {

    public static final String KEY_TARGET = "KEY_TARGET";
    private AppCompatAutoCompleteTextView startStation;
    private AppCompatAutoCompleteTextView endStation;
    private static final String TAG = "AddTaskActivity";
    private JSONObject stationCache;
    private ArrayAdapter arrayAdapter;
    private ExecutorService executorService = Executors.newCachedThreadPool();
    private EditText selectTimeText;
    private EditText selectUserText;
    private MessageClient messageClient;
    private HashMap<String, JSONObject> passengerMap;
    private List<String> passengerSelected;
    private EditText trainListText;
    private EditText uidText;
    private EditText pwdText;
    private int targetId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        targetId = getIntent().getIntExtra(KEY_TARGET, -1);
        messageClient = new MessageClient(this, this);
        setContentView(R.layout.activity_add_task);
        startStation = findViewById(R.id.start_station);
        endStation = findViewById(R.id.end_station);
        showStationPrompt();
        Button selectTime = findViewById(R.id.select_time);
        selectTimeText = findViewById(R.id.select_time_text);
        selectUserText = findViewById(R.id.select_user_text);
        trainListText = findViewById(R.id.train_list);
        uidText = findViewById(R.id.uid);
        pwdText = findViewById(R.id.pwd);
        selectTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Calendar calendar = Calendar.getInstance();
                new DatePickerDialog(AddTaskActivity.this,
                        new DatePickerDialog.OnDateSetListener() {
                            @Override
                            public void onDateSet(DatePicker view, int year,
                                                  int monthOfYear, int dayOfMonth) {
                                String text = String.format(Locale.CHINA, "%d%s%s", year,
                                        doubleChar(monthOfYear + 1), doubleChar(dayOfMonth));
                                if (selectTimeText.getText().length() == 0) {
                                    selectTimeText.setText(text);
                                } else {
                                    selectTimeText.append(";");
                                    selectTimeText.append(text);
                                }
                            }
                        }
                        , calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
            }
        });
        Button selectUser = findViewById(R.id.select_user_btn);
        selectUser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showProgress("正在请求联系人...");
                final Timer timer = new Timer();
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                hideProgress();
                                Toast.makeText(AddTaskActivity.this, "请先启动12306客户端", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }, 5000);
                messageClient.sendToTarget(EventCode.CODE_SELECT_PASSENGER, null, new MessageClient.Callback() {
                    @Override
                    public void onResponse(Message message) {
                        timer.cancel();
                        hideProgress();
                        String data = message.getData().getString("data");
                        Log.i(TAG, "passenger=" + data);
                        try {
                            JSONArray jsonArray = new JSONArray(data);
                            passengerMap = new HashMap<>();
                            passengerSelected = new ArrayList<>();
                            for (int i = 0; i < jsonArray.length(); i++) {
                                JSONObject jsonObject = jsonArray.getJSONObject(i);
                                passengerMap.put(jsonObject.optString("user_name"), jsonObject);
                            }
                            final List<String> passengerList = new ArrayList<>(passengerMap.keySet());
                            boolean[] booleans = new boolean[passengerList.size()];
                            Arrays.fill(booleans, false);
                            new AlertDialog.Builder(AddTaskActivity.this)
                                    .setTitle("请选择用户")
                                    .setMultiChoiceItems(passengerList.toArray(new String[0]),
                                            booleans, new DialogInterface.OnMultiChoiceClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                                                    if (isChecked) {
                                                        passengerSelected.add(passengerList.get(which));
                                                    } else {
                                                        passengerSelected.remove(passengerList.get(which));
                                                    }
                                                }
                                            })
                                    .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            if (passengerSelected != null) {
                                                selectUserText.setText(null);
                                                for (int i = 0; i < passengerSelected.size(); i++) {
                                                    selectUserText.append(passengerSelected.get(i));
                                                    if (i < passengerSelected.size() - 1) {
                                                        selectUserText.append(";");
                                                    }
                                                }
                                            }
                                        }
                                    })
                                    .show();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        });
        // 选择车次
        findViewById(R.id.train_list_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!checkNotNull(startStation, "起始站不能为空")
                        || !checkNotNull(endStation, "终点站不能为空")
                        || !checkNotNull(selectTimeText, "发车时间不能为空")
                        ) {
                    return;
                }
                String startStationCode = stationCache.optString(startStation.getText().toString());
                String endStationCode = stationCache.optString(endStation.getText().toString());
                if (TextUtils.isEmpty(startStationCode) || TextUtils.isEmpty(endStationCode)) {
                    Toast.makeText(AddTaskActivity.this, "站点信息异常", Toast.LENGTH_SHORT).show();
                    return;
                }
                String[] trainDateArray = selectTimeText.getText().toString().split("[；|;]");
                Pattern pattern = Pattern.compile("^\\d{8}$");
                for (String date : trainDateArray) {
                    Matcher matcher = pattern.matcher(date);
                    if (!matcher.find()) {
                        Toast.makeText(AddTaskActivity.this, "日期异常", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
                showProgress("正在请求车次信息...");
                final Timer timer = new Timer();
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                hideProgress();
                                Toast.makeText(AddTaskActivity.this, "请先启动12306客户端", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }, 5000);
                Message message = Message.obtain(null, EventCode.CODE_SELECT_TRAIN_LIST);
                Bundle bundle = new Bundle();
                bundle.putString("trainDate", trainDateArray[0]);
                bundle.putString("from", startStationCode);
                bundle.putString("to", endStationCode);
                message.setData(bundle);
                messageClient.sendToTarget(message,
                        new MessageClient.Callback() {
                    @Override
                    public void onResponse(Message message) {
                        timer.cancel();
                        hideProgress();
                        String data = message.getData().getString("data");
                        try {
                            final List<String> trainList = new ArrayList<>();
                            List<String> trainShowList = new ArrayList<>();
                            JSONObject jsonObject = new JSONObject(data);
                            JSONArray array = jsonObject.optJSONArray("ticketResult");
                            for (int i=0;i<array.length();i++) {
                                JSONObject item = array.optJSONObject(i);
                                Trains trains = Trains.loads(item);
                                trainList.add(trains.code);
                                trainShowList.add(trains.code + " (" + trains.startTime + ")");
                            }
                            boolean[] booleans = new boolean[trainShowList.size()];
                            Arrays.fill(booleans, false);
                            final Set<Integer> selectedSet = new HashSet<>();
                            new AlertDialog.Builder(AddTaskActivity.this)
                                    .setTitle("请选择车次")
                                    .setMultiChoiceItems(trainShowList.toArray(new String[0]),
                                            booleans, new DialogInterface.OnMultiChoiceClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                                                    if (isChecked) {
                                                        selectedSet.add(which);
                                                    } else {
                                                        selectedSet.remove(which);
                                                    }
                                                }
                                            })
                                    .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            List<String> selectedList = new ArrayList<>();
                                            for (int i : selectedSet) {
                                                selectedList.add(trainList.get(i));
                                            }
                                            trainListText.setText(Utils.listToString(selectedList));
                                        }
                                    })
                                    .show();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        });
        // 提交
        findViewById(R.id.btn_submit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (stationCache == null) {
                    return;
                }
                TaskDao taskDao = new TaskDao();
                if (!checkNotNull(startStation, "起始站不能为空")
                        || !checkNotNull(endStation, "终点站不能为空")
                        || !checkNotNull(selectTimeText, "发车时间不能为空")
                        || !checkNotNull(selectUserText, "用户信息不能为空")
                        || !checkNotNull(selectTimeText, "日期不能为空")
                        || !checkNotNull(trainListText, "车次不能为空")
                        ) {
                    return;
                }
                String startStationCode = stationCache.optString(startStation.getText().toString());
                String endStationCode = stationCache.optString(endStation.getText().toString());
                if (TextUtils.isEmpty(startStationCode) || TextUtils.isEmpty(endStationCode)) {
                    Toast.makeText(AddTaskActivity.this, "站点信息异常", Toast.LENGTH_SHORT).show();
                    return;
                }
                taskDao.setFromStationCode(startStationCode);
                taskDao.setToStationCode(endStationCode);
                taskDao.setFromStationName(startStation.getText().toString());
                taskDao.setToStationName(endStation.getText().toString());
                String[] trainDateArray = selectTimeText.getText().toString().split("[；|;]");
                Pattern pattern = Pattern.compile("^\\d{8}$");
                for (String date : trainDateArray) {
                    Matcher matcher = pattern.matcher(date);
                    if (!matcher.find()) {
                        Toast.makeText(AddTaskActivity.this, "日期异常", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
                taskDao.setTrainDate(Arrays.asList(trainDateArray));
                String[] trainArray = trainListText.getText().toString().split("[；|;]");
                taskDao.setTrainList(Arrays.asList(trainArray));
                String[] userArray = selectUserText.getText().toString().split("[；|;]");
                List<Passenger> users = new ArrayList<>();
                for (String name : userArray) {
                    JSONObject jsonObject = passengerMap.get(name);
                    if (jsonObject != null) {
                        users.add(new Passenger(jsonObject.optString("user_name"),
                                jsonObject.optString("id_no"), jsonObject.optString("mobile_no")));
                    }
                }
                if (users.isEmpty()) {
                    Toast.makeText(AddTaskActivity.this, "用户信息不是从12306选择", Toast.LENGTH_SHORT).show();
                    return;
                }
                taskDao.setPassengerList(users);
                taskDao.setType(String.valueOf(SeatType.YW.getSign()));
                taskDao.setUid(uidText.getText().toString());
                taskDao.setPwd(pwdText.getText().toString());
                SaveCallback callback = new SaveCallback() {
                    @Override
                    public void onFinish(final boolean success) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(AddTaskActivity.this, "保存完成:" + success,
                                        Toast.LENGTH_SHORT).show();
                                setResult(RESULT_OK);
                                finish();
                            }
                        });
                    }
                };
                if (targetId > 0) {
                    taskDao.saveOrUpdateAsync("id=?", String.valueOf(targetId)).listen(callback);
                } else {
                    taskDao.saveAsync().listen(callback);
                }
            }
        });
        restoreData();
    }

    /**
     * 恢复数据
     */
    private void restoreData() {
        if (targetId > 0) {
            LitePal.where("id=?", String.valueOf(targetId))
                    .findFirstAsync(TaskDao.class).listen(new FindCallback<TaskDao>() {
                @Override
                public void onFinish(final TaskDao taskDao) {
                    if (taskDao != null) {
                        passengerMap = new HashMap<>();
                        List<Passenger> users = taskDao.getPassengerList();
                        final List<String> userNameList = new ArrayList<>();
                        for (Passenger user : users) {
                            JSONObject jsonObject = new JSONObject();
                            try {
                                jsonObject.put("user_name", user.getName());
                                jsonObject.put("id_no", user.getId());
                                jsonObject.put("mobile_no", user.getPhone());
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            userNameList.add(user.getName());
                            passengerMap.put(user.getName(), jsonObject);
                        }
                        startStation.setText(taskDao.getFromStationName());
                        endStation.setText(taskDao.getToStationName());
                        selectTimeText.setText(Utils.listToString(taskDao.getTrainDate()));
                        trainListText.setText(Utils.listToString(taskDao.getTrainList()));
                        uidText.setText(taskDao.getUid());
                        pwdText.setText(taskDao.getPwd());
                        selectUserText.setText(Utils.listToString(userNameList));
                    }
                }
            });
        }
    }

    private boolean checkNotNull(@Nullable TextView textView, @NonNull String toastTextIfNull) {
        if (textView == null) {
            return false;
        }
        String text = textView.getText().toString().trim();
        if (TextUtils.isEmpty(text)) {
            Toast.makeText(this, toastTextIfNull, Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private ProgressDialog progressDialog;

    private void showProgress(String msg) {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(this);
        }
        progressDialog.setMessage(msg);
        progressDialog.show();
    }

    private void hideProgress() {
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
    }

    private void showStationPrompt() {
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    InputStream is = getAssets().open("station.json");
                    InputStreamReader reader = new InputStreamReader(is);
                    BufferedReader bufferedReader = new BufferedReader(reader);
                    StringBuilder buffer = new StringBuilder();
                    String str;
                    try {
                        while ((str = bufferedReader.readLine()) != null) {
                            buffer.append(str);
                        }
                        Log.d(TAG, buffer.toString());
                        stationCache = new JSONObject(buffer.toString());
                        Iterator<String> keys = stationCache.keys();
                        arrayAdapter = new ArrayAdapter<>(AddTaskActivity.this, android.R.layout.simple_dropdown_item_1line,
                                Utils.copyIterator(keys));
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                startStation.setAdapter(arrayAdapter);
                                endStation.setAdapter(arrayAdapter);
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private String doubleChar(int num) {
        if (num < 10) {
            return "0" + num;
        }
        return String.valueOf(num);
    }

    @Override
    public void onQuery(Message msg, MessageClient.Response response) {

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        messageClient.onDestroy();
    }
}
