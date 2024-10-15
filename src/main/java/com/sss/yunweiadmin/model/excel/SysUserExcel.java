package com.sss.yunweiadmin.model.excel;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

@Data
public class SysUserExcel {
    @ExcelProperty("登录账号")
    private String loginName;
    @ExcelProperty("显示姓名")
    private String displayName;
    @ExcelProperty("密级")
    private String secretDegree;
    @ExcelProperty("身份证号")
    private String idNumber;
    @ExcelProperty("部门")
    private String deptName;
}
