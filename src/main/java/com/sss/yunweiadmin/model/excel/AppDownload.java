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
public class AppDownload {
    //AsDeviceCommon
    @ColumnWidth(value=8)
    @ExcelProperty("序号")
    private Integer order;
    @ExcelProperty("*编号")
    private String no;
    @ExcelProperty("*类别")
    private String typeName;
    @ExcelProperty("子类")
    private String port_no;
    @ExcelProperty("*工具名称(中)")
    @ColumnWidth(20)
    private String name;
    @ExcelProperty("工具名称(英)")
    private String hostName;
    @ExcelProperty("联网类别")//20230404
    private String netType;
    @ExcelProperty("状态")
    private String state;
    @ExcelProperty("用途")
    private String usagee;

    @ExcelProperty("工具厂商")
    private String manufacturer;
    @ExcelProperty("工具版本")
    private String model;
    @ExcelProperty("设备序列号")
    private String sn;
    @ExcelIgnore
    private LocalDate buyDate;
    @ExcelProperty("首次启用日期")
    @DateTimeFormat("yyyy-MM-dd")
    private String useDateTmp;
    @ExcelIgnore
    private LocalDate useDate;

    @ExcelProperty("国别")
    private String miji;
    @ExcelProperty("安装范围")
    private String location;
    @ExcelProperty("备注")
    private String remark;

}
