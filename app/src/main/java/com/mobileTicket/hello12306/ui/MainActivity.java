package com.mobileTicket.hello12306.ui;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.support.annotation.AnyThread;
import android.support.annotation.NonNull;
import android.support.annotation.UiThread;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.mobileTicket.hello12306.R;
import com.mobileTicket.hello12306.dao.TaskDao;
import com.mobileTicket.hello12306.model.EventCode;
import com.mobileTicket.hello12306.util.MessageClient;
import com.mobileTicket.hello12306.util.Utils;
import com.tencent.mmkv.MMKV;

import org.litepal.LitePal;
import org.litepal.crud.callback.FindMultiCallback;
import org.litepal.crud.callback.SaveCallback;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements MessageClient.QueryListener {

    public static final String TAG = "MainActivity";
    private MessageClient messageClient;
    private MainAdapter adapter;
    private MMKV ticketKV = MMKV.mmkvWithID(EventCode.KEY_TICKET_CACHE, MMKV.MULTI_PROCESS_MODE);
    private ExecutorService executorService = Executors.newCachedThreadPool();
    private RecyclerView recyclerView;
    private int selectedId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d(TAG, "registerReceiver server");
        messageClient = new MessageClient(this, this);
        selectedId = ticketKV.getInt(EventCode.KEY_TASK_SELECTED_ID, -1);
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MainAdapter();
        recyclerView.setAdapter(adapter);
        adapter.update();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.reload:
                startActivityForResult(new Intent(this, AddTaskActivity.class), 0);
                break;
            case R.id.clean:
                adapter.update();
                sendSelectedChange();
                break;
            case R.id.close_music:
                messageClient.sendToTarget(Message.obtain(null, EventCode.CODE_CLOSE_MUSIC), null);
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode  == RESULT_OK) {
            adapter.update();
            sendSelectedChange();
        }
    }

    @Override
    public void onQuery(Message msg, MessageClient.Response response) {
        switch (msg.what) {
            case EventCode.CODE_QUERY_SELECT_TRANS:
                sendSelectedChange();
                break;
            default:
                break;
        }
    }

    /**
     * 通知配置文件有变动
     */
    private void sendSelectedChange() {
        messageClient.sendToTarget(EventCode.CODE_TASK_CHANGE);
    }

    class MainAdapter extends RecyclerView.Adapter<MainViewHolder> {

        private List<TaskDao> taskDaoList = new ArrayList<>();

        @NonNull
        @Override
        public MainViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View root = LayoutInflater.from(parent.getContext()).inflate(R.layout.adapter_main, parent, false);
            return new MainViewHolder(root);
        }

        @SuppressLint("SetTextI18n")
        @Override
        public void onBindViewHolder(@NonNull MainViewHolder holder, int position) {
            final TaskDao taskDao = taskDaoList.get(position);
            holder.title.setText(taskDao.getId() + "、" + taskDao.getFromStationName() + "(" + taskDao.getFromStationCode()
                    + ")" + "=>" + taskDao.getToStationName() + "(" + taskDao.getToStationCode()
                    + ")");
            String stringBuilder = Utils.listToString(taskDao.getTrainDate()) + "\n" +
                    Utils.listToString(taskDao.getTrainList()) + "\n" +
                    Utils.listToString(taskDao.getPassengerList()) + "\n" +
                    "登录账号: " + taskDao.getUid() + '\n';
            holder.ticket.setText(stringBuilder);
            holder.setSelected(taskDao.getId() == selectedId);
            holder.itemView.setOnLongClickListener(new MainOnLongClickListener(taskDao));
        }

        @UiThread
        public void update(@NonNull List<TaskDao> trainsList) {
            this.taskDaoList.clear();
            this.taskDaoList.addAll(trainsList);
            notifyDataSetChanged();
        }

        @AnyThread
        public void update() {
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    LitePal.where("status=?", "0").findAsync(TaskDao.class)
                            .listen(new FindMultiCallback<TaskDao>() {
                                @Override
                                public void onFinish(final List<TaskDao> list) {
                                    recyclerView.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            update(list);
                                        }
                                    });
                                }
                            });
                }
            });
        }

        @Override
        public int getItemCount() {
            return taskDaoList.size();
        }
    }

    class MainOnLongClickListener implements View.OnLongClickListener {

        private TaskDao taskDao;

        public MainOnLongClickListener(TaskDao taskDao) {
            this.taskDao = taskDao;
        }

        @Override
        public boolean onLongClick(View v) {
            new AlertDialog.Builder(MainActivity.this)
                    .setItems(new String[]{"选中", "删除", "编辑"}, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            switch (which) {
                                case 0:
                                    if (selectedId != taskDao.getId()) {
                                        selectedId = taskDao.getId();
                                        ticketKV.putInt(EventCode.KEY_TASK_SELECTED_ID, selectedId);
                                        adapter.notifyDataSetChanged();
                                        sendSelectedChange();
                                    }
                                    break;
                                case 1:
                                    taskDao.setStatus(1);
                                    taskDao.saveOrUpdateAsync("id=?", String.valueOf(taskDao.getId()))
                                            .listen(new SaveCallback() {
                                        @Override
                                        public void onFinish(boolean success) {
                                            sendSelectedChange();
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    adapter.update();
                                                }
                                            });
                                        }
                                    });
                                    break;
                                case 2:
                                    Intent it = new Intent(MainActivity.this, AddTaskActivity.class);
                                    it.putExtra(AddTaskActivity.KEY_TARGET, taskDao.getId());
                                    startActivityForResult(it, 0);
                                    break;
                                default:
                                    break;
                            }
                        }
                    })
                    .show();
            return false;
        }
    }

    class MainViewHolder extends RecyclerView.ViewHolder {

        private TextView title;
        private TextView ticket;
        private View selected;

        public MainViewHolder(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.title);
            ticket = itemView.findViewById(R.id.ticket);
            selected = itemView.findViewById(R.id.selected);
        }

        public void setSelected(boolean selected) {
            this.selected.setVisibility(selected ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "unregisterReceiver server");
        messageClient.onDestroy();
    }
}
