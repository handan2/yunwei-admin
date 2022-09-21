package com.sss.yunweiadmin.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
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

import java.sql.Struct;
import java.util.List;
import java.util.Map;
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
    public int add(SysUser sysUser) {
        List<SysUser> list = this.list(new QueryWrapper<SysUser>().eq("login_name", sysUser.getLoginName()));
        if (list.size() > 0) {
            throw new RuntimeException(sysUser.getLoginName() + "已存在");
        }
        List<SysUser> list2 = this.list(new QueryWrapper<SysUser>().eq("id_number", sysUser.getIdNumber()));
        if (list2.size() > 0) {
            throw new RuntimeException(sysUser.getIdNumber() + "已存在");
        }
        boolean flag1, flag2;
        sysUser.setPassword(SecureUtil.md5(sysUser.getPassword()));
        flag1 = this.save(sysUser);
        //默认为普通用户
        SysRoleUser sysRoleUser = new SysRoleUser();
        sysRoleUser.setRoleId(11);//约定普通用户ID
        sysRoleUser.setUserId(sysUser.getId());
        flag2 = sysRoleUserService.save(sysRoleUser);
        //return flag1 && flag2;
        return sysUser.getId();//20220820改成返回新增用户的ID：为了前端在手工添加代理人后需要获得用户ID(以拼成committer_str)
    }

    @Override
    public boolean upateByIdentity(String identity ,String loginName) {
        if(StrUtil.isNotEmpty(identity) && StrUtil.isNotEmpty(loginName)){
            List<SysUser> list = this.list(new QueryWrapper<SysUser>().eq("id_number",identity));
            if(CollUtil.isNotEmpty(list)){
                SysUser user = list.get(0);
                user.setLoginName(loginName);
                this.updateById(user);
                return true;
            }
        }

        return false;

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
