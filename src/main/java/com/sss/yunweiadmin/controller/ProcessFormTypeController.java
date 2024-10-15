package com.sss.yunweiadmin.controller;


import com.sss.yunweiadmin.common.result.ResponseResultWrapper;
import com.sss.yunweiadmin.model.entity.ProcessFormType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 流程中的自定义表单时，下拉类型 前端控制器
 * </p>
 *
 * @author 任勇林
 * @since 2021-03-19
 */
@RestController
@RequestMapping("/processFormType")
@ResponseResultWrapper
public class ProcessFormTypeController extends BaseController<ProcessFormType> {

}
