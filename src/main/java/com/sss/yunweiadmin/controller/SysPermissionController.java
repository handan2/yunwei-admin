package com.sss.yunweiadmin.controller;


import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.common.collect.Lists;
import com.sss.yunweiadmin.common.result.ResponseResultWrapper;
import com.sss.yunweiadmin.common.utils.TreeUtil;
import com.sss.yunweiadmin.model.entity.SysPermission;
import com.sss.yunweiadmin.model.vo.TreeSelectVO;
import com.sss.yunweiadmin.service.SysPermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * <p>
 * 权限表
 * </p>
 *
 * @author 任勇林
 * @since 2021-10-31
 */
@RestController
@RequestMapping("/sysPermission")
@ResponseResultWrapper
public class SysPermissionController {
    @Autowired
    SysPermissionService sysPermissionService;

    @GetMapping("list")
    public IPage<SysPermission> list(int currentPage, int pageSize) {
        //取出pid=0的数据
        IPage<SysPermission> page = sysPermissionService.page(new Page<>(currentPage, pageSize), new QueryWrapper<SysPermission>().eq("pid", 0));
        List<SysPermission> list = page.getRecords();
        //取出pid！=0的数据
        List<SysPermission> otherList = sysPermissionService.list(new QueryWrapper<SysPermission>().ne("pid", 0));

        TreeUtil.setTableTree(list, otherList);
        return page;
    }

    @PostMapping("add")
    public boolean add(@RequestBody SysPermission sysPermission) {
        return sysPermissionService.save(sysPermission);
    }

    @GetMapping("get")
    public SysPermission getById(String id) {
        return sysPermissionService.getById(id);
    }

    @PostMapping("edit")
    public boolean edit(@RequestBody SysPermission sysPermission) {
        return sysPermissionService.updateById(sysPermission);
    }

    @GetMapping("delete")
    public boolean delete(Integer[] idArr) {
        List<Integer> idList = Stream.of(idArr).collect(Collectors.toList());
        //根据idList，取出所有的子节点
        List<Integer> list = Lists.newArrayList(idList);
        while (true) {
            List<SysPermission> tmp = sysPermissionService.list(new QueryWrapper<SysPermission>().in("pid", idList));
            if (ObjectUtil.isEmpty(tmp)) {
                break;
            } else {
                idList = tmp.stream().map(SysPermission::getId).collect(Collectors.toList());
                list.addAll(idList);
            }
        }
        return sysPermissionService.remove(new QueryWrapper<SysPermission>().in("id", list));
    }

    @GetMapping("getPermissionTree")
    public List<TreeSelectVO> getPermissionTree() {
        List<SysPermission> list = sysPermissionService.list(new QueryWrapper<SysPermission>().ne("type","权限").orderByAsc("sort"));
        return TreeUtil.getTreeSelectVO(list);
    }

    @GetMapping("crud")
    public boolean crud(Integer pid) {
        List<SysPermission> list = new ArrayList<>();

        List<SysPermission> list2 = sysPermissionService.list(new QueryWrapper<SysPermission>().eq("pid", 83));
        for (SysPermission sysPermission : list2) {
            sysPermission.setId(null);
            sysPermission.setPid(pid);
            list.add(sysPermission);
        }

        return sysPermissionService.saveBatch(list);
    }
}
