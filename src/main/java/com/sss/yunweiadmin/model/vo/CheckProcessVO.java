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
    private String haveComment;//这个是可以从DB中直接读的，不需要从前端传
    //审批意见
    private String comment;
    //
    private String haveOperate;//这个是可以从DB中直接读的，
    private String operate;
    // 是否允许修改表单
    private String haveEditForm;//这个是可以从DB中直接读的，
    private ProcessFormValue1 value1;//20220531 为了在流程核验validate方法的参数中与startVo兼容；其实只用到了里面的defitionId & value两个属性
    private String valueOfValue1;
    //20220531添加
    private List<ProcessFormValue2> value2List;//只传了一部分属性
    //是否允许指定下一步处理人
    private String haveNextUser;//这个是可以从DB中直接读的，
    private String operatorType;//这个是下一步处理人信息：DB中没有值，只能从前端读
    private String operatorTypeValue;//这个是下一步处理人信息：DB中没有值，只能从前端读
    private String operatorTypeLabel;//这个是下一步处理人信息：DB中没有值，只能从前端读
    //20220626加
    private String haveSelectProcess;//这个是可以从DB中直接读的，
    private String selectedProcess;
    //20220621加：为了最后流程结束保存结果时用
    private List<DiskForHisForProcess> diskListForHisForProcess;
}
