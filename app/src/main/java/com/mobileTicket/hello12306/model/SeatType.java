package com.mobileTicket.hello12306.model;

public enum SeatType {
    YZ("硬座", "1".charAt(0)),
    RZ("软座", "2".charAt(0)),
    YW("硬卧", "3".charAt(0)),
    RW("软卧", "4".charAt(0)),
    GR("高软", "6".charAt(0)),
    EDZ("二等", 'O'),
    YDZ("一等", 'M'),
    TDZ("特等", 'P'),
    WZ("无座", 'W');

    private String name;
    private char sign;

    SeatType(String name, char sign) {
        this.name = name;
        this.sign = sign;
    }

    public String getName() {
        return name;
    }

    public char getSign() {
        return sign;
    }
}
