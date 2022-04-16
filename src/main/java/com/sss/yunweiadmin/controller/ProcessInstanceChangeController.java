package com.sss.yunweiadmin.controller;


import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sss.yunweiadmin.common.result.ResponseResultWrapper;
import com.sss.yunweiadmin.model.entity.ProcessInstanceChange;
import com.sss.yunweiadmin.service.ProcessInstanceChangeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    public IPage<ProcessInstanceChange> list(Integer asId) {
        if (ObjectUtil.isNotEmpty(asId)) {
            return processInstanceChangeService.page(new Page<>(1, 100), new QueryWrapper<ProcessInstanceChange>().eq("as_id", asId));
        }
        return null;
    }
}
