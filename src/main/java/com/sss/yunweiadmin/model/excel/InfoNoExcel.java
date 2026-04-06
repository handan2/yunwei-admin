package com.sss.yunweiadmin.model.excel;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

@Data
public class InfoNoExcel {
    @ExcelProperty("联网类别")
    private String netType;
    @ExcelProperty("信息点号")
    private String value;
    @ExcelProperty("端口号")
    private String port;
    @ExcelProperty("交换机IP")
    private String ip;
    @ExcelProperty("状态")
    private String status;
    @ExcelProperty("关联资产号")
    private String assetNoStr;
    @ExcelProperty("位置")
    private String location;
    @ExcelProperty("备注")
    private String remark;
}
