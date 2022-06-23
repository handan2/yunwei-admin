package com.sss.yunweiadmin.common.activiti;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sss.yunweiadmin.bean.UserTaskBean;
import com.sss.yunweiadmin.common.utils.SpringUtil;
import com.sss.yunweiadmin.model.entity.ProcessDefinitionTask;
import com.sss.yunweiadmin.model.entity.ProcessInstanceData;
import com.sss.yunweiadmin.model.entity.ProcessInstanceNode;
import com.sss.yunweiadmin.model.entity.SysUser;
import com.sss.yunweiadmin.model.vo.NextUserVO;
import com.sss.yunweiadmin.service.ProcessDefinitionTaskService;
import com.sss.yunweiadmin.service.ProcessInstanceDataService;
import com.sss.yunweiadmin.service.ProcessInstanceNodeService;
import lombok.extern.slf4j.Slf4j;
import org.activiti.engine.delegate.event.ActivitiEntityEvent;
import org.activiti.engine.delegate.event.ActivitiEvent;
import org.activiti.engine.delegate.event.ActivitiEventListener;
import org.activiti.engine.delegate.event.ActivitiEventType;
import org.activiti.engine.impl.persistence.entity.TaskEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpSession;
import java.util.List;

//给userTask设置处理人
@Component
@Slf4j
public class ActEventListener implements ActivitiEventListener {
    @Autowired
    HttpSession httpSession;
    //这里无法注入自己定义的service,所以使用了SpringUtil
    //@Autowired
    //ProcessInstanceDataService processInstanceDataService


    @Override
    public void onEvent(ActivitiEvent activitiEvent) {
        //ActivitiEventType.TASK_CREATED限定了，只有“task结点的创建”这里会处理：第一个startEvent、最后一个endEvent以及parallelGateway之类结点的创建都不属于这种情况
        if (activitiEvent.getType().equals(ActivitiEventType.TASK_CREATED)) {
            ActivitiEntityEvent entityEvent = (ActivitiEntityEvent) activitiEvent;
            TaskEntity taskEntity = (TaskEntity) entityEvent.getEntity();
            //入网流程_10:1:5003
            String actProcessDefinitionId = taskEntity.getProcessDefinitionId();
            //10
            Integer processDefinitionId = Integer.parseInt(actProcessDefinitionId.split(":")[0].split("_")[1]);
            //
            String actProcessInstanceId = taskEntity.getProcessInstanceId();
            //Task_3in0qiu
            String taskDefKey = taskEntity.getTaskDefinitionKey();
            //
            ProcessInstanceDataService processInstanceDataService = SpringUtil.getBean(ProcessInstanceDataService.class);
            ProcessInstanceNodeService processInstanceNodeService = SpringUtil.getBean(ProcessInstanceNodeService.class);
            ProcessDefinitionTaskService processDefinitionTaskService = SpringUtil.getBean(ProcessDefinitionTaskService.class);
            UserTaskBean userTaskBean = SpringUtil.getBean(UserTaskBean.class);
            //历史处理节点
            ProcessInstanceNode processInstanceNode = null;
            ProcessInstanceData processInstanceData = processInstanceDataService.getOne(new QueryWrapper<ProcessInstanceData>().eq("process_definition_id", processDefinitionId).eq("act_process_instance_id", actProcessInstanceId));
          //20211214 processInstanceData可能=null：先创建task,后创建data/node记录：流程启动时会接连创建两个activiTask
            // 在发起activi节点创建时（流程启动的第一个activi节点创建），data/node里还没有记录；在创建第二个activi节点时，才创建data、node ，
            if (processInstanceData != null) {
                List<ProcessInstanceNode> list = processInstanceNodeService.list(new QueryWrapper<ProcessInstanceNode>().eq("process_instance_data_id", processInstanceData.getId()).eq("task_def_key", taskDefKey));
                if (ObjectUtil.isNotEmpty(list)) {
                    processInstanceNode = list.get(0);
                }
            }
            //设置处理人
            if (processInstanceNode != null) {
                //存在历史节点，使用历史处理人
                taskEntity.addCandidateUser(processInstanceNode.getLoginName());
            } else {
                ProcessDefinitionTask processDefinitionTask = processDefinitionTaskService.getOne(new QueryWrapper<ProcessDefinitionTask>().eq("process_definition_id", processDefinitionId).eq("task_def_key", taskDefKey));
                //如果是发起结点，直接使用当前登陆者作为处理人，当然发起者提交流程和处理这一步是合并在一个action方法里完成的
                if (processDefinitionTask.getTaskType().equals("bpmn:startTask")) {//20211215发起结点，直接给值
                    SysUser currentUser = (SysUser) httpSession.getAttribute("user");
                    taskEntity.addCandidateUser(currentUser.getLoginName());
                } else {
                    List<SysUser> userList;
                    NextUserVO nextUserVO = (NextUserVO) httpSession.getAttribute("nextUserVO");
                   //设置了下一步处理人的情况
                    if (nextUserVO != null) {
                        userList = userTaskBean.getUserList(nextUserVO.getOperatorType(), nextUserVO.getOperatorTypeValue(), nextUserVO.getHaveStarterDept(),processInstanceData.getId());
                    } else {//20211208 todo在getUserList（）添加一种情况，task表的type字段是“发起人”的情况；
                        if (ObjectUtil.isNotEmpty(processInstanceData)) {
                            userList = userTaskBean.getUserList(processDefinitionTask.getOperatorType(), processDefinitionTask.getOperatorTypeValue(), processDefinitionTask.getHaveStarterDept(), processInstanceData.getId());
                        } else//处理发起节点的下个节点的情况（用户发起流程动作时：会在创建第二个activ节点后才创建NODE，所以此时data表依旧为空），此时processInstanceData里还是null,下面相应参数给个”占位符“
                            userList = userTaskBean.getUserList(processDefinitionTask.getOperatorType(), processDefinitionTask.getOperatorTypeValue(), processDefinitionTask.getHaveStarterDept(), 0);
                    }
                    if (ObjectUtil.isNotEmpty(userList)) {
                        userList.forEach(user -> taskEntity.addCandidateUser(user.getLoginName()));
                    } else {//20211208 张强：这里抛出异常也不会被截获。但待后续思考如果真的没处理人，流程流转时会出现什么情况
                        log.error(processDefinitionTask.getTaskDefKey() + "," + processDefinitionTask.getTaskName() + "没有处理人");
                    }
                }
            }
        }
    }

    @Override
    public boolean isFailOnException() {
        log.error("GlobalEventListener-isFailOnException处理人发生错误");
        return false;
    }
}
