package com.sss.yunweiadmin.model.vo;

import com.sss.yunweiadmin.model.entity.ProcessInstanceData;
import lombok.Data;

import java.util.List;

@Data
public class StartOrHandleProcessResultVO {
   private Boolean isSuccess;
   private List<ProcessInstanceData> processInstanceDataList;

}
