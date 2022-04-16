package com.sss.yunweiadmin.model.vo;

import lombok.Data;

import java.util.List;

@Data
public class ScoreNodeVO {
    //审批节点名称
    private List<String> nodeNameList;
    //处理人名称
    private List<String> operatorNameList;

}
