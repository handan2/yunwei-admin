package com.sss.yunweiadmin.model.vo;

import com.sss.yunweiadmin.model.entity.ProcessDefinitionTask;
import com.sss.yunweiadmin.model.entity.SystemInfo;
import com.sss.yunweiadmin.model.entity.Usblog;
import lombok.Data;

import java.util.List;

//审批流程时，各种条件数据
@Data
public class UploadCheckInfoVO {

    private List<Usblog> usblogsList;

    private SystemInfo systemInfo;

}
