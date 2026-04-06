package com.sss.yunweiadmin.common.activiti;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sss.yunweiadmin.bean.UserTaskBean;
import com.sss.yunweiadmin.common.config.GlobalParam;
import com.sss.yunweiadmin.common.utils.SpringUtil;
import com.sss.yunweiadmin.model.entity.ProcessDefinitionTask;
import com.sss.yunweiadmin.model.entity.ProcessInstanceData;
import com.sss.yunweiadmin.model.entity.ProcessInstanceNode;
import com.sss.yunweiadmin.model.entity.SysUser;
import com.sss.yunweiadmin.model.vo.AssiginTaskAndUserVO;
import com.sss.yunweiadmin.service.ProcessDefinitionTaskService;
import com.sss.yunweiadmin.service.ProcessInstanceDataService;
import com.sss.yunweiadmin.service.ProcessInstanceNodeService;
import com.sss.yunweiadmin.service.SysUserService;
import lombok.extern.slf4j.Slf4j;
import org.activiti.engine.ManagementService;
import org.activiti.engine.delegate.event.ActivitiEntityEvent;
import org.activiti.engine.delegate.event.ActivitiEvent;
import org.activiti.engine.delegate.event.ActivitiEventListener;
import org.activiti.engine.delegate.event.ActivitiEventType;
import org.activiti.engine.impl.persistence.entity.TaskEntity;
import org.apache.tomcat.jni.Proc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        Integer crossOrgId = (Integer)httpSession.getAttribute("crossOrgId");//20241108
        Integer orgId = GlobalParam.orgId;
        if(ObjUtil.isNotEmpty(crossOrgId))
            orgId = crossOrgId;

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
            SysUserService sysUserService = SpringUtil.getBean(SysUserService.class);
            UserTaskBean userTaskBean = SpringUtil.getBean(UserTaskBean.class);
            //历史处理节点
            ProcessInstanceNode processInstanceNode = null;
            ProcessInstanceData processInstanceData = processInstanceDataService.getOne(new  QueryWrapper<ProcessInstanceData>().eq("org_id",orgId).eq("process_definition_id", processDefinitionId).eq("act_process_instance_id", actProcessInstanceId));
            //20211214 processInstanceData可能=null：先创建task,后创建data/node记录：流程启动时会接连创建两个activiTask
            // 在发起activi节点创建时（流程启动的第一个activi节点创建），data/node里还没有记录；在创建第二个activi节点时，才创建data、node ，
            List<ProcessInstanceNode> list = null;
            String assiginUserId = null;
            List<SysUser> userList = null;
            ProcessDefinitionTask processDefinitionTask = processDefinitionTaskService.getOne(new  QueryWrapper<ProcessDefinitionTask>().eq("org_id",orgId).eq("process_definition_id", processDefinitionId).eq("task_def_key", taskDefKey));

            if (processInstanceData != null) {
                list = processInstanceNodeService.list(new  QueryWrapper<ProcessInstanceNode>().eq("org_id", orgId).eq("process_instance_data_id", processInstanceData.getId()).eq("task_def_key", taskDefKey));
                if (CollUtil.isNotEmpty(list))
                    processInstanceNode = list.get(0);
                //20250219 下句加了倒排.orderByDesc("id")，这样在遇到“退回”场景 && 给一个节点多次分配分理人时 就可以“用j最新的”了
                List<ProcessInstanceNode> listForFindAssignTask = processInstanceNodeService.list(new  QueryWrapper<ProcessInstanceNode>().eq("org_id",orgId).eq("process_instance_data_id", processInstanceData.getId()).orderByDesc("id"));
                if (CollUtil.isNotEmpty(listForFindAssignTask)) {
                    //20240811 前提：不是首节点；查寻当前(要指定责任人的)task的“中文名”,在node记录里找到有没有被assign有的话记录下来，并且在下面的       if (assiginTaskAndUserVO != null) 后加个分支，，
                    //由于node表有写入“延迟”，它只能影响“下一节点的下一节点”；如果是“下一节点”，还是得用（本段逻辑下面的代码机制中）session里那个vo来读取处理人
                    for(ProcessInstanceNode node: listForFindAssignTask){
                        String[] strArr = null;
                        if(ObjectUtil.isNotEmpty(node.getAssigin())){
                            strArr = node.getAssigin().split("\\|");
                            if(strArr[0].equals(processDefinitionTask.getTaskName())){
                                assiginUserId = strArr[1];
                                break;
                            }

                        }
                    }
                }
            }




            AssiginTaskAndUserVO assiginTaskAndUserVO = (AssiginTaskAndUserVO) httpSession.getAttribute("assiginTaskAndUserVO");


            //设置处理人
            //如果是发起结点，直接使用当前登陆者作为处理人，当然发起者提交流程和处理这一步是合并在一个action方法里完成的
            if (processDefinitionTask.getTaskType().equals("bpmn:startTask")) {//20211215发起结点，直接给值 20240907 todo 要区分退回时的情况
                SysUser sysUser = null;
                if (processInstanceNode != null)   //存在历史节点，使用历史处理人
                    sysUser = sysUserService.getOne(new  QueryWrapper<SysUser>().eq("org_id",orgId).eq("login_name",processInstanceNode.getLoginName()));
                else {
                    sysUser = (SysUser) httpSession.getAttribute("user");
                }
                // userTaskBean.test();
                userList = userTaskBean.getUserList("用户", sysUser.getId().toString(), "",-1);//"用户分支"不需要processInstanceDataID

            } else {//这个分支里已经是发起结点之后的结点：instData表及node表肯定有记录

                //设置了下一步处理人（及第N步处理人”）的情况：（分支到这里时）这里不包括发起节点及“历史节点”
                if (assiginTaskAndUserVO != null) {

                    if(ObjectUtil.isNotEmpty(assiginTaskAndUserVO.getAssiginTask())){
                        String assiginTask = assiginTaskAndUserVO.getAssiginTask();
                        /*当前（下一步）处理人的设置优先级：
                        1.设置了“下一步处理人”（或者assignTask节点就是下一步）：这时用界面传来的用户信息来设置
                        2.（从node表里查询）设置了当前节点的处理人
                        3.历史结点中处理人
                        4.如果assign节点设置了&& 但不是下一节点，则不在此处进行操作* */
                        if("下一节点".equals(assiginTask) || processDefinitionTask.getTaskName().equals(assiginTask))
                            //assiginTaskAndUserVO.getOperatorType()目前 似乎 都是“用户”
                            userList = userTaskBean.getUserList(assiginTaskAndUserVO.getOperatorType(), assiginTaskAndUserVO.getOperatorTypeIds(), assiginTaskAndUserVO.getHaveStarterDept(),processInstanceData.getId());
                            //20240813 todo断点，根据assiginUserId取出USER对象：或者重载一个 userTaskBean.getUserList
                        else if(ObjectUtil.isNotEmpty(assiginUserId)){
                            userList = userTaskBean.getUserList("用户", assiginUserId, "",processInstanceData.getId());
                        } else
                            userList = userTaskBean.getUserList(processDefinitionTask.getOperatorType(), processDefinitionTask.getOperatorTypeIds(), processDefinitionTask.getHaveStarterDept(), processInstanceData.getId());
                        //   userList = userTaskBean.getUserList(assiginTaskAndUserVO.getOperatorType(), assiginTaskAndUserVO.getOperatorTypeIds(), assiginTaskAndUserVO.getHaveStarterDept(),processInstanceData.getId());
                    }
                } else {//20211208 todo在getUserList（）添加一种情况，task表的type字段是“发起人”的情况；
                    if(ObjectUtil.isNotEmpty(assiginUserId)){
                        //SysUser sysUser = sysUserService.getOne(new  QueryWrapper<SysUser>().eq("org_id",orgId).eq("id",assiginUserId));
                        userList = userTaskBean.getUserList("用户", assiginUserId, "",processInstanceData.getId());
                    } else if (processInstanceNode != null) {//20240811 todo排除“刚执行完的节点”里设置“下一步节点”审批人的情况<两种类型：一种是精典的“下一步处理人”，另一种就是现在做的“指定某步处理人”>：这时需要使用“最新的设置结果”
                        //存在历史节点，使用历史处理人
                        SysUser sysUser = sysUserService.getOne(new  QueryWrapper<SysUser>().eq("org_id",orgId).eq("login_name",processInstanceNode.getLoginName()));
                        //taskEntity.addCandidateUser(processInstanceNode.getLoginName());
                        userList = userTaskBean.getUserList("用户", sysUser.getId().toString(), "",processInstanceData.getId());
                    } else
                        userList = userTaskBean.getUserList(processDefinitionTask.getOperatorType(), processDefinitionTask.getOperatorTypeIds(), processDefinitionTask.getHaveStarterDept(), processInstanceData.getId());
//                        if (ObjectUtil.isNotEmpty(processInstanceData)) {
//                            userList = userTaskBean.getUserList(processDefinitionTask.getOperatorType(), processDefinitionTask.getOperatorTypeIds(), processDefinitionTask.getHaveStarterDept(), processInstanceData.getId());
//                        } else//处理发起节点的下个节点的情况（用户发起流程动作时：会在创建第二个activ节点后才创建NODE，所以此时data表依旧为空），此时processInstanceData里还是null,下面相应参数给个”占位符“
//                           //20240827 第一个节点|发起节点不会进这个分支了：所以这个分支可考虑删除
//                            userList = userTaskBean.getUserList(processDefinitionTask.getOperatorType(), processDefinitionTask.getOperatorTypeIds(), processDefinitionTask.getHaveStarterDept(), 0);
                }

            }
            if (ObjectUtil.isNotEmpty(userList)) {
                userList.forEach(user -> taskEntity.addCandidateUser(user.getLoginName()));
                //20240830 todo断点 在这里维护一个Seesion：(processDateid:map(taskname:处理人信息))：这个session只用于流程节点处理完后node写入时使用：所以这个session就在node写入前(也可以在getCurrentStep里，这个被start|handle|startAndEnd调用)先删除，然后在这个liscenner里写入
                //20241017 这个机制目前没用 && procoessInstanceService里还有部分残留未清除；
//                Map<Integer, Map> prcIDAndHandlerMap = (Map<Integer, Map>)httpSession.getAttribute("currentStepHandler");//<1000884,<taskName,userList>>
//                Map<String, List> taskAndHandlerMap = prcIDAndHandlerMap.get(processInstanceData.getId());//"发起节点"也会执行到这里：这时processInstanceData.getId()有null指针问题 && 可能是这个listener比较特殊吧,竟然不报错还能往下运行
//                taskAndHandlerMap.put(processDefinitionTask.getTaskName(),userList);


            } else {//20211208 张强：这里抛出异常也不会被截获。但待后续思考如果真的没处理人，流程流转时会出现什么情况；20241018确认这里捕捉不到处理人不存在的异常（导致在基他步骤&&提交时因下一步处理人不存在而在其他程序位置报错 && 前台提示java错误）
                log.error(processDefinitionTask.getTaskDefKey() + "," + processDefinitionTask.getTaskName() + "没有处理人");
            }
        }
    }

    @Override
    public boolean isFailOnException() {
        log.error("GlobalEventListener-isFailOnException处理人发生错误");
        return false;
    }
}
