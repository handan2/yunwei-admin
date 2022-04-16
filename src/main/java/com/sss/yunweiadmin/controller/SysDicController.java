package com.sss.yunweiadmin.controller;


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

import java.util.List;
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
        List<SysDic> list = sysDicService.list(new QueryWrapper<SysDic>().eq("flag", flag));
        return list.stream().map(item -> new ValueLabelVO(item.getName(), item.getName())).collect(Collectors.toList());
    }

    @GetMapping("getDicValueList")
    public List<String> getDicValueList(String flag) {
        List<SysDic> list = sysDicService.list(new QueryWrapper<SysDic>().eq("flag", flag));
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
