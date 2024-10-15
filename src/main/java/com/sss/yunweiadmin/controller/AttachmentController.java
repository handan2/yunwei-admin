package com.sss.yunweiadmin.controller;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sss.yunweiadmin.common.result.ResponseResultWrapper;
import com.sss.yunweiadmin.model.entity.Attachment;
import com.sss.yunweiadmin.model.entity.ProcessFormValue1;
import com.sss.yunweiadmin.model.entity.ProcessInstanceData;
import com.sss.yunweiadmin.service.impl.AttachmentServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author 任勇林
 * @since 2024-01-12
 */
@RestController
@ResponseResultWrapper
@RequestMapping("/attachment")
public class AttachmentController {
    @Autowired
    AttachmentServiceImpl attachmentService;

    @GetMapping("list")
    public IPage<Attachment> list(int currentPage , int pageSize, String route) {
        QueryWrapper<Attachment> queryWrapper = new QueryWrapper<Attachment>().orderByDesc("id");
        queryWrapper.eq("route",route);
        return attachmentService.page(new Page<>(currentPage, 20), queryWrapper);//20240725 pageSize = 20,
    }
}
