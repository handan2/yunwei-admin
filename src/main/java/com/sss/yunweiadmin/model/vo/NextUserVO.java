package com.sss.yunweiadmin.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

//页面上指定的下一步处理人
@Data
@AllArgsConstructor
public class NextUserVO {
    private String type;
    private String typeValue;
    //是否勾选了提交人部门
    private String haveStarterDept;
}
