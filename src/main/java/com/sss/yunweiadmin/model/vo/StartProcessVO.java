package com.sss.yunweiadmin.model.vo;

import com.sss.yunweiadmin.model.entity.AsDeviceCommon;
import com.sss.yunweiadmin.model.entity.DiskForHisForProcess;
import com.sss.yunweiadmin.model.entity.ProcessFormValue1;
import com.sss.yunweiadmin.model.entity.ProcessFormValue2;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

//启动流程实例时，表单数据
@Data
@EqualsAndHashCode(callSuper = false)
//public class StartProcessVO extends ProcessFormValue1 {//20220531 直接改成属性成员:格式上也能和checkVO对应
public class StartProcessVO  {
    private ProcessFormValue1 value1;//20220531
    private List<ProcessFormValue2> value2List;//只传了一部分属性
    //完成用户任务时，提供条件值
    private String buttonName;
    //是否允许指定下一步处理人
    private String haveNextUser;
    private String operatorType;
    private String operatorTypeValue;
    private String operatorTypeLabel;
    //是否勾选了提交人部门
    private String haveStarterDept;
    private List<DiskForHisForProcess> diskListForHisForProcess;//20220620
}
