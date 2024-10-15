package com.sss.yunweiadmin.controller;


import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.common.base.Strings;
import com.sss.yunweiadmin.common.result.ResponseResultWrapper;
import com.sss.yunweiadmin.model.entity.SysDic;
import com.sss.yunweiadmin.model.vo.ValueLabelVO;
import com.sss.yunweiadmin.service.SysDicService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * <p>
 * 数据字典，用于固定的下拉选项 前端控制器
 * </p>
 *
 * @author 任勇林
 * @since 2021-03-17
 */
@RestController
@RequestMapping("/sysDic")
@ResponseResultWrapper
public class SysDicController {
    @Autowired
    private SysDicService sysDicService;

    @GetMapping("list")
    public IPage<SysDic> list(int currentPage, int pageSize, String flag, String name) {
        QueryWrapper<SysDic> queryWrapper = new QueryWrapper<>();
        if (!Strings.isNullOrEmpty(flag)) {
            queryWrapper.like("flag", flag);
        }
        if (!Strings.isNullOrEmpty(name)) {
            queryWrapper.like("name", name);
        }
        return sysDicService.page(new Page<>(currentPage, pageSize), queryWrapper);
    }

    @GetMapping("getDicVL")
    public List<ValueLabelVO> getDicVL(String flag) {
        List<SysDic> list = sysDicService.list(new QueryWrapper<SysDic>().eq("flag", flag).orderByAsc("sort"));
        return list.stream().map(item -> new ValueLabelVO(item.getName(), item.getName())).collect(Collectors.toList());
    }

    @GetMapping("getDicValueList")
    public List<String> getDicValueList(String flag) {
        List<SysDic> list = sysDicService.list(new QueryWrapper<SysDic>().eq("flag", flag).orderByAsc("sort"));
        return list.stream().map(SysDic::getName).collect(Collectors.toList());
    }

    @PostMapping("add")
    public boolean add(@RequestBody SysDic sysDic) {
        return sysDicService.save(sysDic);
    }

    @GetMapping("get")
    public SysDic getById(String id) {
        return sysDicService.getById(id);
    }

    @GetMapping("getAppRoleMap")
    public Map<String,String> getAppRoleMap(String status, String flag, String name) {
        Map<String,String> map = new HashMap<>();
        List<SysDic> list =  sysDicService.list(new QueryWrapper<SysDic>().eq("status","角色"));
        if(CollUtil.isNotEmpty(list)){
            for(SysDic s :list){
                map.put(s.getFlag(),s.getName());
            }
        }
        return map;
    }


    @GetMapping("getReportPathMap")
    public Map<String,Object> getReportPathMap(String status, String flag, String name) {
        Map<String,Object> map = new HashMap<>();
        List<SysDic> list;
        list = sysDicService.list(new QueryWrapper<SysDic>().eq("flag","资产概览").eq("status","报表"));
        if(CollUtil.isNotEmpty(list))
            map.put("资产概览",list.get(0).getName());
        list = sysDicService.list(new QueryWrapper<SysDic>().eq("flag","指标监控").eq("status","报表"));
        if(CollUtil.isNotEmpty(list))
            map.put("指标监控",list.get(0).getName());
        list = sysDicService.list(new QueryWrapper<SysDic>().eq("flag","报表路径前缀").eq("status","报表"));
        if(CollUtil.isNotEmpty(list))
            map.put("报表路径前缀",list.get(0).getName());
        Map<String,String> map_process = new HashMap<>();
        list = sysDicService.list(new QueryWrapper<SysDic>().like("flag","流程").eq("status","报表"));
        if(CollUtil.isNotEmpty(list)){
            for(SysDic s :list){
                map_process.put(s.getFlag(),s.getName());
            }
            map.put("流程",map_process);

        }
        list = sysDicService.list(new QueryWrapper<SysDic>().like("flag","生命周期").eq("status","报表"));
        if(CollUtil.isNotEmpty(list)){
            map.put("生命周期",list.get(0).getName());
        }
        list = sysDicService.list(new QueryWrapper<SysDic>().like("flag","生命周期_查询").eq("status","报表"));
        if(CollUtil.isNotEmpty(list)){
            map.put("生命周期_查询",list.get(0).getName());
        }
        list = sysDicService.list(new QueryWrapper<SysDic>().like("flag","资产台账").eq("status","报表"));
        if(CollUtil.isNotEmpty(list)){
            map.put("资产台账",list.get(0).getName());
        }
        return map;
    }

    @GetMapping("getOne")
    public SysDic getOne(String status,  String flag, String name) {
        QueryWrapper<SysDic> queryWrapper = new QueryWrapper<>();
        if (!Strings.isNullOrEmpty(flag)) {
            queryWrapper.like("flag", flag);
        }
        if (!Strings.isNullOrEmpty(name)) {
            queryWrapper.like("name", name);
        }
        if (!Strings.isNullOrEmpty(status)) {
            queryWrapper.like("status", status);
        }
        List<SysDic> list = sysDicService.list(queryWrapper);
        if(CollUtil.isNotEmpty(list))
            return list.get(0);
        return null;
    }

    @PostMapping("edit")
    public boolean edit(@RequestBody SysDic sysDic) {
        return sysDicService.updateById(sysDic);
    }

    @GetMapping("delete")
    public boolean delete(Integer[] idArr) {
        List<Integer> idList = Stream.of(idArr).collect(Collectors.toList());
        return sysDicService.removeByIds(idList);

    }

}










