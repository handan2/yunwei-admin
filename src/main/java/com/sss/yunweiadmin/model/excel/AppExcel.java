package com.sss.yunweiadmin.model.excel;

import com.alibaba.excel.annotation.ExcelIgnore;
import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.format.DateTimeFormat;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Data;

import java.time.LocalDate;

@Data
public class AppExcel {
    //AsDeviceCommon
    @ExcelProperty("*编号")
    private String no;
    @ExcelProperty("*类别")
    private String typeName;
    @ExcelProperty("*子类")
    private String portNo;
    @ExcelProperty("*工具名称(中)")
    @ColumnWidth(20)
    private String name;
    @ExcelProperty("工具名称(英)")
    private String hostName;
    @ExcelProperty("联网类别")//20230404
    private String netType;
    @ExcelProperty("保密编号")
    private String baomiNo;
    @ExcelProperty("状态")
    private String state;
    @ExcelProperty("用途")
    private String usagee;

    @ExcelProperty("工具厂商")
    private String manufacturer;
    @ExcelProperty("工具版本")
    private String model;
//    @ExcelProperty("设备序列号")
//    private String sn;
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
