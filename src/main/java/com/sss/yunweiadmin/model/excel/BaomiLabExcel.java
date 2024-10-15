package com.sss.yunweiadmin.model.excel;

import com.alibaba.excel.annotation.ExcelIgnore;
import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.format.DateTimeFormat;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Data;

import java.time.LocalDate;

@Data
public class BaomiLabExcel {
    //Format,userDept,type,miji,userName,no,baomiNo,ypSn,Printer
    @ExcelProperty("Format")
    private String format;
    @ExcelProperty("title")
    private String title;
    @ExcelProperty("userDept")
    private String userDept;
    @ExcelProperty("type")
    private String type;
    @ExcelProperty("miji")
    private String miji;
    @ExcelProperty("userName")
    private String userName;
    @ExcelProperty("no")
    private String no;
    @ExcelProperty("sn")
    private String sn;
    @ExcelProperty("baomiNo")
    private String baomiNo;
    @ExcelProperty("Printer")
    private String printer;
}
