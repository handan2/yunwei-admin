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
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class ExcelDateUtil {
    //将easyexcel的字符串日期转为localDate日期类型
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
                        //yyyy/MM/dd
                        String regex3 = "\\d{4}[/]\\d{2}[/]\\d{2}";
                        //  DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd"); 测试成功
                        // ReflectUtil.setFieldValue(obj, dateName.replaceAll("Tmp", ""), LocalDate.parse(dateValue, dateTimeFormatter));
                        if (ReUtil.isMatch(regex1, dateValue)) { //20221115 两类日期格式都统一转成了短日期
                            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                            ReflectUtil.setFieldValue(obj, dateName.replaceAll("Tmp", ""), LocalDateTime.parse(dateValue, dateTimeFormatter).toLocalDate());

                        } else if (ReUtil.isMatch(regex2, dateValue)) {
                            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                            ReflectUtil.setFieldValue(obj, dateName.replaceAll("Tmp", ""), LocalDate.parse(dateValue, dateTimeFormatter));
                        } else if (ReUtil.isMatch(regex3, dateValue)) {
                            //20221215 注意 excel里如果是2022/12/1这种也报错：必须是写全了“2022/12/01”:不过，从DB里导出来的应该都是全的吧(todo再确认下)？（VS excel里手工调只能是2022/12/1这咱）
                            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd");
                            ReflectUtil.setFieldValue(obj, dateName.replaceAll("Tmp", ""), LocalDate.parse(dateValue, dateTimeFormatter));
                        } else {
                            throw new RuntimeException(dateName + "错误");
                        }
                    }
                }
            }
        }
    }

    //将entity的localDate日期类型转为字符串,并保存在excelDataVO中的相应字段（字段名约定为'db中字段名+tmp后缀'）
    public static <T,V> void dateConverToString(T  dbData, V excelData,Class clazz) {
        //获取buyDateTmp格式的日期字段名称
        List<String> dateNameList = new ArrayList<>();
        Field[] fieldArr = ReflectUtil.getFields(clazz);
        for (Field field : fieldArr) {
            if (field.getName().endsWith("Date")) {//20230307约定了日期类型字段命名必须以‘date’结尾；暂不考虑dateTime类型
                dateNameList.add(field.getName());
            }
        }
        //
        if (ObjectUtil.isNotEmpty(dbData)) {
            for (String dateName : dateNameList) {
                LocalDate dateValue = (LocalDate) ReflectUtil.getFieldValue(dbData, dateName);
                if (ObjectUtil.isNotEmpty(dateValue)) {
                    DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy/MM/dd");
                    String dateStr = dateValue.format(fmt);
                    ReflectUtil.setFieldValue(excelData, dateName+"Tmp", dateStr);


                }
            }
        }

    }
}
