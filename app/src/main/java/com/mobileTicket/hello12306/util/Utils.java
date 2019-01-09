package com.mobileTicket.hello12306.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Utils {
    /**
     * 转换java数组为js数组
     *
     * @param array String[]{"a", "b"}
     * @return ['a','b']
     */
    public static String toJsArray(String[] array) {
        StringBuilder builder = new StringBuilder("[");
        for (int i = 0; i < array.length; i++) {
            builder.append('\'').append(array[i]).append('\'');
            if (i < array.length - 1) {
                builder.append(',');
            }
        }
        builder.append("]");
        return builder.toString();
    }

    public static String toJsArray(List<String> list) {
        return toJsArray(list.toArray(new String[0]));
    }

    public static int parseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static <T> List<T> copyIterator(Iterator<T> iter) {
        List<T> copy = new ArrayList<>();
        while (iter.hasNext()) {
            copy.add(iter.next());
        }
        return copy;
    }

    /**
     * List 转 String
     *
     * @param dataList List
     * @param <T>      List类型
     * @return string
     */
    public static <T> String listToString(List<T> dataList) {
        return listToString(dataList, ";");
    }

    public static <T> String listToString(List<T> dataList, String separator) {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < dataList.size(); i++) {
            stringBuilder.append(dataList.get(i).toString());
            if (i < dataList.size() - 1) {
                stringBuilder.append(separator);
            }
        }
        return stringBuilder.toString();
    }
}
