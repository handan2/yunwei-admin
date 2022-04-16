package com.sss.yunweiadmin.model.excel;

import com.alibaba.excel.annotation.ExcelIgnore;
import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.format.DateTimeFormat;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Data;

import java.time.LocalDate;

@Data
public class AsNetworkDeviceSpecialExcel {
    //AsDeviceCommon
    @ExcelProperty("资产编号")
    private String no;
    @ExcelProperty("资产类别")
    private String typeName;
    @ExcelProperty("资产名称")
    @ColumnWidth(20)
    private String name;
    @ExcelProperty("保密编号")
    private String baomiNo;
    @ExcelProperty("资金来源")
    private String fundSrc;
    @ExcelProperty("联网类别")
    private String netType;
    @ExcelProperty("信息点号")
    private String portNo;
    @ExcelProperty("主机名称")
    private String hostName;
    @ExcelProperty("所在位置")
    private String location;
    @ExcelProperty("是否合用")
    private String shared;
    @ExcelProperty("合用人")
    private String nameShared;
    @ExcelProperty("状态")
    private String state;
    @ExcelProperty("用途")
    private String usagee;
    @ExcelProperty("管理员")
    private String administrator;
    @ExcelProperty("管理员电话")
    private String adminTel;
    @ExcelProperty("使用人")
    private String userName;
    @ExcelProperty("使用部门")
    private String userDept;
    @ExcelProperty("使用人密级")
    private String userMiji;
    @ExcelProperty("使用人电话")
    private String userTel;
    @ExcelProperty("设备厂商")
    private String manufacturer;
    @ExcelProperty("设备型号")
    private String model;
    @ExcelProperty("设备序列号")
    private String sn;
    @ExcelProperty("生产日期")
    @DateTimeFormat("yyyy-MM-dd")
    private String madeDateTmp;
    @ExcelIgnore
    private LocalDate madeDate;
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
    @ExcelProperty("价格")
    private Integer price;
    @ExcelProperty("报废日期")
    @DateTimeFormat("yyyy-MM-dd")
    private String discardDateTmp;
    @ExcelIgnore
    private LocalDate discardDate;
    @ExcelProperty("IP地址")
    private String ip;
    @ExcelProperty("MAC地址")
    private String mac;
    @ExcelProperty("涉密级别")
    private String miji;
    @ExcelProperty("备注")
    private String remark;
    //AsNetworkDeviceSpecial
    @ExcelProperty("内存ROM(MB)")
    @ColumnWidth(20)
    private String rom;
    @ExcelProperty("内存FLASH(MB)")
    @ColumnWidth(20)
    private String flash;
    @ExcelProperty("端口总数")
    @ColumnWidth(15)
    private String portTotal;
    @ExcelProperty("IOS版本")
    @ColumnWidth(15)
    private String ios;
}
