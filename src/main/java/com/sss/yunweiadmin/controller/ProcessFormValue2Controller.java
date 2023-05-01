package com.sss.yunweiadmin.controller;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sss.yunweiadmin.common.result.ResponseResultWrapper;
import com.sss.yunweiadmin.model.entity.ProcessFormValue1;
import com.sss.yunweiadmin.model.entity.ProcessFormValue2;
import com.sss.yunweiadmin.service.ProcessFormValue1Service;
import com.sss.yunweiadmin.service.ProcessFormValue2Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * <p>
 * 自定义表单，走流程时，保存对应的自定义表的as_id，辅助process_form_value1 前端控制器
 * </p>
 *
 * @author 任勇林
 * @since 2021-10-02
 */
@RestController
@RequestMapping("/processFormValue2")
@ResponseResultWrapper
public class ProcessFormValue2Controller {
    @Autowired
    ProcessFormValue2Service processFormValue2Service;

    @GetMapping("get")
    public List<ProcessFormValue2> get(String actProcessInstanceId) {
        return processFormValue2Service.list(new QueryWrapper<ProcessFormValue2>().eq("act_process_instance_id", actProcessInstanceId));
    }

}
