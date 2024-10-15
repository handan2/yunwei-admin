package com.sss.yunweiadmin.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

//页面上指定的下一步处理人
@Data
@AllArgsConstructor
public class AssiginTaskAndUserVO {//这是接受前端返回的vo
    private String operatorType;
    private String operatorTypeIds;
    //是否勾选了提交人部门
    private String haveStarterDept;
    private String assiginTask; //如果是“精典的nextUser场景”：这里也给值“下一节点”
    private String assiginUser;//不需要，有空删除，包括task表本身这个字段也没用
}
