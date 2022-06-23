package com.sss.yunweiadmin.model.vo;

import com.sss.yunweiadmin.model.entity.DiskForHisForProcess;
import com.sss.yunweiadmin.model.entity.ProcessFormValue1;
import com.sss.yunweiadmin.model.entity.ProcessFormValue2;
import lombok.Data;

import java.util.List;

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
    private ProcessFormValue1 value1;//20220531 为了在流程核验validate方法的参数中与startVo兼容；其实只用到了里面的defitionId & value两个属性
    private String valueOfValue1;
    //20220531添加
    private List<ProcessFormValue2> value2List;//只传了一部分属性
    //是否允许指定下一步处理人
    private String haveNextUser;
    private String operatorType;
    private String operatorTypeValue;
    private String operatorTypeLabel;
    //20220621加：为了最后流程结束保存结果时用
    private List<DiskForHisForProcess> diskListForHisForProcess;
}
