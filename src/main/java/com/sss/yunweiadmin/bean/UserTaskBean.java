package com.sss.yunweiadmin.bean;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sss.yunweiadmin.model.entity.ProcessDefinitionTask;
import com.sss.yunweiadmin.model.entity.ProcessInstanceNode;
import com.sss.yunweiadmin.model.entity.SysRoleUser;
import com.sss.yunweiadmin.model.entity.SysUser;
import com.sss.yunweiadmin.model.vo.NextUserVO;
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
    //workFlowBean里调用他，实参也得加
    public List<SysUser> getUserList(Integer processDefinitionId, String preTaskDefKey, String currentTaskDefKey,Integer processInstanceDataId) {
        ProcessDefinitionTask preTask = processDefinitionTaskService.getOne(new QueryWrapper<ProcessDefinitionTask>().eq("process_definition_id", processDefinitionId).eq("task_def_key", preTaskDefKey));
        ProcessDefinitionTask currentTask = processDefinitionTaskService.getOne(new QueryWrapper<ProcessDefinitionTask>().eq("process_definition_id", processDefinitionId).eq("task_def_key", currentTaskDefKey));
        if (preTask.getHaveNextUser().equals("是")) {
            NextUserVO nextUserVO = (NextUserVO) httpSession.getAttribute("nextUserVO");
            return getUserList(nextUserVO.getOperatorType(), nextUserVO.getOperatorTypeValue(), nextUserVO.getHaveStarterDept(),processInstanceDataId);
        } else {
            return getUserList(currentTask.getOperatorType(), currentTask.getOperatorTypeValue(), currentTask.getHaveStarterDept(),processInstanceDataId);
        }
    }
//todo改造：这个函数及上面那个都需要添加一个流程实例ID的参数
    //listener里调用他，实参要加
    public List<SysUser> getUserList(String operatorType, String operatorTypeValue, String haveStarterDept,Integer processInstanceDataId) {
        List<SysUser> userList;
        if (operatorType.equals("角色")) {
            if (ObjectUtil.isNotEmpty(haveStarterDept)) {
                /*
                    提交人部门
                    根据提交人获取部门所有用户，接着查询出所有用户的角色，最后根据页面角色，帅选出有效用户
                 */
                SysUser currentUser = (SysUser) httpSession.getAttribute("user");
                List<SysUser> userTmp = sysUserService.list(new QueryWrapper<SysUser>().eq("dept_id", currentUser.getDeptId()));
                List<SysRoleUser> roleUserList = sysRoleUserService.list(new QueryWrapper<SysRoleUser>().in("role_id", Arrays.asList(operatorTypeValue.split(","))).in("user_id", userTmp.stream().map(SysUser::getId).collect(Collectors.toList())));
                userList = sysUserService.listByIds(roleUserList.stream().map(SysRoleUser::getUserId).collect(Collectors.toList()));
            } else {
                List<SysRoleUser> roleUserList = sysRoleUserService.list(new QueryWrapper<SysRoleUser>().in("role_id", Arrays.asList(operatorTypeValue.split(","))));
                userList = sysUserService.listByIds(roleUserList.stream().map(SysRoleUser::getUserId).collect(Collectors.toList()));
            }
        } else if (operatorType.equals("用户")) {
           // List<SysRoleUser> roleUserList = sysRoleUserService.list(new QueryWrapper<SysRoleUser>().in("user_id", Arrays.asList(operatorTypeValue.split(","))));
          //20220528改造，直接利用ID
            userList = sysUserService.listByIds( Arrays.asList(operatorTypeValue.split(",")));
            // userList = sysUserService.listByIds(roleUserList.stream().map(SysRoleUser::getUserId).collect(Collectors.toList()));
        } else {// 20211208最后一种情况：task表的type字段是“发起人”的情况；实际上在调用本方法的主方法里都已经查过一遍nodeList,从性能角度看，后续可考虑这分支上移到父方法中
            //断点：这个函数需要多传个参数：流程实例ID，方便我从流程实例的首个node点取“发起人”
            //todo20211210 这里报错了  20211213todo判断：如果nodelist为空（指的是发起结点的下一个节点就是处理人是发起人，这样发起结点还没在
            // node里记录，自然也查不到），就取处理人为当前Session的

            List<ProcessInstanceNode> nodeList  =  processInstanceNodeService.list(new QueryWrapper<ProcessInstanceNode>().eq("process_instance_data_id",processInstanceDataId).orderByAsc("id"));
            if((ObjectUtil.isNotEmpty(nodeList))){
            String startNodeLoginName  = nodeList.get(0).getLoginName();
            //组装成sysUserList：虽然只有一个值
            userList = sysUserService.list(new QueryWrapper<SysUser>().eq("login_name",startNodeLoginName));}
            else{//发起结点的下一个节点就是处理人是发起人 todo 测试
                SysUser currentUser = (SysUser) httpSession.getAttribute("user");
                userList = new ArrayList<>();
                userList.add(currentUser);
            }
        }
        return userList;
    }
}
