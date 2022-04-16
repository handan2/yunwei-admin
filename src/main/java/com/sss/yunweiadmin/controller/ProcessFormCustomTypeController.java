package com.sss.yunweiadmin.controller;


import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sss.yunweiadmin.common.result.ResponseResultWrapper;
import com.sss.yunweiadmin.model.entity.ProcessFormCustomType;
import com.sss.yunweiadmin.service.ProcessFormCustomTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * <p>
 * 流程中的自定义表单时，下拉类型中的，自定义类型 前端控制器
 * </p>
 *
 * @author 任勇林
 * @since 2021-03-19
 */
@RestController
@RequestMapping("/processFormCustomType")
@ResponseResultWrapper
public class ProcessFormCustomTypeController extends BaseController<ProcessFormCustomType> {
    @Autowired
    ProcessFormCustomTypeService processFormCustomTypeService;

    @Override
    @PostMapping("add")
    public boolean add(@RequestBody ProcessFormCustomType processFormCustomType) {
        //判断名称是否重复
        List<ProcessFormCustomType> list = processFormCustomTypeService.list(new QueryWrapper<ProcessFormCustomType>().eq("name", processFormCustomType.getName()));
        if (ObjectUtil.isNotEmpty(list)) {
            throw new RuntimeException("表名称已重复");
        }
        return processFormCustomTypeService.save(processFormCustomType);
    }

    @Override
    @PostMapping("edit")
    public boolean edit(@RequestBody ProcessFormCustomType processFormCustomType) {
        List<ProcessFormCustomType> list = processFormCustomTypeService.list(new QueryWrapper<ProcessFormCustomType>().ne("id", processFormCustomType.getId()).eq("name", processFormCustomType.getName()));
        if (ObjectUtil.isNotEmpty(list)) {
            throw new RuntimeException("表名称已重复");
        }
        return processFormCustomTypeService.updateById(processFormCustomType);
    }


}
