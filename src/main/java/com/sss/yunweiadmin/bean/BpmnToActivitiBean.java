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

    private Map<String, String> getEgeMap(ProcessDefinition processDefinition) {
        Map<String, String> map = new HashMap<>();
        List<ProcessDefinitionEdge> list = processDefinitionEdgeService.list(new QueryWrapper<ProcessDefinitionEdge>().eq("process_definition_id", processDefinition.getId()).ne("edge_name", ""));
        if (ObjectUtil.isNotEmpty(list)) {
            for (ProcessDefinitionEdge edge : list) {
                List<String> tmp = new ArrayList<>();
                tmp.add("<sequenceFlow id=\"" + edge.getEdgeId() + "\" name=\"" + edge.getEdgeName() + "\" sourceRef=\"" + edge.getSourceId() + "\" targetRef=\"" + edge.getTargetId() + "\">");
                if (ObjectUtil.isNotEmpty(edge.getButtonName())) {
                    tmp.add("<conditionExpression xsi:type=\"tFormalExpression\"><![CDATA[#{" + edge.getSourceId() + "==\"" + edge.getButtonName() + "\"}]]></conditionExpression>");
                } else {
                    tmp.add("<conditionExpression xsi:type=\"tFormalExpression\"><![CDATA[#{" + edge.getConditionn() + "}]]></conditionExpression>");
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
