package com.sss.yunweiadmin.controller;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sss.yunweiadmin.common.result.ResponseResultWrapper;
import com.sss.yunweiadmin.model.entity.ProcessInstanceNode;
import com.sss.yunweiadmin.service.ProcessInstanceNodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 流程实例节点 前端控制器
 * </p>
 *
 * @author 任勇林
 * @since 2021-04-17
 */
@RestController
@RequestMapping("/processInstanceNode")
@ResponseResultWrapper
public class ProcessInstanceNodeController {
    @Autowired
    ProcessInstanceNodeService processInstanceNodeService;

    @GetMapping("list")
    public IPage<ProcessInstanceNode> list(Integer processInstanceDataId) {
        return processInstanceNodeService.page(new Page<>(1, 100), new QueryWrapper<ProcessInstanceNode>().eq("process_instance_data_id", processInstanceDataId));
    }
}
