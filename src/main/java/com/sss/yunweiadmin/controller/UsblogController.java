package com.sss.yunweiadmin.controller;


import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.common.base.Strings;
import com.sss.yunweiadmin.common.config.GlobalParam;
import com.sss.yunweiadmin.common.result.ResponseResultWrapper;
import com.sss.yunweiadmin.model.entity.Usblog;
import com.sss.yunweiadmin.service.UsblogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
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
@ResponseResultWrapper
@RequestMapping("/usblog")
public class UsblogController {

    @Autowired
    UsblogService usblogService;

    @ResponseBody

    @GetMapping("list")
    public IPage<Usblog> list(int currentPage, int pageSize, String no, String checkDate, Integer deptId, String status) {
        QueryWrapper<Usblog> queryWrapper = new  QueryWrapper<Usblog>().eq("org_id", GlobalParam.orgId).eq("org_id",GlobalParam.orgId);
        queryWrapper.orderByDesc("id");


        if (!Strings.isNullOrEmpty(no)) {
            queryWrapper.like("ebm", no);
        }
        if (ObjectUtil.isNotEmpty(checkDate)) {
            String[] dateArr = checkDate.split(",");
            queryWrapper.ge("check_date", dateArr[0] + " 00:00:00");
            queryWrapper.le("check_date", dateArr[1] + " 00:00:00");

        }
        //20211116
        IPage<Usblog> page = usblogService.page(new Page<>(currentPage, pageSize), queryWrapper);
        //page.getRecords().forEach(item -> item.setTemp(sysDeptService.getById(ObjectUtil.isNotEmpty(item.getDeptId()) ? item.getDeptId() : 2).getName()));
        return page;
    }



    @GetMapping("delete")
    public boolean delete(Integer[] idArr) {
        List<Integer> idList = Stream.of(idArr).collect(Collectors.toList());
        return usblogService.removeByIds(idList);

    }



}
