package com.sss.yunweiadmin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sss.yunweiadmin.mapper.SysRoleMapper;
import com.sss.yunweiadmin.model.entity.SysRole;
import com.sss.yunweiadmin.model.entity.SysRolePermission;
import com.sss.yunweiadmin.service.SysPermissionService;
import com.sss.yunweiadmin.service.SysRolePermissionService;
import com.sss.yunweiadmin.service.SysRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 角色表 服务实现类
 * </p>
 *
 * @author 任勇林
 * @since 2021-04-09
 */
@Service
public class SysRoleServiceImpl extends ServiceImpl<SysRoleMapper, SysRole> implements SysRoleService {
    @Autowired
    SysRolePermissionService sysRolePermissionService;
    @Autowired
    SysPermissionService sysPermissionService;

    @Override
    public boolean updateRolePermission(Integer roleId, List<Integer> permissionIdList) {
        boolean flag;
        //先删除，后插入
        sysRolePermissionService.remove(new QueryWrapper<SysRolePermission>().eq("role_id", roleId));
        //
        List<SysRolePermission> list = permissionIdList.stream().map(permissionId -> {
            SysRolePermission rolePermission = new SysRolePermission();
            rolePermission.setRoleId(roleId);
            rolePermission.setPermissionId(permissionId);
            return rolePermission;
        }).collect(Collectors.toList());
        flag = sysRolePermissionService.saveBatch(list);
        return flag;
    }
}
