package com.sss.yunweiadmin.controller;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sss.yunweiadmin.common.config.GlobalParam;
import com.sss.yunweiadmin.common.result.ResponseResultWrapper;
import com.sss.yunweiadmin.model.entity.ProcessFormValue1;
import com.sss.yunweiadmin.service.ProcessFormValue1Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 自定义表单，走流程时，保存表单中数据 前端控制器
 * </p>
 *
 * @author 任勇林
 * @since 2021-10-02
 */
@RestController
@RequestMapping("/processFormValue1")
@ResponseResultWrapper
public class ProcessFormValue1Controller {
    @Autowired
    ProcessFormValue1Service processFormValue1Service;

    @GetMapping("get")
    public ProcessFormValue1 get(Integer processDefinitionId, String actProcessInstanceId) {
        if(actProcessInstanceId.contains("_")) {//20241106 穿透流程的前端参数标记
            String[] a = actProcessInstanceId.split("_");
            return processFormValue1Service.getOne(new  QueryWrapper<ProcessFormValue1>().eq("org_id", a[1]).eq("process_definition_id", processDefinitionId).eq("act_process_instance_id",  a[0]));
        } else
            return processFormValue1Service.getOne(new  QueryWrapper<ProcessFormValue1>().eq("org_id", GlobalParam.orgId).eq("process_definition_id", processDefinitionId).eq("act_process_instance_id", actProcessInstanceId));
    }

}
