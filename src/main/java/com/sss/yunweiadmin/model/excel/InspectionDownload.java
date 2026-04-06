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
public class InspectionDownload {
    //AsDeviceCommon
    @ColumnWidth(value=8)
    @ExcelProperty("序号")
    private Integer order;
    @ExcelProperty("*检查人")
    private String inspector;
    @DateTimeFormat("yyyy-MM-dd")
    @ExcelProperty("*检查时间")
    private String inspectDateTmp;
    @ExcelIgnore
    private LocalDate inspectDate;
    @ExcelProperty("*设备编号")
    private String no;
    @ExcelProperty("设备类别")
    private String type;
    @ExcelProperty("设备密级")
    private String miji;
    @ExcelProperty("联网类别")
    private String netType;
    @ExcelProperty("责任部门")
    private String userDept;
    @ExcelProperty("责任人")
    private String userName;
    @ExcelProperty("账物标识问题")
    private String label;
    @ExcelProperty("检查模式")
    private String mode;
    @ExcelProperty("系统设置及安装软件问题")
    private String system;
    @ExcelProperty("安全产品问题")
    private String safeSoft;
    @ExcelProperty("违规接入问题")
    private String illegalAccess;
    @ExcelProperty("文件问题")
    private String files;
    @ExcelProperty("其他问题")
    private String others;
    @ExcelProperty("*填报时间")
    private String createDatetime;

}
