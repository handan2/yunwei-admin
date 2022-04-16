package com.sss.yunweiadmin.model.vo;

import com.sss.yunweiadmin.model.entity.ProcessInstanceData;
import lombok.Data;

import java.util.List;

@Data
public class StartProcessResultVO {
   private Boolean isSuccess;
   private List<ProcessInstanceData> processInstanceDataList;

}
