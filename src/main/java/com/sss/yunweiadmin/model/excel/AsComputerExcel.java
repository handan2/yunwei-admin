package com.sss.yunweiadmin.model.excel;

import com.alibaba.excel.annotation.ExcelIgnore;
import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.format.DateTimeFormat;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class AsComputerExcel {
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
    //AsComputerSpecial
    @ExcelProperty("网络接口")
    private String netInterface;
    @ExcelProperty(" 物理内存(MB)")
    private Integer ram;
    @ExcelProperty("光驱")
    private String cdrom;
    @ExcelProperty("显卡")
    private String videoCard;
    @ExcelProperty("备用网卡MAC")
    private String macBackup;
    @ExcelProperty("声卡")
    private String soundCard;
    @ExcelProperty("操作系统安装时间")
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    private String osDateTmp;
    @ExcelIgnore
    private LocalDateTime osDate;
    @ExcelProperty("操作系统")
    private String osType;
    @ExcelProperty("CPU个数")
    private Integer cpuTotal;
    @ExcelProperty("硬盘个数")
    private Integer diskTotal;
    @ExcelProperty("硬盘总容量")
    private Integer diskSize;
    @ExcelProperty("硬盘型号1")
    private String diskMode1;
    @ExcelProperty("硬盘型号2")
    private String diskMode2;
    @ExcelProperty("硬盘型号3")
    private String diskMode3;
    @ExcelProperty("硬盘型号4")
    private String diskMode4;
    @ExcelProperty("硬盘序列号1")
    private String diskSn1;
    @ExcelProperty("硬盘序列号2")
    private String diskSn2;
    @ExcelProperty("硬盘序列号3")
    private String diskSn3;
    @ExcelProperty("硬盘序列号4")
    private String diskSn4;
    //AsComputerGranted
    @ExcelProperty("USB接口")
    private String usb;
    @ExcelProperty("串口")
    private String serial;
    @ExcelProperty("并口")
    private String parallel;
    @ExcelProperty("红外")
    private String hongwai;
    @ExcelProperty("蓝牙")
    private String bluetooth;
    @ExcelProperty("拷屏")
    private String screenShot;
    @ExcelProperty("dev1394")
    private String dev1394;
    @ExcelProperty("设备接入")
    private String connection;
    @ExcelProperty("IP绑定")
    private String ipBind;
    @ExcelProperty("虚拟机")
    private String vm;
    @ExcelProperty("文件共享")
    private String docShare;
    @ExcelProperty("PCMCIA")
    private String pcmcia;
    @ExcelProperty("图形设备")
    private String devImage;
    @ExcelProperty("卷影设备")
    private String devJuanying;
    @ExcelProperty("多功能导入装置涉密口")
    private String portShemi;
    @ExcelProperty("多功能导入装置通用口")
    private String portCommon;
}
