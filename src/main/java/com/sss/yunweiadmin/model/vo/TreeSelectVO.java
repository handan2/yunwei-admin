package com.sss.yunweiadmin.model.vo;

import lombok.Data;

import java.util.List;

@Data
public class TreeSelectVO {//对应noForm的treeSelect组件的treeData属性值（数组中的成员）
    private String title;
    private Object value;//value不可重复; value在逻辑中记录的是元组的ID && 在人员/机构表中ID会复复，所以在action中对value做了“去重处理”（不过也只对部门元素做了处理）：这个字段在流程定义&&流程实例 用户选择中没有用到
   //20220903 再对部门人员场景下的value做下调整：把value设置成 "id.中文姓名.中文部门.密级"这种格式：用于变更责任人时的人员选择
    private Object key;//张强用于记录元组ID；查了官网也没知道为啥key不是唯一&&value要唯一
    private boolean selectable=true;//20211121加
    private List<TreeSelectVO> children;
}
