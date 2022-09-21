package com.sss.yunweiadmin.controller;


import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.common.collect.Lists;
import com.sss.yunweiadmin.common.result.ResponseResultWrapper;
import com.sss.yunweiadmin.common.utils.TreeUtil;
import com.sss.yunweiadmin.model.entity.SysDept;
import com.sss.yunweiadmin.model.entity.SysUser;
import com.sss.yunweiadmin.model.vo.TreeSelectVO;
import com.sss.yunweiadmin.model.vo.TreeTransferVO;
import com.sss.yunweiadmin.service.SysDeptService;
import com.sss.yunweiadmin.service.SysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * <p>
 * 部门表 前端控制器
 * </p>
 *
 * @author 任勇林
 * @since 2021-04-09
 */
@RestController
@RequestMapping("/sysDept")
@ResponseResultWrapper
public class SysDeptController extends BaseController<SysDept> {
    @Autowired
    private SysDeptService sysDeptService;
    @Autowired
    private  SysUserService sysUserService;

    @Override
    @GetMapping("list")
    public IPage<SysDept> list(int currentPage, int pageSize) {
        //取出pid=0的数据
        IPage<SysDept> page = sysDeptService.page(new Page<>(currentPage, pageSize), new QueryWrapper<SysDept>().eq("pid", 0).orderByAsc("sort"));
        List<SysDept> list = page.getRecords();
        //取出pid！=0的数据
        List<SysDept> otherList = sysDeptService.list(new QueryWrapper<SysDept>().ne("pid", 0).orderByAsc("sort"));

        TreeUtil.setTableTree(list, otherList);
        return page;
    }

    @PostMapping("add")
    public boolean add(@RequestBody SysDept sysDept) {
        return sysDeptService.save(sysDept);
    }

    @GetMapping("get")
    public SysDept getById(String id) {
        return sysDeptService.getById(id);
    }

    @PostMapping("edit")
    public boolean edit(@RequestBody SysDept sysDept) {
        return sysDeptService.updateById(sysDept);
    }

    @GetMapping("delete")
    public boolean delete(Integer[] idArr) {
        List<Integer> idList = Stream.of(idArr).collect(Collectors.toList());
        //根据idList，取出所有的子节点
        List<Integer> list = Lists.newArrayList(idList);
        while (true) {
            List<SysDept> tmp = sysDeptService.list(new QueryWrapper<SysDept>().in("pid", idList));
            if (CollUtil.isEmpty(tmp)) {
                break;
            } else {
                idList = tmp.stream().map(SysDept::getId).collect(Collectors.toList());
                list.addAll(idList);
            }
        }
        return sysDeptService.remove(new QueryWrapper<SysDept>().in("id", list));
    }

    @GetMapping("getDeptTree")
    public List<TreeSelectVO> getDeptTree() {
        List<SysDept> list = sysDeptService.list(new QueryWrapper<SysDept>().orderByAsc("sort").eq("remark","基层部门"));
        return TreeUtil.getTreeSelectVO(list);
    }
    //穿梭框里的部门人员树，，仅查询第第三层部门成员 todo改名
    @GetMapping("getDeptUserTree")
    public List<TreeTransferVO> getDeptUserTree() {
        List<SysDept> list = sysDeptService.list(new QueryWrapper<SysDept>().notIn("id", 1, 2).orderByAsc("sort"));
        return TreeUtil.getSelectDeptUserTree(list);
    }
    //20211121获得treeSelect部门人员那种tree，仅查询第第三层部门成员
    @GetMapping("getDeptUserTreeSelect")
    public  List<TreeSelectVO> getDeptUserTreeSelect() {
        List<SysDept> list = sysDeptService.list(new QueryWrapper<SysDept>().eq("pid", 2).orderByAsc("sort"));
        if (CollUtil.isEmpty(list)) throw new RuntimeException("集合为空！");
        List<TreeSelectVO> treeList =new ArrayList<>();
        for(SysDept obj:list){
            TreeSelectVO treeSelectVO = new TreeSelectVO();
            treeSelectVO.setTitle(obj.getName());
            treeSelectVO.setKey(obj.getId());
            treeSelectVO.setValue(obj.getId()+100000);//为了不和子结点的value重复
            treeSelectVO.setSelectable(false);//部门这级不可选
            treeList.add(treeSelectVO);
            List<SysUser> userList = sysUserService.list(new QueryWrapper<SysUser>().eq("dept_id",obj.getId()));
            List<TreeSelectVO> treeList2 =new ArrayList<>();
            for(SysUser user:userList){
                TreeSelectVO treeSelectVO1 = new TreeSelectVO();
                treeSelectVO1.setTitle(user.getDisplayName());
                treeSelectVO1.setKey(user.getId()+"."+user.getDisplayName()+"."+user.getSecretDegree()+"."+obj.getName()+"."+user.getIdNumber());//
                treeSelectVO1.setValue(user.getId());//+user.getDisplayName()+"."+user.getSecretDegree()+"."+obj.getName());//20220903改造：之前只是存id
                treeList2.add(treeSelectVO1);
            }
            if(CollUtil.isNotEmpty(treeList2))
                treeSelectVO.setChildren(treeList2);
        }
        return treeList;

    }

}
