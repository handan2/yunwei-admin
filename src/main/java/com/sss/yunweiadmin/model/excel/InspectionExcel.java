package com.sss.yunweiadmin.model.excel;

import com.alibaba.excel.annotation.ExcelIgnore;
import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.format.DateTimeFormat;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Data;

import java.time.LocalDate;

@Data
public class InspectionExcel {
    @ExcelProperty("*检查人")
    private String inspector;
    @DateTimeFormat("yyyy-MM-dd")
    @ExcelProperty("*检查时间")
    private String inspectDateTmp;
    @ExcelIgnore
    private LocalDate inspectDate;
    @ExcelProperty("*设备编号")
    private String no;
//
//@ExcelIgnore
//    @ExcelProperty("设备密级")
//    private String miji;
//    @ExcelIgnore
//    @ExcelProperty("联网类别")
//    private String netType;
//    @ExcelProperty("责任部门")
//    private String userDept;
//    @ExcelIgnore
//    @ExcelProperty("责任人")
//    private String userName;
    @ColumnWidth(40)
    @ExcelProperty("账物标识问题")
    private String label;
    //    @ExcelProperty("检查模式")
//    private String mode;
    @ColumnWidth(40)
    @ExcelProperty("系统设置及安装软件问题")
    private String system;
    @ColumnWidth(40)
    @ExcelProperty("安全产品问题")
    private String safeSoft;
    @ColumnWidth(40)
    @ExcelProperty("违规接入问题")
    private String illegalAccess;
    @ColumnWidth(40)
    @ExcelProperty("文件问题")
    private String files;
    @ColumnWidth(40)
    @ExcelProperty("其他问题")
    private String others;
}
