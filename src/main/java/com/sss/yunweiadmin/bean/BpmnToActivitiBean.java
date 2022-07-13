package com.sss.yunweiadmin.bean;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.google.common.collect.Lists;
import com.sss.yunweiadmin.model.entity.ProcessDefinition;
import com.sss.yunweiadmin.model.entity.ProcessDefinitionEdge;
import com.sss.yunweiadmin.service.ProcessDefinitionEdgeService;
import com.sss.yunweiadmin.service.ProcessDefinitionTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class BpmnToActivitiBean {
    @Autowired
    ProcessDefinitionTaskService processDefinitionTaskService;
    @Autowired
    ProcessDefinitionEdgeService processDefinitionEdgeService;
    //（注意：这里的分支条件相关xml在definition表xmlbpmn中并没有记录：直接由Db/task表生成）；
    // 这个函数是由definition表xmlbpmn转化成部署到ACTIVITI时所要补充的分支条件相关信息：
    // definition表xmlbpmn除在此用于转化成activity格式bpmnxml外，还直接用于LogicFlow的可视化流程编辑（除节点属性编辑外的）逻辑
    private Map<String, String> getEgeMap(ProcessDefinition processDefinition) {
        Map<String, String> map = new HashMap<>();
        List<ProcessDefinitionEdge> list = processDefinitionEdgeService.list(new QueryWrapper<ProcessDefinitionEdge>().eq("process_definition_id", processDefinition.getId()).ne("edge_name", ""));
        if (ObjectUtil.isNotEmpty(list)) {
            for (ProcessDefinitionEdge edge : list) {
                List<String> tmp = new ArrayList<>();
                tmp.add("<sequenceFlow id=\"" + edge.getEdgeId() + "\" name=\"" + edge.getEdgeName() + "\" sourceRef=\"" + edge.getSourceId() + "\" targetRef=\"" + edge.getTargetId() + "\">");
                String cExpressionStart = "<conditionExpression xsi:type=\"tFormalExpression\"><![CDATA[#{" ;
                String cExpressionEnd = "}]]></conditionExpression>";
                String cExpressionCenter = "";
                if(!edge.getSourceId().contains("ExclusiveGateway")){//20220628加判断
                    if (ObjectUtil.isNotEmpty(edge.getButtonName())) {
                        cExpressionCenter = edge.getSourceId() + "==\"" + edge.getButtonName()+"\"";
                    } else {//这个分支也有点问题，普通结点没有buttonName时（单分支）也没有必要设置分支条件
                      //  cExpressionCenter = edge.getConditionn()  ;
                    }
                }else{//ExclusiveGateway结点的处理
                    //2022608这里可添加判断edge表里的“流程参数字段”（可保存在现有的表字段：var_name<这个值由编辑edge属性时，下拉框筛选流程定义表单的变更字段/空转字段时获取>/condition<目前需求中这个值可不设，bpmn里的condtion条件可直接写死为！=“”这种>）是不是有值，有的话，也参考上面组装下conditionExpression
                    if(ObjectUtil.isNotEmpty(edge.getVarName())) {
                       // cExpressionCenter =  edge.getVarName() + "!=\"\"";
                        cExpressionCenter =  edge.getVarName() + edge.getConditionn()  ;
                    }//edge的condition变量还未处理
                }
                if(ObjectUtil.isNotEmpty(cExpressionCenter)) {
                    cExpressionCenter = cExpressionCenter + " && " + cExpressionCenter;//为了测语法，故意多加了一个条件
                    tmp.add(cExpressionStart + cExpressionCenter + cExpressionEnd);
                }
                tmp.add("</sequenceFlow>");
                map.put(edge.getEdgeId(), tmp.stream().collect(Collectors.joining(System.getProperty("line.separator"))));
            }
        }
        return map;
    }

    public String convert(ProcessDefinition processDefinition) {
        Map<String, String> edgeMap = getEgeMap(processDefinition);
        List<String> list = Lists.newArrayList();
        String bpmnXml = processDefinition.getBpmnXml();
        String actProcessName = processDefinition.getProcessName() + "_" + processDefinition.getId();
        list.add("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        list.add("<definitions xmlns=\"http://www.omg.org/spec/BPMN/20100524/MODEL\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:activiti=\"http://activiti.org/bpmn\" xmlns:bpmndi=\"http://www.omg.org/spec/BPMN/20100524/DI\" xmlns:omgdc=\"http://www.omg.org/spec/DD/20100524/DC\" xmlns:omgdi=\"http://www.omg.org/spec/DD/20100524/DI\" typeLanguage=\"http://www.w3.org/2001/XMLSchema\" expressionLanguage=\"http://www.w3.org/1999/XPath\" targetNamespace=\"http://www.activiti.org/test\">");
        String[] arr = bpmnXml.replaceAll("bpmn:", "").split("\\r\\n|\\r|\\n");
        for (String str : arr) {
            if (ObjectUtil.isNotEmpty(str)) {
                if (str.contains("<process")) {
                    list.add("<process id=\"" + actProcessName + "\"    isExecutable=\"true\">");
                } else if (str.contains("<startEvent")) {
                    list.add(str + "</startEvent>");
                } else if (str.contains("<endEvent")) {
                    list.add(str + "</endEvent>");
                } else if (str.contains("<exclusiveGateway")) {
                    list.add(str + "</exclusiveGateway>");
                } else if (str.contains("<parallelGateway")) {
                    list.add(str + "</parallelGateway>");
                } else if (str.contains("<startTask") || str.contains("<approvalTask") || str.contains("<handleTask") || str.contains("<archiveTask")) {
                    list.add(str.replaceAll("<(\\w+)Task", "<userTask") + "</userTask>");
                } else if (str.contains("<sequenceFlow")) {
                    String edgeId = ReUtil.getGroup0("id=\"[\\w|\\W]+?\"", str).replaceAll("id=", "").replaceAll("\"", "");
                    if (ObjectUtil.isNotEmpty(edgeMap.get(edgeId))) {
                        list.add(edgeMap.get(edgeId));
                    } else {
                        list.add(str);
                    }
                } else if (str.contains("</process>")) {
                    list.add("</process>");
                    list.add("</definitions>");
                    break;
                }
            }
        }
        return list.stream().collect(Collectors.joining(System.getProperty("line.separator")));
    }
}
