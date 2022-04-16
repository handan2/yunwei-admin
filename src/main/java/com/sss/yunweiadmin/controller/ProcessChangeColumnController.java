package com.sss.yunweiadmin.controller;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sss.yunweiadmin.common.result.ResponseResultWrapper;
import com.sss.yunweiadmin.model.entity.ProcessChangeColumn;
import com.sss.yunweiadmin.service.ProcessChangeColumnService;
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
 * @since 2021-05-13
 */
@RestController
@RequestMapping("/processChangeColumn")
@ResponseResultWrapper
public class ProcessChangeColumnController {
    @Autowired
    ProcessChangeColumnService processChangeColumnService;

    @GetMapping("list")
    public IPage<ProcessChangeColumn> list(Integer asId) {
        return processChangeColumnService.page(new Page<>(1, 100), new QueryWrapper<ProcessChangeColumn>().eq("as_id", asId));
    }
}
