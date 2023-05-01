package com.sss.yunweiadmin.model.vo;

import lombok.Data;

import java.util.List;

@Data
public class ScoreNodeVO {
    //审批节点名称（不含处理节点）
    private List<String> nodeNameList;
    //处理人名称（不含审批节点的处理人）
    private List<String> operatorNameList;

}
