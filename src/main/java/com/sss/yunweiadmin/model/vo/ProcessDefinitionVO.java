package com.sss.yunweiadmin.model.vo;

import com.sss.yunweiadmin.model.entity.*;
import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

@Data
public class ProcessDefinitionVO {
    private ProcessDefinition processDefinition;
    //提交流程时使用
    private List<ProcessFormTemplate> formTemplateList;
    //回显流程时使用
    private LinkedHashMap<String, ArrayList<ProcessFormTemplate>> formTemplateMap;
    //
    private List<ProcessDefinitionTask> taskList;
    private List<ProcessDefinitionEdge> edgeList;
}
