package com.sss.yunweiadmin.bean;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sss.yunweiadmin.common.config.GlobalParam;
import com.sss.yunweiadmin.model.entity.ProcessDefinitionEdge;
import com.sss.yunweiadmin.model.entity.ProcessDefinitionTask;
import com.sss.yunweiadmin.model.entity.ProcessInstanceNode;
import com.sss.yunweiadmin.model.entity.SysUser;
import com.sss.yunweiadmin.service.*;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.FlowNode;
import org.activiti.bpmn.model.Process;
import org.activiti.engine.*;
import org.activiti.engine.history.HistoricTaskInstance;
import org.activiti.engine.impl.persistence.entity.ExecutionEntity;
import org.activiti.engine.impl.persistence.entity.IdentityInfoEntity;
import org.activiti.engine.impl.persistence.entity.TaskEntity;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.runtime.Execution;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.IdentityLink;
import org.activiti.engine.task.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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
    @Autowired
    ProcessDefinitionService processDefinitionService;
    @Autowired
    SysUserService sysUserService;
    @Autowired
    IdentityService identityService;
    @Autowired
    ManagementService managementService;

    public List<Task> getRunTaskList(String actProcessInstanceId) {
        return taskService.createTaskQuery().processInstanceId(actProcessInstanceId).active().list();
    }

    //自由跳转1
    @Transactional(noRollbackFor = Exception.class)
    public void jump1(Task startTask, String targetTaskKey) {
        // 获取流程定义
        Process process = repositoryService.getBpmnModel(startTask.getProcessDefinitionId()).getMainProcess();
        // 获取目标节点定义
        FlowNode targetNode = (FlowNode) process.getFlowElement(targetTaskKey);
        // 删除当前运行任务
        String executionEntityId = managementService.executeCommand(new DeleteTaskCmd(startTask.getId()));
        //删除其他运行任务
        List<Task> runTaskList = this.getRunTaskList(startTask.getProcessInstanceId());
        for (Task task : runTaskList) {
            if (!task.getId().equals(startTask.getId())) {
                managementService.executeCommand(new DeleteTaskCmd(task.getId()));
            }
        }
        // 流程执行到来源节点
        managementService.executeCommand(new SetFLowNodeAndGoCmd(targetNode, executionEntityId));
    }

    //自由跳转11
    @Transactional(noRollbackFor = Exception.class)
    public void jump11(String curTaskId, String targetTaskKey) {
        // 当前任务
        Task currentTask = taskService.createTaskQuery().taskId(curTaskId).singleResult();
        // 获取流程定义
        Process process = repositoryService.getBpmnModel(currentTask.getProcessDefinitionId()).getMainProcess();
        // 获取目标节点定义
        FlowNode targetNode = (FlowNode) process.getFlowElement(targetTaskKey);
        // 删除当前运行任务
        String executionEntityId = managementService.executeCommand(new DeleteTaskCmd(currentTask.getId()));
        // 流程执行到来源节点
        managementService.executeCommand(new SetFLowNodeAndGoCmd(targetNode, executionEntityId));
    }

    //自由跳转22
    @Transactional(noRollbackFor = Exception.class)
    public void jump22(String curTaskId, String targetKey) {
        managementService.executeCommand(new Jump2TargetFlowNodeCommand(curTaskId, targetKey));
    }

    //20230715 通过url手工修改某个任务的候选执行人：目前限制当前流程实例只有一个活动结点的情况
    public void addCandidateByUrl(String actProcessInstanceId, String loginName){
        List<Task> taskList = getActiveTask(actProcessInstanceId);
        if (CollUtil.isEmpty(taskList)) {
            throw new RuntimeException("任务不存在或已被其他人处理，请尝试刷新待办列表");
        }
        Task task = taskList.get(0);
        taskService.addCandidateUser(task.getId(),loginName);
      //  taskEntity.addCandidateUser(loginName);//（自己通过taskId手工生成）taskEntity的这个方法执行时会报错 VS actEventListner中这个方法就可以运行：暂不研
    }

    public void setProVarList(Task actTask,Map map){
        taskService.setVariables(actTask.getId(), map);
    }

    public List<String> getProVarListForExGateway(Integer processDefinitionId){
        List<String> a = null;
        List<ProcessDefinitionEdge> edgeList = processDefinitionEdgeService.list(new  QueryWrapper<ProcessDefinitionEdge>().eq("org_id", GlobalParam.orgId).eq("process_definition_id",processDefinitionId).like("source_id","ExclusiveGateway").ne("var_name",""));
        if(CollUtil.isNotEmpty(edgeList)){
            return edgeList.stream().map(item->item.getVarName()).distinct().collect(Collectors.toList());
        }
        return null;
    }

    public Object getProcessVariable(Task task){
       return taskService.getVariable(task.getId(),"newProcessName");
    }
    public Deployment deploy(String actProcessName, String activitiXml) {
        System.out.println(activitiXml);
        return repositoryService.createDeployment().name(actProcessName).addString(actProcessName + ".bpmn", activitiXml).deploy();
    }

    //级联删除流程部署
    public void deleteDeploy(String deployId) {
        repositoryService.deleteDeployment(deployId, true);
    }

    //删除流程实例
    public void deleteProcessInstance(String actProcessInstanceId) {
        if (isFinish(actProcessInstanceId)) {
            historyService.deleteHistoricProcessInstance(actProcessInstanceId);
        } else {
            //删除顺序不能换
            runtimeService.deleteProcessInstance(actProcessInstanceId, "删除原因");
            historyService.deleteHistoricProcessInstance(actProcessInstanceId);
        }
    }

    //仅删除流程实例的当前结点信息，历史信息暂不删除（否则handel过程中会报错）
    public void deleteProcessInstance2(String actProcessInstanceId) {
        if (isFinish(actProcessInstanceId)) {
            historyService.deleteHistoricProcessInstance(actProcessInstanceId);
        } else {
            //删除顺序不能换
            runtimeService.deleteProcessInstance(actProcessInstanceId, "删除原因");

        }
    }

    public ProcessInstance startProcessInstance(String actProcessName, Integer businessId) {
//        Map<String, Object> map = new HashMap<>();//20220625测流程变量
//        map.put("aa", 100);
//        map.put("bb", "");
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
//20230707此方法仅做测试用
public List<HistoricTaskInstance> getHistoricTaskInstance1(String actProcessInstanceId) {

    List<HistoricTaskInstance> list =historyService.createHistoricTaskInstanceQuery()
            .processInstanceId(actProcessInstanceId)
            .orderByHistoricTaskInstanceEndTime()
            .desc()//正在运行的节点因为endTime为null ，反而 排在list的最后一个（index最大createProcessInstanceBuilder）
            .list();
    HistoricTaskInstance previousTask =list.get(list.size() - 2);
    BpmnModel bpmnModel = repositoryService.getBpmnModel(previousTask.getProcessDefinitionId());
    FlowNode previousFlowNode = (FlowNode) bpmnModel.getFlowElement(previousTask.getTaskDefinitionKey());
    Task newTask = taskService.newTask();
    newTask.setName(previousFlowNode.getName());
    newTask.setAssignee(previousTask.getAssignee());
   // newTask.setTaskDefinitionKey(actProcessInstanceId);//此方法不存在
    taskService.saveTask(newTask);

    return  list;

}

    public List<Task> getActiveTask(String actProcessInstanceId) {
        return taskService.createTaskQuery().processInstanceId(actProcessInstanceId).active().list();
    }

    public List<Task> getMyTask(String actProcessInstanceId) {
        SysUser user = (SysUser) httpSession.getAttribute("user");
        return taskService.createTaskQuery().processInstanceId(actProcessInstanceId).taskCandidateOrAssigned(user.getLoginName()).active().list();
    }


    public Map<String, String> getCurrentStep(Integer processDefinitionId, Integer processInstanceDataId, String actProcessInstanceId, String preTaskDefKey) {
        Integer orgId =  GlobalParam.orgId;
        Integer crossOrgId = (Integer) httpSession.getAttribute("crossOrgId");
        if(ObjectUtil.isNotEmpty(crossOrgId))
            orgId = crossOrgId;
        Map<String, String> resultMap = new HashMap<>();
        if (!this.isFinish(actProcessInstanceId)) {
            //显示名称
            List<String> displayList = new ArrayList<>();
            //登录名称
            List<String> loginList = new ArrayList<>();
            //任务节点类型
            String taskType = null;
            List<ProcessDefinitionTask> taskDefList = processDefinitionTaskService.list(new  QueryWrapper<ProcessDefinitionTask>().eq("org_id",orgId).eq("process_definition_id",processDefinitionId).ne("task_name",""));
            Map<String, String> taskMap = taskDefList.stream().collect(Collectors.toMap(ProcessDefinitionTask::getTaskDefKey, v -> v.getTaskType(), (key1, key2) -> key2));
            //历史处理节点
            List<ProcessInstanceNode> list = processInstanceNodeService.list(new  QueryWrapper<ProcessInstanceNode>().eq("org_id",orgId).eq("process_instance_data_id", processInstanceDataId));
            Map<String, ProcessInstanceNode> map = list.stream().collect(Collectors.toMap(ProcessInstanceNode::getTaskDefKey, v -> v, (key1, key2) -> key2));
            //获取当前活动任务
            List<Task> taskList = this.getActiveTask(actProcessInstanceId);

            for (Task task : taskList) {
                ProcessInstanceNode processInstanceNode = map.get(task.getTaskDefinitionKey());
                taskType = taskMap.get(task.getTaskDefinitionKey());
                List<IdentityLink> list1 = taskService.getIdentityLinksForTask(task.getId());//每个候选人是一个元素：IdentityLinkEntity[id=407519, type=candidate, userId=yangyan, taskId=407517]，
//                    IdentityLink identityLink = list1.get(0);
//                    String candidateUserId = identityLink.getUserId();
                if(CollUtil.isEmpty(list1))//20250730 注意这里的throw并不到让“调用它的service函数回滚（仅在Debug模式下可以）：需要额外在service方法里try/catch”;
                    throw new RuntimeException("流程下一步处理人员为空，请流程管理员检查!");
                List<SysUser> userList = sysUserService.list(new  QueryWrapper<SysUser>().eq("org_id",orgId).in("login_name",list1.stream().map( IdentityLink::getUserId).collect(Collectors.toList())));

                List<String> displayNameList = userList.stream().map(SysUser::getDisplayName).collect(Collectors.toList());
                List<String> loginNameList = userList.stream().map(SysUser::getLoginName).collect(Collectors.toList());
                displayList.add(task.getName() + "[" + String.join(",", displayNameList) + "]");
                loginList.add(String.join(",", loginNameList));
                //}
            }
            resultMap.put("displayName", String.join(",", displayList));
            resultMap.put("loginName", String.join(",", loginList));
            resultMap.put("taskType", taskType);
        }
        return resultMap;
    }

    //该节点有多条连线，即多个提交按钮 ;20220628这个名得改
    public void completeTaskByParam(Integer processDefinitionId, Task actTask, String buttonName, String selectedProcess) {
        SysUser currentUser = (SysUser) httpSession.getAttribute("user");
        //拾取任务
        taskService.claim(actTask.getId(), currentUser.getLoginName());
        //设置buttonName条件和排他网关条件
        Map<String, Object> map = new HashMap<>();
        if (ObjectUtil.isNotEmpty(buttonName))
            map.put(actTask.getTaskDefinitionKey(), buttonName);//对应流程变量：两个参数分别为变量名/值
        //判断任务的下一个节点（可能是多个）有没有排他网关：下面这段不需要：因为暂“约定”：用户节点后面不能同时有排他网关 & 普通分支（这种情况下直接合并到排他网关即可）
//        String taskDefKey = actTask.getTaskDefinitionKey();
//        List<ProcessDefinitionEdge> exclusiveGatewayList = getExclusiveGatewayCondition(processDefinitionId, taskDefKey);
//        if (ObjectUtil.isNotEmpty(exclusiveGatewayList)) {
//            Set<String> varNameSet = exclusiveGatewayList.stream().map(ProcessDefinitionEdge::getVarName).collect(Collectors.toSet());
//            //设置排他网关条件，自由发挥
//            map.put("aa", 100);
//        }
       if (selectedProcess != null) {//在开始出现选择待办流程的环节之前的节点表单（不含退回时）（因没有渲染对应f）
            map.put("newProcessName", selectedProcess);
        }
        //完成任务
       // map.put("haveReInstall","否");
        //this.jump1(actTask,"Task_0pk11tu");//20250313 用于跳转任意节点
        taskService.complete(actTask.getId(), map);
    }
//20220628加
//    public void completeTaskBySelectedProcess(Integer processDefinitionId, Task actTask, String selectedProcess) {
//        SysUser currentUser = (SysUser) httpSession.getAttribute("user");
//        //拾取任务
//        taskService.claim(actTask.getId(), currentUser.getLoginName());
//        //设置buttonName条件和排他网关条件
//        Map<String, Object> map = new HashMap<>();
//        map.put("newProcessName", selectedProcess);//
//        //完成任务
//        taskService.complete(actTask.getId(), map);
//
//    }


    public void completeTask(Integer processDefinitionId, Task actTask) {
        SysUser currentUser = (SysUser) httpSession.getAttribute("user");
        //拾取任务
        taskService.claim(actTask.getId(), currentUser.getLoginName());
        //判断任务的下一个节点有没有排他网关
        String taskDefKey = actTask.getTaskDefinitionKey();
        List<ProcessDefinitionEdge> exclusiveGatewayList = getExclusiveGatewayCondition(processDefinitionId, taskDefKey);
        if (ObjectUtil.isNotEmpty(exclusiveGatewayList)) {
            Set<String> varNameSet = exclusiveGatewayList.stream().map(ProcessDefinitionEdge::getVarName).collect(Collectors.toSet());
            //设置排他网关条件，自由发挥
            Map<String, Object> map = new HashMap<>();
            map.put("aa", 100);
            //完成任务
            //this.jump1(actTask,"Task_0pk11tu");//20250313 用于跳转任意节点
           taskService.complete(actTask.getId(), map);
        } else {
            //完成任务
            // this.jump1(actTask,"Task_0pk11tu");//20250313 用于跳转任意节点
           taskService.complete(actTask.getId());
        }
    }

    public boolean isFinish(String actProcessInstanceId) {
        return runtimeService.createProcessInstanceQuery().processInstanceId(actProcessInstanceId).singleResult() == null;
    }

    public Integer getBusinessKeyByProcessInstanceId(String actProcessInstanceId) {
        ProcessInstance pi = runtimeService.createProcessInstanceQuery()
                .processInstanceId(actProcessInstanceId)
                .singleResult();
        return pi == null ? null : Integer.parseInt(pi.getBusinessKey());
    }

    //获取节点的多条连线
    public List<String> getButtonNameList(Integer processDefinitionId, String taskDefKey) {
        Integer orgId =  GlobalParam.orgId;
        Integer crossOrgId = (Integer) httpSession.getAttribute("crossOrgId");
        if(ObjectUtil.isNotEmpty(crossOrgId))
            orgId = crossOrgId;
        List<String> buttonNameList = null;
        //判断是否有多条连线
        List<ProcessDefinitionEdge> edgeList = processDefinitionEdgeService.list(new  QueryWrapper<ProcessDefinitionEdge>().eq("org_id",orgId).eq("process_definition_id", processDefinitionId).eq("source_id", taskDefKey));
        if (ObjectUtil.isNotEmpty(edgeList)) {
            List<String> list = edgeList.stream().filter(item -> ObjectUtil.isNotEmpty(item.getButtonName())).map(ProcessDefinitionEdge::getButtonName).collect(Collectors.toList());
            if (ObjectUtil.isNotEmpty(list)) {
                buttonNameList = list;
            }
        }
        return buttonNameList;
    }
    //20220629加 获取“可能的”流程代办人员：从结束节点往前找(随便取一个)直连的处理者是“提交人”的userTask:这个算法基于一定约定&&
    public ProcessDefinitionTask getFeedBackUserTask(Integer processDefId) {
        Integer orgId =  GlobalParam.orgId;
        Integer crossOrgId = (Integer) httpSession.getAttribute("crossOrgId");
        if(ObjectUtil.isNotEmpty(crossOrgId))
            orgId = crossOrgId;
        ProcessDefinitionTask endEvent = processDefinitionTaskService.getOne(new  QueryWrapper<ProcessDefinitionTask>().eq("org_id",orgId).eq("task_type","bpmn:endEvent").eq("process_definition_id",processDefId).last("limit 1"));
        List<ProcessDefinitionEdge> edgeList = processDefinitionEdgeService.list(new  QueryWrapper<ProcessDefinitionEdge>().eq("org_id",orgId).eq("target_id",endEvent.getTaskDefKey()).eq("process_definition_id",processDefId));
        List< ProcessDefinitionTask> lastUserTaskList =processDefinitionTaskService.list(new  QueryWrapper<ProcessDefinitionTask>().eq("org_id",orgId).eq("process_definition_id",processDefId).in("task_def_key",edgeList.stream().map(item->item.getSourceId()).collect(Collectors.toList())).like("task_type","Task"));
        if(lastUserTaskList.size() == 1){//限制直连结束节点只有一个userTask且处理人必须是发起人
            if(lastUserTaskList.get(0).getOperatorType().equals("发起人"))
                return lastUserTaskList.get(0);
            else if(processDefinitionService.getById(processDefId).getProcessName().contains("归库"))//20221225新增：归库时因授权发起人是安中，所以这里直接放过审核吧
                return lastUserTaskList.get(0);
        }
        return null;
        //.eq("operator_type","")
    }

    //获取节点的到下一个节点(排他网关)的连线
    public List<ProcessDefinitionEdge> getExclusiveGatewayCondition(Integer processDefinitionId, String taskDefKey) {
        Integer crossOrgId = (Integer)httpSession.getAttribute("crossOrgId");//20241108
        Integer orgId = GlobalParam.orgId;
        if(ObjUtil.isNotEmpty(crossOrgId))
            orgId = crossOrgId;
        List<ProcessDefinitionEdge> list = null;
        //排他网关的连线的id
        List<ProcessDefinitionEdge> exclusiveGatewayTmp = processDefinitionEdgeService.list(new  QueryWrapper<ProcessDefinitionEdge>().eq("org_id",orgId).eq("process_definition_id", processDefinitionId).eq("source_id", taskDefKey).likeLeft("target_id", "ExclusiveGateway"));
        if (ObjectUtil.isNotEmpty(exclusiveGatewayTmp)) {
            if (exclusiveGatewayTmp.size() == 1) {
                //排他网关的连线的edge
                List<ProcessDefinitionEdge> exclusiveGatewayList = processDefinitionEdgeService.list(new  QueryWrapper<ProcessDefinitionEdge>().eq("org_id",orgId).eq("process_definition_id", processDefinitionId).eq("source_id", exclusiveGatewayTmp.get(0).getSourceId()));
                if (ObjectUtil.isNotEmpty(exclusiveGatewayList)) {
                    list = exclusiveGatewayList;
                }
            } else {
                throw new RuntimeException("getExclusiveGatewayCondition-流程图错误");
            }
        }
        return list;
    }

    //获取上一个节点和当前运行节点的连线关系:20220624只有这一个地方用到了连线的“direction”属性：只是为了“识别’退回’的流程状态”
    public ProcessDefinitionEdge getReturnedTaskEdge(Integer processDefinitionId, String preTaskDefKey, String currentTaskDefKey) {
        return processDefinitionEdgeService.getOne(new  QueryWrapper<ProcessDefinitionEdge>().eq("org_id",GlobalParam.orgId).eq("process_definition_id", processDefinitionId).eq("source_id", preTaskDefKey).eq("target_id", currentTaskDefKey).eq("edge_direction", "退回"));
    }
}
