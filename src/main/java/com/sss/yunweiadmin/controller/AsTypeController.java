package com.sss.yunweiadmin.controller;


import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sss.yunweiadmin.common.result.ResponseResultWrapper;
import com.sss.yunweiadmin.model.entity.AsComputerSpecial;
import com.sss.yunweiadmin.model.entity.AsType;
import com.sss.yunweiadmin.service.impl.AsTypeServiceImpl;
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
 * @since 2021-10-08
 */
@RestController
@RequestMapping("/asType")
@ResponseResultWrapper
public class AsTypeController {
    @Autowired
    AsTypeServiceImpl asTypeService;
    @GetMapping("getAsType")
    public int getAsIdByType(String name) {
        AsType astype =  asTypeService.getOne(new QueryWrapper<AsType>().eq("name", name));
        if(ObjectUtil.isNotEmpty(astype))
            return astype.getId();
        else
            return 0;
    }



}
