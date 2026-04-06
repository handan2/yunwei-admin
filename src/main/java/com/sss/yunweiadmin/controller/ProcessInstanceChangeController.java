package com.sss.yunweiadmin.controller;


import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sss.yunweiadmin.common.config.GlobalParam;
import com.sss.yunweiadmin.common.result.ResponseResultWrapper;
import com.sss.yunweiadmin.model.entity.ProcessInstanceChange;
import com.sss.yunweiadmin.service.ProcessInstanceChangeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.xml.bind.Element;

/**
 * <p>
 * 流程实例结束时，变更字段表 前端控制器
 * </p>
 *
 * @author 任勇林
 * @since 2021-10-23
 */
@RestController
@RequestMapping("/processInstanceChange")
@ResponseResultWrapper
public class ProcessInstanceChangeController {
    @Autowired
    ProcessInstanceChangeService processInstanceChangeService;

    @GetMapping("list")
    public IPage<ProcessInstanceChange> list(int currentPage, int pageSize, Integer asId, String newValue, String oldValue, String isFinish) {
        QueryWrapper<ProcessInstanceChange> queryWrapper  = new  QueryWrapper<ProcessInstanceChange>().eq("org_id", GlobalParam.orgId).eq("is_report_title", "否").orderByDesc("modify_datetime");
        int currentPage1 = 1;
        int pageSize1 = 100;
        if(ObjectUtil.isNotEmpty(currentPage))
            currentPage1 = currentPage;
        if(ObjectUtil.isNotEmpty(pageSize))
            pageSize1 = pageSize;
        if(ObjectUtil.isNotEmpty(newValue))
            queryWrapper.eq("new_value", newValue);
        if(ObjectUtil.isNotEmpty(oldValue))
            queryWrapper.eq("old_value", oldValue);
        if(ObjectUtil.isNotEmpty(isFinish))
            queryWrapper.eq("is_finish", isFinish);
        else//默认值
            queryWrapper.eq("is_finish", "是");
        if (ObjectUtil.isNotEmpty(asId)) {
            queryWrapper.eq("as_id", asId);
        }
        return processInstanceChangeService.page(new Page<>(currentPage1, pageSize1), queryWrapper);
    }
}
