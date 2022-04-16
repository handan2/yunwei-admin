package com.sss.yunweiadmin.service;

import com.sss.yunweiadmin.model.entity.SysUser;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 * 用户表 服务类
 * </p>
 *
 * @author 任勇林
 * @since 2021-04-09
 */
public interface SysUserService extends IService<SysUser> {
    boolean add(SysUser sysUser);

    boolean delete(Integer[] idArr);

    boolean updateRoleUser(Integer userId, List<Integer> roleIdList);
}
