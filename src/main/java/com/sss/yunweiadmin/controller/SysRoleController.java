package com.sss.yunweiadmin.controller;


import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sss.yunweiadmin.common.config.GlobalParam;
import com.sss.yunweiadmin.common.operate.OperateLog;
import com.sss.yunweiadmin.common.result.ResponseResult;
import com.sss.yunweiadmin.common.result.ResponseResultWrapper;
import com.sss.yunweiadmin.common.utils.TreeUtil;
import com.sss.yunweiadmin.model.entity.*;
import com.sss.yunweiadmin.model.vo.KeyTitleVO;
import com.sss.yunweiadmin.model.vo.PermissionGiveVO;
import com.sss.yunweiadmin.model.vo.TreeSelectVO;
import com.sss.yunweiadmin.model.vo.ValueLabelVO;
import com.sss.yunweiadmin.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.sql.Array;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * <p>
 * 角色表 前端控制器
 * </p>
 *
 * @author 任勇林
 * @since 2021-04-09
 */
@RestController
@RequestMapping("/sysRole")
@ResponseResultWrapper
public class SysRoleController {
    @Autowired
    SysRoleService sysRoleService;
    @Autowired
    SysPermissionService sysPermissionService;
    @Autowired
    SysRolePermissionService sysRolePermissionService;
    @Autowired
    SysRoleUserService sysRoleUserService;
    @Autowired
    HttpSession httpSession;
    @Autowired
    OperateeLogService operateeLogService;

    //20231205 专用于手工记日志：用户相关
    void saveLog(String mode, String roleInfo){
        SysUser user = (SysUser) httpSession.getAttribute("user");
        OperateeLog operateeLog = new OperateeLog();
        String paramStr = "";
        if("edit".equals(mode)){
            paramStr =  "编辑角色【"+ roleInfo + "】信息";
            operateeLog.setOperateType("编辑角色");
            operateeLog.setMethod("com.sss.yunweiadmin.controller.edit()");
        } else if("delete".equals(mode)){
            paramStr =  "删除角色【"+ roleInfo + "】信息";
            operateeLog.setOperateType("删除角色");
            operateeLog.setMethod("com.sss.yunweiadmin.controller.delete()");
        } else if("add".equals(mode)){
            operateeLog.setMethod("com.sss.yunweiadmin.controller.add()");
            paramStr =  "新增角色【"+ roleInfo + "】信息";
            operateeLog.setOperateType("新增角色");
        } else if("grant".equals(mode)){
            operateeLog.setMethod("com.sss.yunweiadmin.controller.grant()");
            paramStr =  "角色权限分配【"+ roleInfo.split(",")[0] + "】:" + roleInfo ;
            operateeLog.setOperateType("角色权限分配");
        }

//        if(CollUtil.isNotEmpty(roleNameList)) {
//            paramStr = paramStr +  String.join(",", roleNameList);
//        }
        operateeLog.setParam(paramStr);
        operateeLog.setOperateModule("角色模块");

        operateeLog.setLoginName(user.getLoginName());
        operateeLog.setDisplayName(user.getDisplayName());
        //operateeLog.setIp(ServletUtil.getClientIP(request));
        String ip = (String) httpSession.getAttribute("IP");
        operateeLog.setIp(ip);

        operateeLog.setCreateDatetime(LocalDateTime.now());
        operateeLogService.save(operateeLog);

    }

    @GetMapping("list")
    public IPage<SysRole> list(int currentPage, int pageSize) {
        return sysRoleService.page(new Page<>(currentPage, pageSize), new  QueryWrapper<SysRole>().eq("org_id",GlobalParam.orgId).ne("name", "提交人领导").notIn("name",  Arrays.asList(new String[]{"系统管理员", "安全管理员", "系统审计员"})));
    }

    @PostMapping("add")
    public boolean add(@RequestBody SysRole sysRole) {
        saveLog("add",sysRole.getName());
        return sysRoleService.save(sysRole);
    }

    @GetMapping("get")
    public SysRole getById(String id) {
        return sysRoleService.getById(id);
    }

//    @OperateLog(module = "角色模块", type = "编辑角色")
    @PostMapping("edit")
    public boolean edit(@RequestBody SysRole sysRole) {
        saveLog("edit",sysRole.getName());
        return sysRoleService.updateById(sysRole);
    }
//    @OperateLog(module = "角色模块", type = "删除角色")
    @GetMapping("delete")
    public boolean delete(Integer[] idArr) {
        List<Map<String, Object>> listMaps1 =  sysRoleService.listMaps(new  QueryWrapper<SysRole>().eq("org_id", GlobalParam.orgId).in("id", Arrays.asList(idArr)).select("name"));
        List<String> roleNameList = listMaps1.stream().map(item -> item.get("name").toString()).collect(Collectors.toList());
        saveLog("delete", String.join(",", roleNameList));
        return sysRoleService.removeByIds(Arrays.asList(idArr));
    }

    @GetMapping("getRoleVL")
    public List<ValueLabelVO> getRoleVL() {
        List<SysRole> list = sysRoleService.list(new  QueryWrapper<SysRole>().eq("org_id",GlobalParam.orgId));
        return list.stream().map(item -> new ValueLabelVO(item.getId(), item.getName())).collect(Collectors.toList());
    }

    @GetMapping("getRoleNameVL")
    public List<ValueLabelVO> getRoleNameVL() {
        List<SysRole> list = sysRoleService.list(new  QueryWrapper<SysRole>().eq("org_id",GlobalParam.orgId));
        return list.stream().map(item -> new ValueLabelVO(item.getName(), item.getName())).collect(Collectors.toList());
    }

    //20211114新增
    @GetMapping("getRoleIdVL")
    public List<ValueLabelVO> getRoleIdVL() {
        List<SysRole> list = sysRoleService.list(new  QueryWrapper<SysRole>().eq("org_id",GlobalParam.orgId));
        return list.stream().map(item -> new ValueLabelVO(item.getId().toString(), item.getName())).collect(Collectors.toList());

    }

    @GetMapping("getRoleKT")
    public List<KeyTitleVO> getRoleKT() {
        List<SysRole> list = sysRoleService.list(new  QueryWrapper<SysRole>().eq("org_id",GlobalParam.orgId));
        return list.stream().map(item -> new KeyTitleVO(item.getId(), item.getName())).collect(Collectors.toList());
    }

    @GetMapping("getRoleNameStr")
    public ResponseResult getRoleNameStr(Integer[] idArr) {
        List<Integer> idList = Stream.of(idArr).collect(Collectors.toList());
        List<SysRole> list = sysRoleService.list(new  QueryWrapper<SysRole>().eq("org_id",GlobalParam.orgId).in("id", idList));
        List<SysRoleUser> roleUserList = sysRoleUserService.list(new  QueryWrapper<SysRoleUser>().eq("org_id",GlobalParam.orgId).in("role_id", list.stream().map(SysRole::getId).collect(Collectors.toList())));
        Set<Integer> roleIdSet = roleUserList.stream().map(SysRoleUser::getRoleId).collect(Collectors.toSet());
        if (list.size() != roleIdSet.size()) {
            throw new RuntimeException("所选角色没有绑定用户");
        }
        String roleName = list.stream().map(SysRole::getName).collect(Collectors.joining(","));
        return ResponseResult.success(roleName);
    }

    //过滤父节点
    public List<Integer> parentFilter(List<Integer> list) {

        List<Integer> list2 = list.stream().map(item -> item + 1 - 1).collect(Collectors.toList());

        for (int cur : list) {
            //所有的元素都有PID，顶级菜单的PID是0
            System.out.println("cur:" + cur);
            Integer pId = sysPermissionService.getById(cur).getPid();
            System.out.println("pid:" + pId);
            Iterator<Integer> iterator2 = list2.iterator();
            while (iterator2.hasNext()) {
                Integer next = iterator2.next();
                if (next.equals(pId)) {
                    iterator2.remove();
                }
            };

        }
        ;
        return list2;

    }


    //反显-权限分配
    //20211118因tree显示时父节点选中就会默认把子结点全选中，故要过滤掉父节点
    @GetMapping("getPermissionGiveVO")
    public PermissionGiveVO getPermissionGiveVO(Integer roleId) {
        //20231130测评加.ne("remark","不可见")
        List<SysPermission> list = sysPermissionService.list(new  QueryWrapper<SysPermission>().eq("org_id",GlobalParam.orgId).ne("remark","不可见").orderByAsc("sort"));
        List<TreeSelectVO> permissionList = TreeUtil.getTreeSelectVO(list);
        List<Integer> checkPermissionIdList = sysRolePermissionService.list(new  QueryWrapper<SysRolePermission>().eq("org_id",GlobalParam.orgId).eq("role_id", roleId)).stream().map(SysRolePermission::getPermissionId).collect(Collectors.toList());
        System.out.println(checkPermissionIdList);
        List<Integer> checkPermissionIdList2 = this.parentFilter(checkPermissionIdList);
        // System.out.println(111);
        System.out.println(checkPermissionIdList);
        System.out.println(checkPermissionIdList2);
        PermissionGiveVO permissionGiveVO = new PermissionGiveVO();
        permissionGiveVO.setPermissionList(permissionList);
        permissionGiveVO.setCheckPermissionIdList(checkPermissionIdList2);
        // permissionGiveVO.setCheckPermissionIdList(checkPermissionIdList2.stream().map(item-> String.valueOf(item)).collect(Collectors.toList()));
        return permissionGiveVO;
    }

    //权限分配
//    @OperateLog(module = "角色模块", type = "角色权限分配")
    @GetMapping("permissionGive")
    public boolean permissionGive(Integer roleId, Integer[] permissionIdArr) {
        List<Integer> permissionIdList = Stream.of(permissionIdArr).collect(Collectors.toList());

        List<Map<String, Object>> listMaps1 =  sysPermissionService.listMaps(new  QueryWrapper<SysPermission>().eq("org_id",GlobalParam.orgId).in("id", permissionIdList).select("name"));
        List<String> permissionNameList = listMaps1.stream().map(item -> item.get("name").toString()).collect(Collectors.toList());
        SysRole sysRole = sysRoleService.getById(roleId);
        saveLog("grant", sysRole.getName() + "," + String.join(",", permissionNameList));

        return sysRoleService.updateRolePermission(roleId, permissionIdList);
    }
}
