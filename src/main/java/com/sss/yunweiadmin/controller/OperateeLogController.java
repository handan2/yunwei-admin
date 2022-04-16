package com.sss.yunweiadmin.controller;


import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sss.yunweiadmin.common.result.ResponseResultWrapper;
import com.sss.yunweiadmin.model.entity.OperateeLog;
import com.sss.yunweiadmin.model.entity.ProcessInstanceData;
import com.sss.yunweiadmin.model.entity.SysUser;
import com.sss.yunweiadmin.service.OperateeLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 用户操作记录日志 前端控制器
 * </p>
 *
 * @author 任勇林
 * @since 2021-10-18
 */
@RestController
@ResponseResultWrapper
@RequestMapping("/operateeLog")
public class OperateeLogController {
    @Autowired
    OperateeLogService operateeLogService;
    @GetMapping("list")
    public IPage<OperateeLog> list(int currentPage, int pageSize, String displayName,String loginName,String dateRange){
        QueryWrapper<OperateeLog> queryWrapper = new QueryWrapper<OperateeLog>().orderByDesc("id");
        if (ObjectUtil.isNotEmpty(displayName)) {
            queryWrapper.eq("display_name", displayName);
        }
        if (ObjectUtil.isNotEmpty(loginName)) {
            queryWrapper.eq("login_name",loginName);
        }

        IPage<OperateeLog> page = operateeLogService.page(new Page<>(currentPage, pageSize), queryWrapper);
        return page;
    }

}
