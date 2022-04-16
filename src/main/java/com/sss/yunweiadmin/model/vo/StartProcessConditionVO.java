package com.sss.yunweiadmin.model.vo;

import lombok.Data;

import java.util.List;
//发起流程时，
@Data
public class StartProcessConditionVO {
    //当前节点有多条连线
    private List<String> buttonNameList;
    //是否允许指定下一步处理人
    private String haveNextUser;
}
