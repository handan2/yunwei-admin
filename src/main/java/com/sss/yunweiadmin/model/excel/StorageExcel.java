package com.sss.yunweiadmin.model.excel;

import com.alibaba.excel.annotation.ExcelIgnore;
import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.format.DateTimeFormat;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Data;

import java.time.LocalDate;

@Data
public class StorageExcel {
    //AsDeviceCommon
    @ExcelProperty("设备编号")
    private String no;
    @ExcelProperty("设备类别")
    private String typeName;
    @ExcelProperty("设备名称")
    @ColumnWidth(20)
    private String name;
    @ExcelProperty("联网类别")//20230404
    private String netType;
    @ExcelProperty("保密编号")
    private String baomiNo;
    @ExcelProperty("状态")
    private String state;
    @ExcelProperty("使用范围")
    private String usagee;
    @ExcelProperty("管理员")
    private String administrator;
    @ExcelProperty("责任人")
    private String userName;
    @ExcelProperty("责任部门")
    private String userDept;
    @ExcelProperty("责任人密级")
    private String userMiji;
    @ExcelProperty("设备厂商")
    private String manufacturer;
    @ExcelProperty("设备型号")
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
    @ExcelProperty("容量(G)")
    private Integer price;
    @ExcelProperty("报废日期")
//    @DateTimeFormat("yyyy/MM/dd")
    private String discardDateTmp;
    @ExcelIgnore
    private LocalDate discardDate;
    @ExcelProperty("涉密级别")
    private String miji;
    @ExcelProperty("所在位置")
    private String location;
    @ExcelProperty("备注")
    private String remark;

}
