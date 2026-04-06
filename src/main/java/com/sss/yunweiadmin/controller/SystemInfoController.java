package com.sss.yunweiadmin.controller;


import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sss.yunweiadmin.common.config.GlobalParam;
import com.sss.yunweiadmin.common.result.ResponseResultWrapper;
import com.sss.yunweiadmin.model.entity.SystemInfo;
import com.sss.yunweiadmin.model.entity.Usblog;
import com.sss.yunweiadmin.service.SystemInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author 任勇林
 * @since 2025-04-02
 */
@RestController
@RequestMapping("/systemInfo")
public class SystemInfoController {
    @Autowired
    SystemInfoService systemInfoService;

    @ResponseBody
    @ResponseResultWrapper
    @GetMapping("list")
    public IPage<SystemInfo> list(int currentPage, int pageSize, String no, String checkDate) {
        QueryWrapper<SystemInfo> queryWrapper = new  QueryWrapper<SystemInfo>().eq("org_id", GlobalParam.orgId);
        queryWrapper.orderByDesc("id");





        //20211116
        IPage<SystemInfo> page = systemInfoService.page(new Page<>(currentPage, pageSize), queryWrapper);
        //page.getRecords().forEach(item -> item.setTemp(sysDeptService.getById(ObjectUtil.isNotEmpty(item.getDeptId()) ? item.getDeptId() : 2).getName()));
        return page;
    }

    @ResponseResultWrapper
    @GetMapping("delete")
    public boolean delete(Integer[] idArr) {
        List<Integer> idList = Stream.of(idArr).collect(Collectors.toList());
        return systemInfoService.removeByIds(idList);

    }

}
