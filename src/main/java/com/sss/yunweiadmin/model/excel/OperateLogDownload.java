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
public class OperateLogDownload {
    //AsDeviceCommon
    @ColumnWidth(value=8)
    @ExcelProperty("序号")
    private Integer order;
    @ExcelProperty("登陆账号")
    private String loginName;
    @ExcelProperty("用户姓名")
    private String displayName;
    @ExcelProperty("IP")
    private String ip;
    @ExcelProperty("操作时间")
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    private String createDatetimeTmp;
    @ExcelIgnore
    private LocalDate createDatetime;
    @ExcelProperty("操作模块")
    private String operateModule;
    @ExcelProperty("操作类型")
    private String operateType;
    @ExcelProperty("操作参数")
    private String param;


}
