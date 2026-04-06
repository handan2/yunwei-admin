package com.sss.yunweiadmin.model.vo;

import com.sss.yunweiadmin.model.entity.DiskForHisForProcess;
import com.sss.yunweiadmin.model.entity.ProcessFormValue1;
import com.sss.yunweiadmin.model.entity.ProcessFormValue2;
import lombok.Data;

import java.util.List;

@Data
public class ModifyProcessFormVO {
    private Integer processFormValue1Id;
   // private String value;
    private ProcessFormValue1 value1;
    private List<ProcessFormValue2> value2List;//只传了一部分属性
    private List<DiskForHisForProcess> diskListForHisForProcess;
}
