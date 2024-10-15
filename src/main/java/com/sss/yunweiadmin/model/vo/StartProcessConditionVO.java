package com.sss.yunweiadmin.model.vo;

import com.sss.yunweiadmin.model.entity.ProcessDefinitionTask;
import lombok.Data;

import java.util.List;
//发起流程时，
@Data
public class StartProcessConditionVO  extends ProcessDefinitionTask {
    //当前节点有多条连线
    private List<String> buttonNameList;


}
