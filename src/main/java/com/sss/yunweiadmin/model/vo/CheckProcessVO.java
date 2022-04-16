package com.sss.yunweiadmin.model.vo;

import lombok.Data;

//审批流程实例时，表单数据
@Data
public class CheckProcessVO {
    private Integer processInstanceDataId;
    //完成用户任务时，提供条件值
    private String buttonName;
    //是否允许填写审批意见
    private String haveComment;
    //审批意见
    private String comment;
    //
    private String haveOperate;
    private String operate;
    // 是否允许修改表单
    private String haveEditForm;
    private String value;
    //是否允许指定下一步处理人
    private String haveNextUser;
    private String type;
    private String typeValue;
    private String typeLabel;
}
