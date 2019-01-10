package com.mobileTicket.hello12306.model;

public interface EventCode {
    int CODE_QUERY_TICKET = 0x1;
    int CODE_QUERY_SELECT_TRANS = 0x2;
    int CODE_SELECT_TRANS_CHANGE = 0x3;
    int CODE_PLAY_MUSIC = 0x4;
    int CODE_CLOSE_MUSIC = 0x5;
    int CODE_SHOW_REQUEST = 0x6;
    int CODE_SHOW_PROMPT = 0x7;
    // 是否开始开关
    int CODE_SWITCH_CHANGE = 0x8;
    // 选择联系人
    int CODE_SELECT_PASSENGER = 0x9;
    // 界面更新配置信息
    int CODE_TICKET_CONFIG = 0xA;
    // 查询抢票配置信息
    int CODE_QUERY_TASK = 0xB;
    // 抢票配置变动通知
    int CODE_TASK_CHANGE = 0xC;
    // 选择车次
    int CODE_SELECT_TRAIN_LIST = 0xD;

    String KEY_TICKET_CACHE = "KEY_TICKET_CACHE";
    // 选择的任务ID, 默认-1
    String KEY_TASK_SELECTED_ID = "KEY_TASK_SELECTED_ID";
    // 标记是否自动登录
    String KEY_AUTO_LOGIN = "KEY_AUTO_LOGIN";
}
