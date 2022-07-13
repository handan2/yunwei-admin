package com.sss.yunweiadmin.controller;


import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sss.yunweiadmin.common.result.ResponseResultWrapper;
import com.sss.yunweiadmin.model.entity.AsComputerSpecial;
import com.sss.yunweiadmin.model.entity.AsType;
import com.sss.yunweiadmin.model.entity.ProcessDefinition;
import com.sss.yunweiadmin.model.vo.ValueLabelVO;
import com.sss.yunweiadmin.service.ProcessDefinitionService;
import com.sss.yunweiadmin.service.impl.AsTypeServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

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
    @Autowired
    ProcessDefinitionService processDefinitionService;

    @GetMapping("getLevelTwoAsTypeLV")
    public List<ValueLabelVO> getLevel2AsTypeLV(){
        return asTypeService.list(new QueryWrapper<AsType>().eq("level",2)).stream().map(item->new ValueLabelVO(item.getName(),item.getName())).collect(Collectors.toList());




    }

    @GetMapping("getAsTypeIdByName")
    public int getAsTypeIdByName(String name) {
        System.out.println(name);
        AsType astype =  asTypeService.getOne(new QueryWrapper<AsType>().eq("name", name));
        if(ObjectUtil.isNotEmpty(astype))
            return astype.getId();
        else
            return 0;//20220702 从-1改成0，暂假设不会引发逻辑错误
    }
    @GetMapping("getAllowedAsTypeIdByProDefId")
    public int getAllowedAsTypeIdByProDefId(Integer id) {
        ProcessDefinition def = processDefinitionService.getOne(new QueryWrapper< ProcessDefinition>().eq("id",id));
        return getAsTypeIdByName(def.getProcessType2());

    }


}
