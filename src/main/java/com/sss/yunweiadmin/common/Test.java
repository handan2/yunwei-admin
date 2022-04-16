package com.sss.yunweiadmin.common;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Test {
    public static void main(String[] args) throws Exception {
        String startDate = "2021-02-14 00:00:00,2021-02-15 00:00:00";
        String aaa = "xxxxxss";
        String[] bbb= aaa.split(",");
        System.out.println("bbb");
        System.out.println(bbb.length);
        String[] dateArr = startDate.split(",");
        System.out.println(dateArr);
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime startDateTime0 = LocalDateTime.parse(dateArr[0], dateTimeFormatter);
        LocalDateTime startDateTime1 = LocalDateTime.parse(dateArr[1], dateTimeFormatter);
        System.out.println(startDateTime0);
        System.out.println(startDateTime1);
    }
}
