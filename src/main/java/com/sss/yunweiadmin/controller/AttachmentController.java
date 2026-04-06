package com.sss.yunweiadmin.controller;


import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sss.yunweiadmin.common.config.GlobalParam;
import com.sss.yunweiadmin.common.result.ResponseResultWrapper;
import com.sss.yunweiadmin.model.entity.Attachment;
import com.sss.yunweiadmin.model.entity.ProcessFormValue1;
import com.sss.yunweiadmin.model.entity.ProcessInstanceData;
import com.sss.yunweiadmin.service.ProcessInstanceDataService;
import com.sss.yunweiadmin.service.impl.AttachmentServiceImpl;
import com.sun.corba.se.spi.orbutil.threadpool.WorkQueue;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
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
 * @since 2024-01-12
 */
@RestController
@ResponseResultWrapper
@RequestMapping("/attachment")
public class AttachmentController {
    @Autowired
    AttachmentServiceImpl attachmentService;
    @Autowired
    ProcessInstanceDataService processInstanceDataService;
    @Autowired
    private Environment environment;




    @PostMapping("upload1")
    @SneakyThrows
    public boolean uploadAttach(MultipartFile[] files, String formValue) {
        return attachmentService.uploadAttach(files,formValue);
    }


    @GetMapping("delete")
    public boolean delete(Integer[] idArr) {

        return attachmentService.delete(idArr);

    }

    @GetMapping("list")
    public IPage<Attachment> list(int currentPage , int pageSize, String route,  String name,  String miji) {

        QueryWrapper<Attachment> queryWrapper = new  QueryWrapper<Attachment>().eq("org_id", GlobalParam.orgId).orderByDesc("id");
        if(ObjUtil.isNotEmpty(route))
            queryWrapper.like("route",route);
        if(ObjUtil.isNotEmpty(name))
            queryWrapper.like("name",name);
        if(ObjUtil.isNotEmpty(miji))
            queryWrapper.eq("miji",miji);
        return attachmentService.page(new Page<>(currentPage, pageSize), queryWrapper);//20240725 pageSize = 20,
    }

    @GetMapping("listForProcess")
    public IPage<Attachment> listForProcess(int currentPage , int pageSize, String route, Integer orgIdQ) {
        Integer orgId = GlobalParam.orgId;
        if(ObjUtil.isNotEmpty(orgIdQ)) //20241106 穿透流程的前端参数标记
            orgId = orgIdQ;
        QueryWrapper<Attachment> queryWrapper = new  QueryWrapper<Attachment>().eq("org_id", orgId).orderByDesc("id");
        queryWrapper.eq("route",route);
        return attachmentService.page(new Page<>(currentPage, 20), queryWrapper);//20240725 pageSize = 20,
    }
}
