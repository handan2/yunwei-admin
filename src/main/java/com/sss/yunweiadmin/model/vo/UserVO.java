package com.sss.yunweiadmin.model.vo;

import com.sss.yunweiadmin.model.entity.SysPermission;
import com.sss.yunweiadmin.model.entity.SysUser;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class UserVO {
    //用户
    private SysUser user;
    //导航菜单
    private List<SysPermission> menuList;
    //操作按钮-按钮组
    private Map<String, List<SysPermission>> operateButtonMap;
    //数据列表-按钮
    private Map<String, List<SysPermission>> dataListButtonMap;
    //数据列表-发起流程
    private Map<Integer, SysPermission> startProcessButtonMap;
    //查询
    private Map<String, SysPermission> queryMap;
    //角色List
    private  List<Integer> roleIdList;
}

