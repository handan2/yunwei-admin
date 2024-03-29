package com.sss.yunweiadmin.common.utils;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.sss.yunweiadmin.model.entity.ProcessFormTemplate;
import com.sss.yunweiadmin.model.entity.SysDept;
import com.sss.yunweiadmin.model.entity.SysUser;
import com.sss.yunweiadmin.model.vo.FormTemplateVO;
import com.sss.yunweiadmin.model.vo.TreeSelectVO;
import com.sss.yunweiadmin.model.vo.TreeTransferVO;
import com.sss.yunweiadmin.service.SysUserService;
import org.springframework.beans.BeanUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

//前后端都可以实现结构树的方法，https://blog.csdn.net/bertZuo/article/details/107200165
public class TreeUtil {
    //下拉树
    public static <T> List<T> getTreeSelect(List<T> initList) {
        if (CollUtil.isEmpty(initList)) throw new RuntimeException("集合为空！");
        List<T> list = Lists.newArrayList();
        Map<Integer, T> map = Maps.newHashMap();
        for (T obj : initList) {
            Integer id = (Integer) ReflectUtil.getFieldValue(obj, "id");
            map.put(id, obj);
        }
        for (T obj : initList) {
            Integer id = (Integer) ReflectUtil.getFieldValue(obj, "id");
            Integer pid = (Integer) ReflectUtil.getFieldValue(obj, "pid");
            T parent = map.get(pid);
            if (parent != null) {
                Object childen = ReflectUtil.getFieldValue(parent, "children");
                if (childen != null) {
                    ReflectUtil.invoke(childen, "add", map.get(id));
                } else {
                    ReflectUtil.setFieldValue(parent, "children", Lists.newArrayList(map.get(id)));
                }
            } else {
                list.add(map.get(id));
            }
        }
        return list;
    }

    //下拉树;20211121这个仅限as_type那种单纯树表（而不是树表+一个其他表的组合，如部门/人员组合）结构生成tree; TreeSelectVO倒是通用
   //这个逻辑有时间还是应该总结下
    public static <T> List<TreeSelectVO> getTreeSelectVO(List<T> initList) {
        if (CollUtil.isEmpty(initList)) throw new RuntimeException("集合为空！");
        List<TreeSelectVO> treeList = Lists.newArrayList();
        Map<Integer, TreeSelectVO> map = Maps.newHashMap();

        for (T obj : initList) {
            Integer id = (Integer) ReflectUtil.getFieldValue(obj, "id");
            String name = (String) ReflectUtil.getFieldValue(obj, "name");
            TreeSelectVO treeSelectVO = new TreeSelectVO();
            treeSelectVO.setTitle(name);
            treeSelectVO.setKey(id);
            treeSelectVO.setValue(id);
            map.put(id, treeSelectVO);
        }
        //20211119遍历一个（满足特定结构：元组有id&pid字段）List 来生成（应该是森林）tree结构
        for (T obj : initList) {
            Integer id = (Integer) ReflectUtil.getFieldValue(obj, "id");

            Integer pid = (Integer) ReflectUtil.getFieldValue(obj, "pid");
            TreeSelectVO parent = map.get(pid);
            if (parent != null) {
                if (parent.getChildren() != null) {
                    parent.getChildren().add(map.get(id));
                } else {
                    parent.setChildren(Lists.newArrayList(map.get(id)));
                }
            } else {
                treeList.add(map.get(id));
            }
        }
        return treeList;
    }

    //下拉 部门-用户 树  20211121仅用于查询第三层结构的部门成员
    public static List<TreeTransferVO> getSelectDeptUserTree(List<SysDept> initList) {
        if (CollUtil.isEmpty(initList)) throw new RuntimeException("集合为空！");
        List<TreeTransferVO> treeList = Lists.newArrayList();
        Map<Integer, TreeTransferVO> depMap = Maps.newHashMap();

        //获取所有用户
        SysUserService sysUserService = SpringUtil.getBean(SysUserService.class);
        List<SysUser> userList = sysUserService.list(new QueryWrapper<SysUser>().orderByAsc("sort"));
        //20220520 collect（Collectors.groupingBy（））用法：返回一个map
        Map<Integer, List<SysUser>> depUserlistMap = userList.stream().collect(Collectors.groupingBy(SysUser::getDeptId));
        for (SysDept dept : initList) {
            Integer id = dept.getId();
            String deptName = dept.getName();
            TreeTransferVO treeTransferVO = new TreeTransferVO();
            treeTransferVO.setTitle(deptName);
            treeTransferVO.setKey("dept" + id);
            treeTransferVO.setCheckable(false);
            //设置用户
            /*
            * 20220521实际上下面这段逻辑可以这么设计（更逻辑“标准化”）：上一层逻辑组装完deptMap<deptID,transferVO>后
            * 直接“再起一层独立遍历List<sysUser>”组装完成"真正的UserMap<userID,transferVO>"
            *(而不是创建并遍历现在的"这个奇葩且名不符实的userMap<deptID,List<sysUser>>")
            *然后再分别遍历List<sysUser>/List<dept>来实现childern属性的组装；其中，List<dept>的遍历用下下段的代码即可
            * */
            if (depUserlistMap.get(id) != null) {
                List<TreeTransferVO> userTreeList = depUserlistMap.get(id).stream().map(user -> {
                    TreeTransferVO userTreeTransferVO = new TreeTransferVO();
                    userTreeTransferVO.setTitle(user.getDisplayName());
                    userTreeTransferVO.setKey("user" + user.getId());
                    userTreeTransferVO.setTmp(deptName);
                    return userTreeTransferVO;
                }).collect(Collectors.toList());
                treeTransferVO.setChildren(userTreeList);
            }

            depMap.put(id, treeTransferVO);
        }
        for (SysDept dept : initList) {
            Integer id = dept.getId();
            Integer pid = dept.getPid();
            TreeTransferVO parent = depMap.get(pid);
            if (parent != null) {//将上一级部门节点的childern赋值为下一级部门对应的vo
                if (parent.getChildren() != null) {
                    parent.getChildren().add(depMap.get(id));
                } else {
                    parent.setChildren(Lists.newArrayList(depMap.get(id)));
                }
            } else {//只有最上一级的部门结点才放入treeList
                treeList.add(depMap.get(id));
            }
        }
        return treeList;
    }

    //表格树
    public static <T> void setTableTree(List<T> list, List<T> otherList) {
        Map<Integer, T> map = Maps.newHashMap();
        for (T obj : list) {
            Integer id = (Integer) ReflectUtil.getFieldValue(obj, "id");
            map.put(id, obj);
        }
        for (T obj : otherList) {
            Integer id = (Integer) ReflectUtil.getFieldValue(obj, "id");
            map.put(id, obj);
        }

        for (T obj : otherList) {
            Integer id = (Integer) ReflectUtil.getFieldValue(obj, "id");
            Integer pid = (Integer) ReflectUtil.getFieldValue(obj, "pid");
            T parent = map.get(pid);
            if (parent != null) {
                Object childen = ReflectUtil.getFieldValue(parent, "children");
                if (childen != null) {
                    ReflectUtil.invoke(childen, "add", map.get(id));
                } else {
                    ReflectUtil.setFieldValue(parent, "children", Lists.newArrayList(map.get(id)));
                }
            }
        }
    }
    /**
     * 表单模板树
     * 20211207select变更字段的value值(选项字段)，做下判断与处理：根据约定（字符串格式）规则
     * 决定是否查下DB/把结果再塞回options相应的字段值中
     * 20220918 此方法已经拷贝到processTemplateServiceImpl.java中
     */
    public static List<FormTemplateVO> getFormTemplateTree(List<ProcessFormTemplate> initList) {
        if (CollUtil.isEmpty(initList)) throw new RuntimeException("集合为空！");
        List<FormTemplateVO> treeList = Lists.newArrayList();
        //<templateID,templateVO>:只是方法中用于查找方便设的
        Map<Integer, FormTemplateVO> map = Maps.newHashMap();
        //字段组类型的template<template.Label(字段组名),templateId>，作用同上
        Map<String, Integer> groupMap = Maps.newHashMap();

        for (ProcessFormTemplate processFormTemplate : initList) {//组装两类map
            FormTemplateVO formTemplateVO = new FormTemplateVO();
            BeanUtils.copyProperties(processFormTemplate, formTemplateVO);
            //20220918todo 这里添判断下(processFormTemplate.type===下拉单选不可编辑||下拉单选可编辑)&&select变更字段的value值(选项字段)是不是要设置数据源，是的话，做下处理
            if(StrUtil.isNotEmpty(formTemplateVO.getDatasource())){//实际最好加一个processName的约束：不过这个static方法好像不能autoWired相关service
                if(formTemplateVO.getLabel().equals("角色分配")){


                }

            }
            map.put(formTemplateVO.getId(), formTemplateVO);

            if (formTemplateVO.getFlag().equals("字段组类型")) {
                groupMap.put(formTemplateVO.getLabel(), formTemplateVO.getId());
            }
        }

        for (ProcessFormTemplate processFormTemplate : initList) {//我觉得是可以优化的，至少这个循环里应遍历processFormTemplateVOList的，当然前面那个map类型也需要调整：暂不研
            Integer id = processFormTemplate.getId();
            //有GroupParentLabel值的是字段组内的成员，GroupParentLabel值是字段组的label
            String groupParentLabel = processFormTemplate.getGroupParentLabel();
            //利用两个map:查找字段组Label对应的ID && 根据ID获取字段组VO,然后将给这处字段组VO设置children(当前遍历过程中的 processFormTemplate对应的VO)
            Integer groupParentId = groupMap.get(groupParentLabel);
            if (groupParentId != null) {
                FormTemplateVO parent = map.get(groupParentId);
                if (parent != null) {
                    if (parent.getChildren() != null) {
                        parent.getChildren().add(map.get(id));
                    } else {
                        parent.setChildren(Lists.newArrayList(map.get(id)));
                    }
                }
            } else {
                treeList.add(map.get(id));
            }
        }
        return treeList;
    }
}
