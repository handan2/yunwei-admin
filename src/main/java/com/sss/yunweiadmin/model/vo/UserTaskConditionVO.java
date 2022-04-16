package com.sss.yunweiadmin.model.vo;

import lombok.Data;

import java.util.List;

@Data
public class UserTaskConditionVO {
    //当前节点有多条连线
    private List<String> buttonNameList;
    //是否允许填写审批意见
    private String haveComment;
    // 是否允许修改表单
    private String haveEditForm;
    //是否允许指定下一步处理人
    private String haveNextUser;
    //是否显示操作记录
    private String haveOperate;
}
