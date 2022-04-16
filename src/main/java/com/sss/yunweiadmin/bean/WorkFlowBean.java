package com.sss.yunweiadmin.bean;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sss.yunweiadmin.model.entity.ProcessDefinitionEdge;
import com.sss.yunweiadmin.model.entity.ProcessInstanceNode;
import com.sss.yunweiadmin.model.entity.SysUser;
import com.sss.yunweiadmin.service.*;
import org.activiti.engine.HistoryService;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.history.HistoricTaskInstance;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpSession;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class WorkFlowBean {
    @Autowired
    RuntimeService runtimeService;
    @Autowired
    RepositoryService repositoryService;
    @Autowired
    TaskService taskService;
    @Autowired
    HistoryService historyService;
    @Autowired
    HttpSession httpSession;
    @Autowired
    SysDeptService sysDeptService;
    @Autowired
    ProcessInstanceDataService processInstanceDataService;
    @Autowired
    ProcessInstanceNodeService processInstanceNodeService;
    @Autowired
    UserTaskBean userTaskBean;
    @Autowired
    ProcessDefinitionTaskService processDefinitionTaskService;
    @Autowired
    ProcessDefinitionEdgeService processDefinitionEdgeService;


    public Deployment deploy(String actProcessName, String activitiXml) {
        return repositoryService.createDeployment().name(actProcessName).addString(actProcessName + ".bpmn", activitiXml).deploy();
    }

    //级联删除流程部署
    public void deleteDeploy(String deployId) {
        repositoryService.deleteDeployment(deployId, true);
    }

    //删除流程实例
    public void deleteProcessInstance(String actProcessInstanceId) {
        if (finish(actProcessInstanceId)) {
            historyService.deleteHistoricProcessInstance(actProcessInstanceId);
        } else {
            //删除顺序不能换
            runtimeService.deleteProcessInstance(actProcessInstanceId, "删除原因");
            historyService.deleteHistoricProcessInstance(actProcessInstanceId);
        }
    }

    public ProcessInstance startProcessInstance(String actProcessName, Integer businessId) {
        //processDefinitionKey,必须是activitiXml中的process标签的id
        return runtimeService.startProcessInstanceByKey(actProcessName, businessId + "");
    }

    public HistoricTaskInstance getHistoricTaskInstance(String actProcessInstanceId, String taskDefinitionKey) {
        List<HistoricTaskInstance> list = historyService.createHistoricTaskInstanceQuery()
                .processInstanceId(actProcessInstanceId)
                .taskDefinitionKey(taskDefinitionKey)
                .orderByHistoricTaskInstanceEndTime()
                .desc()
                .list();
        return list.get(0);
    }


    public List<Task> getActiveTask(String actProcessInstanceId) {
        return taskService.createTaskQuery().processInstanceId(actProcessInstanceId).active().list();
    }


    public List<Task> getMyTask(String actProcessInstanceId) {
        SysUser user = (SysUser) httpSession.getAttribute("user");
        return taskService.createTaskQuery().processInstanceId(actProcessInstanceId).taskCandidateOrAssigned(user.getLoginName()).active().list();
    }


    public Map<String, String> getCurrentStep(Integer processDefinitionId, Integer processInstanceDataId, String actProcessInstanceId, String preTaskId) {
        Map<String, String> resultMap = new HashMap<>();
        if (!this.finish(actProcessInstanceId)) {
            //显示名称
            List<String> displayList = new ArrayList<>();
            //登录名称
            List<String> loginList = new ArrayList<>();
            //历史处理节点
            List<ProcessInstanceNode> list = processInstanceNodeService.list(new QueryWrapper<ProcessInstanceNode>().eq("process_instance_data_id", processInstanceDataId));
            Map<String, ProcessInstanceNode> map = list.stream().collect(Collectors.toMap(ProcessInstanceNode::getTaskId, v -> v, (key1, key2) -> key2));
            //获取当前活动任务
            List<Task> taskList = this.getActiveTask(actProcessInstanceId);

            for (Task task : taskList) {
                ProcessInstanceNode processInstanceNode = map.get(task.getTaskDefinitionKey());
                if (processInstanceNode != null) {
                    //存在历史节点，使用历史处理人
                    displayList.add(processInstanceNode.getTaskName() + "[" + processInstanceNode.getDisplayName() + "]");
                    loginList.add(processInstanceNode.getLoginName());
                } else {
                    //获取处理人
                    //20211210修改实参
                    List<SysUser> userList = userTaskBean.getUserList(processDefinitionId, preTaskId, task.getTaskDefinitionKey(),processInstanceDataId);
                    List<String> displayNameList = userList.stream().map(SysUser::getDisplayName).collect(Collectors.toList());
                    List<String> loginNameList = userList.stream().map(SysUser::getLoginName).collect(Collectors.toList());
                    displayList.add(task.getName() + "[" + String.join(",", displayNameList) + "]");
                    loginList.add(String.join(",", loginNameList));
                }
            }
            resultMap.put("displayName", String.join(",", displayList));
            resultMap.put("loginName", String.join(",", loginList));
        }
        return resultMap;
    }

    //该节点有多条连线，即多个提交按钮
    public void completeTaskByButtonName(Integer processDefinitionId, Task actTask, String buttonName) {
        SysUser currentUser = (SysUser) httpSession.getAttribute("user");
        //拾取任务
        taskService.claim(actTask.getId(), currentUser.getLoginName());
        //设置buttonName条件和排他网关条件
        Map<String, Object> map = new HashMap<>();
        map.put(actTask.getTaskDefinitionKey(), buttonName);
        //判断任务的下一个节点有没有排他网关
        String taskId = actTask.getTaskDefinitionKey();
        List<ProcessDefinitionEdge> exclusiveGatewayList = getExclusiveGatewayCondition(processDefinitionId, taskId);
        if (ObjectUtil.isNotEmpty(exclusiveGatewayList)) {
            Set<String> varNameSet = exclusiveGatewayList.stream().map(ProcessDefinitionEdge::getVarName).collect(Collectors.toSet());
            //设置排他网关条件，自由发挥
            map.put("aa", 100);
        }
        //完成任务
        taskService.complete(actTask.getId(), map);

    }

    public void completeTask(Integer processDefinitionId, Task actTask) {
        SysUser currentUser = (SysUser) httpSession.getAttribute("user");
        //拾取任务
        taskService.claim(actTask.getId(), currentUser.getLoginName());
        //判断任务的下一个节点有没有排他网关
        String taskId = actTask.getTaskDefinitionKey();
        List<ProcessDefinitionEdge> exclusiveGatewayList = getExclusiveGatewayCondition(processDefinitionId, taskId);
        if (ObjectUtil.isNotEmpty(exclusiveGatewayList)) {
            Set<String> varNameSet = exclusiveGatewayList.stream().map(ProcessDefinitionEdge::getVarName).collect(Collectors.toSet());
            //设置排他网关条件，自由发挥
            Map<String, Object> map = new HashMap<>();
            map.put("aa", 100);
            //完成任务
            taskService.complete(actTask.getId(), map);
        } else {
            //完成任务
            taskService.complete(actTask.getId());
        }
    }

    public boolean finish(String actProcessInstanceId) {
        return runtimeService.createProcessInstanceQuery().processInstanceId(actProcessInstanceId).singleResult() == null;
    }

    public Integer getBusinessKeyByProcessInstanceId(String actProcessInstanceId) {
        ProcessInstance pi = runtimeService.createProcessInstanceQuery()
                .processInstanceId(actProcessInstanceId)
                .singleResult();
        return pi == null ? null : Integer.parseInt(pi.getBusinessKey());
    }

    //获取节点的多条连线
    public List<String> getButtonNameList(Integer processDefinitionId, String taskId) {
        List<String> buttonNameList = null;
        //判断是否有多条连线
        List<ProcessDefinitionEdge> edgeList = processDefinitionEdgeService.list(new QueryWrapper<ProcessDefinitionEdge>().eq("process_definition_id", processDefinitionId).eq("source_id", taskId));
        if (ObjectUtil.isNotEmpty(edgeList)) {
            List<String> list = edgeList.stream().filter(item -> ObjectUtil.isNotEmpty(item.getButtonName())).map(ProcessDefinitionEdge::getButtonName).collect(Collectors.toList());
            if (ObjectUtil.isNotEmpty(list)) {
                buttonNameList = list;
            }
        }
        return buttonNameList;
    }

    //获取节点的下一个节点(排他网关)的连线条件
    public List<ProcessDefinitionEdge> getExclusiveGatewayCondition(Integer processDefinitionId, String taskId) {
        List<ProcessDefinitionEdge> list = null;
        //排他网关的连线的id
        List<ProcessDefinitionEdge> exclusiveGatewayTmp = processDefinitionEdgeService.list(new QueryWrapper<ProcessDefinitionEdge>().eq("process_definition_id", processDefinitionId).eq("source_id", taskId).likeLeft("target_id", "ExclusiveGateway"));
        if (ObjectUtil.isNotEmpty(exclusiveGatewayTmp)) {
            if (exclusiveGatewayTmp.size() == 1) {
                //排他网关的连线的edge
                List<ProcessDefinitionEdge> exclusiveGatewayList = processDefinitionEdgeService.list(new QueryWrapper<ProcessDefinitionEdge>().eq("process_definition_id", processDefinitionId).eq("source_id", exclusiveGatewayTmp.get(0).getSourceId()));
                if (ObjectUtil.isNotEmpty(exclusiveGatewayList)) {
                    list = exclusiveGatewayList;
                }
            } else {
                throw new RuntimeException("getExclusiveGatewayCondition-流程图错误");
            }
        }
        return list;
    }

    //获取上一个节点和当前运行节点的连线关系
    public ProcessDefinitionEdge getPreCurrentTaskEdge(Integer processDefinitionId, String preTaskId, String currentTaskId) {
        return processDefinitionEdgeService.getOne(new QueryWrapper<ProcessDefinitionEdge>().eq("process_definition_id", processDefinitionId).eq("source_id", preTaskId).eq("target_id", currentTaskId).eq("edge_direction", "退回"));
    }
}
