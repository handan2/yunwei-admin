package com.sss.yunweiadmin.model.excel;

import com.alibaba.excel.annotation.ExcelIgnore;
import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.format.DateTimeFormat;
import com.alibaba.excel.annotation.write.style.*;
import lombok.Data;
import org.apache.poi.ss.usermodel.HorizontalAlignment;

import java.time.LocalDate;

@Data
@HeadStyle(fillBackgroundColor = 360,shrinkToFit = true)
//以下这三个函数内参数都不起作用
@ContentStyle(fillBackgroundColor =360,shrinkToFit = true,horizontalAlignment = HorizontalAlignment.CENTER)
@HeadRowHeight(value = 30)//这个起作用
@ContentRowHeight(value = 30)
@ColumnWidth(value=15)
public class AsAffDownload {
    //AsDeviceCommon
    @ColumnWidth(value=8)
    @ExcelProperty("序号")
    private Integer order;
    @ExcelProperty("编号")
    private String no;
    @ExcelProperty("名称")
    private String typeName;
    @ExcelProperty("责任人")
    private String userName;
    @ExcelProperty("部门")
    private String userDept;
    @ExcelProperty("联网类别")
    private String netType;
    @ExcelProperty("所在位置")
    private String location;
    @ExcelProperty("合用人")
    private String nameShared;
    @ExcelProperty("状态")
    private String state;
    @ExcelProperty("用途")
    private String usagee;
    @ExcelProperty("设备厂商")
    private String manufacturer;
    @ExcelProperty("设备型号")
    private String model;
    @ExcelProperty("保密编号")
    private String baomiNo;
    @ExcelProperty("设备序列号")
    private String sn;
    @ExcelProperty("购买日期")
    @DateTimeFormat("yyyy-MM-dd")
    private String buyDateTmp;
    @ExcelIgnore
    private LocalDate buyDate;
    @ExcelProperty("启用日期")
    @DateTimeFormat("yyyy-MM-dd")
    private String useDateTmp;
    @ExcelIgnore
    private LocalDate useDate;
    @ExcelProperty("涉密级别")
    private String miji;
    //AsComputerSpecial
    //20221115 这个类里的@DateTimeFormat字符串转字符串时，这个@DateTimeFormat不起作用：原字符串啥格式也行&&原样转入；
    // 是经测发现，excel的日期类型如2021/11/12: 用 @DateTimeFormat("yyyy/MM/dd")也能直接转成相应"yyyy/MM/dd"字符串 && 用 @DateTimeFormat("yyyy-MM-dd")也能直接转成用 yyyy-MM-dd字符串
    // 小结:这个注解可以把excel里日期格式（如果是字符串<20220104试了下自定义格式的‘2022-01-01 00：00：00’也不起作用>，将不起作用）的时间数据（按注解里的参数格式）转成字符串

//    @ExcelProperty("硬盘总容量(GB)")
//    private Integer diskSize;
    @ExcelProperty("上位机")//20230301 todo进一步梳理
    private String accessHostNo;
    @ExcelProperty("备注")
    private String remark;

}
