package com.sss.yunweiadmin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sss.yunweiadmin.model.entity.SysRole;

import java.util.List;

/**
 * <p>
 * 角色表 服务类
 * </p>
 *
 * @author 任勇林
 * @since 2021-04-09
 */
public interface SysRoleService extends IService<SysRole> {
    boolean updateRolePermission(Integer roleId, List<Integer> menuIdList);
}
