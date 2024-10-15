package com.sss.yunweiadmin.controller;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sss.yunweiadmin.common.result.ResponseResultWrapper;
import com.sss.yunweiadmin.common.utils.TreeUtil;
import com.sss.yunweiadmin.model.entity.AsType;
import com.sss.yunweiadmin.model.entity.SysUser;
import com.sss.yunweiadmin.model.vo.TreeSelectVO;
import com.sss.yunweiadmin.service.AsApplicationSpecialService;
import com.sss.yunweiadmin.service.SysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author 任勇林
 * @since 2021-11-21
 */
@RestController
@ResponseResultWrapper
@RequestMapping("/asApplicationSpecial")
public class AsApplicationSpecialController {
    @Autowired
    AsApplicationSpecialService asApplicationSpecialService;
    @Autowired
    SysUserService sysUserService;


}
