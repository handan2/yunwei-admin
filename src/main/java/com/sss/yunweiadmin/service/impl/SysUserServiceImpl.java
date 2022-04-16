package com.sss.yunweiadmin.service.impl;

import cn.hutool.crypto.SecureUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sss.yunweiadmin.mapper.SysUserMapper;
import com.sss.yunweiadmin.model.entity.SysRoleUser;
import com.sss.yunweiadmin.model.entity.SysUser;
import com.sss.yunweiadmin.service.SysRoleUserService;
import com.sss.yunweiadmin.service.SysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * <p>
 * 用户表 服务实现类
 * </p>
 *
 * @author 任勇林
 * @since 2021-04-09
 */
@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {
    @Autowired
    private SysRoleUserService sysRoleUserService;

    @Override
    public boolean add(SysUser sysUser) {
        List<SysUser> list = this.list(new QueryWrapper<SysUser>().eq("login_name", sysUser.getLoginName()));
        if (list.size() > 0) {
            throw new RuntimeException(sysUser.getLoginName() + "已存在");
        }
        boolean flag1, flag2;
        sysUser.setPassword(SecureUtil.md5(sysUser.getPassword()));
        flag1 = this.save(sysUser);
        //默认为普通用户
        SysRoleUser sysRoleUser = new SysRoleUser();
        sysRoleUser.setRoleId(11);
        sysRoleUser.setUserId(sysUser.getId());
        flag2 = sysRoleUserService.save(sysRoleUser);
        return flag1 && flag2;
    }

    @Override
    public boolean delete(Integer[] idArr) {//20211115这个flag应该没啥用
        boolean flag1, flag2;
        List<Integer> userIdList = Stream.of(idArr).collect(Collectors.toList());
        flag1 = this.removeByIds(userIdList);
        flag2 = sysRoleUserService.remove(new QueryWrapper<SysRoleUser>().in("user_id", userIdList));
        return flag1 && flag2;
    }

    @Override
    public boolean updateRoleUser(Integer userId, List<Integer> roleIdList) {
        boolean flag;
        //先删除，后插入
        sysRoleUserService.remove(new QueryWrapper<SysRoleUser>().eq("user_id", userId));
        List<SysRoleUser> list = roleIdList.stream().map(roleId -> {
            SysRoleUser roleUser = new SysRoleUser();
            roleUser.setUserId(userId);
            roleUser.setRoleId(roleId);
            return roleUser;
        }).collect(Collectors.toList());
        flag = sysRoleUserService.saveBatch(list);
        return flag;
    }
}
