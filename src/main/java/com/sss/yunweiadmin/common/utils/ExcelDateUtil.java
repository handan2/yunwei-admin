package com.sss.yunweiadmin.common.utils;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.ReflectUtil;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;


public class ExcelDateUtil {
    //将easyexcel的字符串日期转为日期类型
    public static <T> void converToDate(List<T> dataList, Class clazz) {
        //获取buyDateTmp格式的日期字段名称
        List<String> dateNameList = new ArrayList<>();
        Field[] fieldArr = ReflectUtil.getFields(clazz);
        for (Field field : fieldArr) {
            if (field.getName().endsWith("Tmp")) {
                dateNameList.add(field.getName());
            }
        }
        //
        if (ObjectUtil.isNotEmpty(dateNameList)) {
            for (T obj : dataList) {
                for (String dateName : dateNameList) {
                    String dateValue = (String) ReflectUtil.getFieldValue(obj, dateName);
                    if (ObjectUtil.isNotEmpty(dateValue)) {
                        dateValue = dateValue.replaceAll("\"", "");
                        //字符串转为LocalDateTime
                        //yyyy-MM-dd HH:mm:ss
                        String regex1 = "\\d{4}[-]\\d{2}[-]\\d{2} \\d{2}:\\d{2}:\\d{2}";
                        //yyyy-MM-dd
                        String regex2 = "\\d{4}[-]\\d{2}[-]\\d{2}";
                        if (ReUtil.isMatch(regex1, dateValue)) {
                            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                            ReflectUtil.setFieldValue(obj, dateName.replaceAll("Tmp", ""), LocalDateTime.parse(dateValue, dateTimeFormatter));
                        } else if (ReUtil.isMatch(regex2, dateValue)) {
                            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                            ReflectUtil.setFieldValue(obj, dateName.replaceAll("Tmp", ""), LocalDate.parse(dateValue, dateTimeFormatter));
                        } else {
                            throw new RuntimeException(dateName + "错误");
                        }
                    }
                }
            }
        }
    }
}
