package com.sss.yunweiadmin.controller;


import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.common.collect.Lists;
import com.sss.yunweiadmin.common.result.ResponseResultWrapper;
import com.sss.yunweiadmin.common.utils.TreeUtil;
import com.sss.yunweiadmin.model.entity.SysMenu;
import com.sss.yunweiadmin.model.vo.TreeSelectVO;
import com.sss.yunweiadmin.service.SysMenuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * <p>
 * 菜单表，别名权限表、资源表 前端控制器
 * </p>
 *
 * @author 任勇林
 * @since 2021-08-09
 */
@RestController
@RequestMapping("/sysMenu")
@ResponseResultWrapper
public class SysMenuController {
    @Autowired
    SysMenuService sysMenuService;

    @GetMapping("list")
    public IPage<SysMenu> list(int currentPage, int pageSize) {
        //取出pid=0的数据
        IPage<SysMenu> page = sysMenuService.page(new Page<>(currentPage, pageSize), new QueryWrapper<SysMenu>().eq("pid", 0));
        List<SysMenu> list = page.getRecords();
        //取出pid！=0的数据
        List<SysMenu> otherList = sysMenuService.list(new QueryWrapper<SysMenu>().ne("pid", 0));

        TreeUtil.setTableTree(list, otherList);
        return page;
    }

    @PostMapping("add")
    public boolean add(@RequestBody SysMenu sysMenu) {
        return sysMenuService.save(sysMenu);
    }

    @GetMapping("get")
    public SysMenu getById(String id) {
        return sysMenuService.getById(id);
    }

    @PostMapping("edit")
    public boolean edit(@RequestBody SysMenu sysMenu) {
        return sysMenuService.updateById(sysMenu);
    }

    @GetMapping("delete")
    public boolean delete(Integer[] idArr) {
        List<Integer> idList = Stream.of(idArr).collect(Collectors.toList());
        //根据idList，取出所有的子节点
        List<Integer> list = Lists.newArrayList(idList);
        while (true) {
            List<SysMenu> tmp = sysMenuService.list(new QueryWrapper<SysMenu>().in("pid", idList));
            if (ObjectUtil.isEmpty(tmp)) {
                break;
            } else {
                idList = tmp.stream().map(SysMenu::getId).collect(Collectors.toList());
                list.addAll(idList);
            }
        }
        return sysMenuService.remove(new QueryWrapper<SysMenu>().in("id", list));
    }

    @GetMapping("getMenuTree")
    public List<TreeSelectVO> getMenuTree() {
        List<SysMenu> list = sysMenuService.list(new QueryWrapper<SysMenu>().orderByAsc("sort"));
        return TreeUtil.getTreeSelectVO(list);
    }

    //生成增删改查按钮
    @GetMapping("/crudButton")
    public boolean crudButton(Integer pid) {
        List<SysMenu> list = Lists.newArrayList();
        //新增
        SysMenu add = new SysMenu();
        add.setPid(pid);
        add.setName("新增");
        add.setFlag("按钮");
        add.setType("add");
        add.setIcon("plus");
        //编辑
        SysMenu edit = new SysMenu();
        edit.setPid(pid);
        edit.setName("编辑");
        edit.setFlag("按钮");
        edit.setType("edit");
        edit.setIcon("edit");
        //浏览
        SysMenu view = new SysMenu();
        view.setPid(pid);
        view.setName("浏览");
        view.setFlag("按钮");
        view.setType("preview");
        view.setIcon("scan");
        //批量删除
        SysMenu delete = new SysMenu();
        delete.setPid(pid);
        delete.setName("批量删除");
        delete.setFlag("按钮");
        delete.setType("delete");
        delete.setIcon("delete");
        //查询
        SysMenu query = new SysMenu();
        query.setPid(pid);
        query.setName("查询");
        query.setFlag("查询");

        list.add(add);
        list.add(edit);
        list.add(view);
        list.add(delete);
        list.add(query);

        return sysMenuService.saveBatch(list);
    }

}
