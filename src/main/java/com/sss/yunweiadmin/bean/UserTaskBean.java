package com.sss.yunweiadmin.bean;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sss.yunweiadmin.common.config.GlobalParam;
import com.sss.yunweiadmin.model.entity.*;
import com.sss.yunweiadmin.model.vo.AssiginTaskAndUserVO;
import com.sss.yunweiadmin.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class UserTaskBean {
    @Autowired
    ProcessDefinitionTaskService processDefinitionTaskService;
    @Autowired
    ProcessDefinitionEdgeService processDefinitionEdgeService;
    @Autowired
    HttpSession httpSession;
    @Autowired
    SysUserService sysUserService;
    @Autowired
    SysRoleUserService sysRoleUserService;
    @Autowired
    ProcessInstanceNodeService processInstanceNodeService;
    @Autowired
    ProcessInstanceDataService processInstanceDataService;
    @Autowired
    SysDeptService sysDeptService;
    //workFlowBean里调用他，实参也得加--
    //20250220 此函数现在不需要了 ，待删除 202408230：在proceessData表里记录下一步记录人时会调用这个
    public List<SysUser> getUserList(Integer processDefinitionId, String preTaskDefKey, String currentTaskDefKey,Integer processInstanceDataId) {
        ProcessDefinitionTask preTask = processDefinitionTaskService.getOne(new  QueryWrapper<ProcessDefinitionTask>().eq("org_id",GlobalParam.orgId).eq("process_definition_id", processDefinitionId).eq("task_def_key", preTaskDefKey));
        ProcessDefinitionTask currentTask = processDefinitionTaskService.getOne(new  QueryWrapper<ProcessDefinitionTask>().eq("org_id",GlobalParam.orgId).eq("process_definition_id", processDefinitionId).eq("task_def_key", currentTaskDefKey));
//        if (preTask.getHaveNextUser().equals("是")) {
//            AssiginTaskAndUserVO assiginTaskAndUserVO = (AssiginTaskAndUserVO) httpSession.getAttribute("assiginTaskAndUserVO");
//            return getUserList(assiginTaskAndUserVO.getOperatorType(), assiginTaskAndUserVO.getOperatorTypeIds(), assiginTaskAndUserVO.getHaveStarterDept(),processInstanceDataId);
//        } else {
//            return getUserList(currentTask.getOperatorType(), currentTask.getOperatorTypeIds(), currentTask.getHaveStarterDept(),processInstanceDataId);
//        }

        //历史处理节点
        ProcessInstanceNode processInstanceNode = null;
        ProcessInstanceData processInstanceData = processInstanceDataService.getById(processInstanceDataId);
        //20211214 processInstanceData可能=null：先创建task,后创建data/node记录：流程启动时会接连创建两个activiTask
        // 在发起activi节点创建时（流程启动的第一个activi节点创建），data/node里还没有记录；在创建第二个activi节点时，才创建data、node ，
        List<ProcessInstanceNode> list = null;
        String assiginUserId = null;
        List<SysUser> userList = null;
        ProcessDefinitionTask processDefinitionTask = processDefinitionTaskService.getOne(new  QueryWrapper<ProcessDefinitionTask>().eq("org_id",GlobalParam.orgId).eq("process_definition_id", processDefinitionId).eq("task_def_key", currentTaskDefKey));

        if (processInstanceData != null) {
            list = processInstanceNodeService.list(new  QueryWrapper<ProcessInstanceNode>().eq("org_id",GlobalParam.orgId).eq("process_instance_data_id", processInstanceData.getId()).eq("task_def_key", currentTaskDefKey));
            if (CollUtil.isNotEmpty(list))
                processInstanceNode = list.get(0);
            List<ProcessInstanceNode> listForFindAssignTask = processInstanceNodeService.list(new  QueryWrapper<ProcessInstanceNode>().eq("org_id",GlobalParam.orgId).eq("process_instance_data_id", processInstanceData.getId()).orderByDesc("id"));
            if (CollUtil.isNotEmpty(listForFindAssignTask)) {
                //20240811todo断点 前提：不是首节点；查寻当前(要指定责任人的)task的“中文名”,在node记录里找到有没有被assign有的话记录下来，并且在下面的       if (assiginTaskAndUserVO != null) 后加个分支，，
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
                    userList = this.getUserList(assiginTaskAndUserVO.getOperatorType(), assiginTaskAndUserVO.getOperatorTypeIds(), assiginTaskAndUserVO.getHaveStarterDept(),processInstanceData.getId());
                    //20240813 todo断点，根据assiginUserId取出USER对象：或者重载一个 this.getUserList
                else if(ObjectUtil.isNotEmpty(assiginUserId)){
                    userList = this.getUserList("用户", assiginUserId, "",processInstanceData.getId());
                } else
                    userList = this.getUserList(processDefinitionTask.getOperatorType(), processDefinitionTask.getOperatorTypeIds(), processDefinitionTask.getHaveStarterDept(), processInstanceData.getId());
                //   userList = this.getUserList(assiginTaskAndUserVO.getOperatorType(), assiginTaskAndUserVO.getOperatorTypeIds(), assiginTaskAndUserVO.getHaveStarterDept(),processInstanceData.getId());
            }
        } else {//20211208 todo在getUserList（）添加一种情况，task表的type字段是“发起人”的情况；
            if(ObjectUtil.isNotEmpty(assiginUserId)){
                //SysUser sysUser = sysUserService.getOne(new  QueryWrapper<SysUser>().eq("org_id",GlobalParam.orgId).eq("id",assiginUserId));
                userList = this.getUserList("用户", assiginUserId, "",processInstanceData.getId());
            } else if (processInstanceNode != null) {//20240811 todo排除“刚执行完的节点”里设置“下一步节点”审批人的情况<两种类型：一种是精典的“下一步处理人”，另一种就是现在做的“指定某步处理人”>：这时需要使用“最新的设置结果”
                //存在历史节点，使用历史处理人
                SysUser sysUser = sysUserService.getOne(new  QueryWrapper<SysUser>().eq("org_id",GlobalParam.orgId).eq("login_name",processInstanceNode.getLoginName()));
                //taskEntity.addCandidateUser(processInstanceNode.getLoginName());
                userList = this.getUserList("用户", sysUser.getId().toString(), "",processInstanceData.getId());
            } else
                userList = this.getUserList(processDefinitionTask.getOperatorType(), processDefinitionTask.getOperatorTypeIds(), processDefinitionTask.getHaveStarterDept(), processInstanceData.getId());
//                        if (ObjectUtil.isNotEmpty(processInstanceData)) {
//                            userList = this.getUserList(processDefinitionTask.getOperatorType(), processDefinitionTask.getOperatorTypeIds(), processDefinitionTask.getHaveStarterDept(), processInstanceData.getId());
//                        } else//处理发起节点的下个节点的情况（用户发起流程动作时：会在创建第二个activ节点后才创建NODE，所以此时data表依旧为空），此时processInstanceData里还是null,下面相应参数给个”占位符“
//                           //20240827 第一个节点|发起节点不会进这个分支了：所以这个分支可考虑删除
//                            userList = this.getUserList(processDefinitionTask.getOperatorType(), processDefinitionTask.getOperatorTypeIds(), processDefinitionTask.getHaveStarterDept(), 0);
        }
        return userList;
    }


    //listener里调用他，实参要加
    public List<SysUser> getUserList(String operatorType, String operatorTypeIds, String haveStarterDept,Integer processInstanceDataId) {
        List<SysUser> userList = null;
        if (operatorType.equals("角色")) {
            List<SysRoleUser> roleUserList = new ArrayList<>();
            if (ObjectUtil.isNotEmpty(haveStarterDept)) {
                /*
                    提交人部门
                    根据提交人获取部门所有用户，接着查询出所有用户的角色，最后根据页面角色，帅选出有效用户
                 */
                //20230516对于非“processInstanceDataId=0”（“发起节点的第二个节点的的处理<：详细描述见actEventListner>”之外的情况）改从流程实例表中读部门 ;     //
                SysDept dept = null;
                if(processInstanceDataId == 0){
                    SysUser currentUser = (SysUser) httpSession.getAttribute("user");
                    dept = sysDeptService.getById(currentUser.getDeptId());
                }
                else{
                    ProcessInstanceData processInstanceData = processInstanceDataService.getById(processInstanceDataId);
                    dept = sysDeptService.getOne(new  QueryWrapper<SysDept>().eq("org_id",GlobalParam.orgId).eq("name",processInstanceData.getDeptName()));
                }
                //20250616 仅为惯性公司“五室涿州分部 在走 定密流程时 定密责任人是 五室正职 而设计”
                Integer deptId = dept.getId();
                if(GlobalParam.orgId == 1 && deptId == GlobalParam.deptIDForSp5 &&  Arrays.asList(operatorTypeIds.split(",")).contains(String.valueOf(GlobalParam.roleIdForDeptCheifManager))){//定密责任人,目前等同于“部门正职领导”
                    deptId = GlobalParam.deptIDForS5;
                }

                List<SysUser> userTmp = sysUserService.list(new  QueryWrapper<SysUser>().eq("org_id", GlobalParam.orgId).eq("dept_id", deptId).eq("status","正常"));//20240228加了status限制
                roleUserList = sysRoleUserService.list(new  QueryWrapper<SysRoleUser>().eq("org_id",GlobalParam.orgId).in("role_id", Arrays.asList(operatorTypeIds.split(","))).in("user_id", userTmp.stream().map(SysUser::getId).collect(Collectors.toList())));
               // userList = sysUserService.listByIds(roleUserList.stream().map(SysRoleUser::getUserId).collect(Collectors.toList()));
            } else {
                roleUserList = sysRoleUserService.list(new  QueryWrapper<SysRoleUser>().eq("org_id",GlobalParam.orgId).in("role_id", Arrays.asList(operatorTypeIds.split(","))));
                //userList = sysUserService.listByIds(roleUserList.stream().map(SysRoleUser::getUserId).collect(Collectors.toList()));
            }
            List<Integer> idList = roleUserList.stream().map(SysRoleUser::getUserId).collect(Collectors.toList());
            if(CollUtil.isNotEmpty(idList))
                userList = sysUserService.listByIds(roleUserList.stream().map(SysRoleUser::getUserId).collect(Collectors.toList()));
            else
                throw new RuntimeException("该节点处理人员不存在！");
        } else if (operatorType.equals("用户")) {
           // List<SysRoleUser> roleUserList = sysRoleUserService.list(new  QueryWrapper<SysRoleUser>().eq("org_id",GlobalParam.orgId).in("user_id", Arrays.asList(operatorTypeIds.split(","))));
          //20220528改造，直接利用ID
            userList = sysUserService.listByIds( Arrays.asList(operatorTypeIds.split(",")));
            // userList = sysUserService.listByIds(roleUserList.stream().map(SysRoleUser::getUserId).collect(Collectors.toList()));
        } else {// 20211208最后一种情况：task表的type字段是“发起人”的情况；实际上在调用本方法的主方法里都已经查过一遍nodeList,从性能角度看，后续可考虑这分支上移到父方法中
            //todo20211210 这里报错了  20211213todo判断：如果nodelist为空（指的是发起结点的下一个节点就是处理人是发起人，这样发起结点还没在
            // node里记录，自然也查不到），就取处理人为当前Session的
            List<ProcessInstanceNode> nodeList  =  processInstanceNodeService.list(new  QueryWrapper<ProcessInstanceNode>().eq("org_id",GlobalParam.orgId).eq("process_instance_data_id",processInstanceDataId).orderByAsc("id"));
            if((ObjectUtil.isNotEmpty(nodeList))){
                String startNodeLoginName  = nodeList.get(0).getLoginName();
                //组装成sysUserList：虽然只有一个值
                userList = sysUserService.list(new  QueryWrapper<SysUser>().eq("org_id",GlobalParam.orgId).eq("login_name",startNodeLoginName));}
            else{//发起结点的下一个节点就是处理人是发起人 todo 测试
                SysUser currentUser = (SysUser) httpSession.getAttribute("user");
                userList = new ArrayList<>();
                userList.add(currentUser);
            }
        }
        return userList;
    }
}
