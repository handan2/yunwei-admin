package com.sss.yunweiadmin.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.sss.yunweiadmin.model.entity.ProcessDefinition;
import com.sss.yunweiadmin.model.entity.ProcessFormTemplate;
import com.sss.yunweiadmin.mapper.ProcessFormTemplateMapper;
import com.sss.yunweiadmin.model.entity.SysDic;
import com.sss.yunweiadmin.model.vo.FormTemplateVO;
import com.sss.yunweiadmin.service.ProcessDefinitionService;
import com.sss.yunweiadmin.service.ProcessFormTemplateService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sss.yunweiadmin.service.SysDicService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 自定义表单模板 服务实现类
 * </p>
 *
 * @author 任勇林
 * @since 2021-07-29
 */
@Service
public class ProcessFormTemplateServiceImpl extends ServiceImpl<ProcessFormTemplateMapper, ProcessFormTemplate> implements ProcessFormTemplateService {

    @Autowired
    ProcessDefinitionService processDefinitionService;
    @Autowired
    SysDicService sysDicService;
    /**
     * 表单模板树
     * 20220918从treeUtil.java拷贝到此
     */
    @Override
    public  List<FormTemplateVO> getFormTemplateTree(List<ProcessFormTemplate> initList) {
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
            if(StrUtil.isNotEmpty(formTemplateVO.getDatasource())){
                ProcessDefinition proDef = processDefinitionService.getById(formTemplateVO.getProcessDefinitionId());
                //注意formTemplateVO.getLabel().contains("选择角色")：可能有“选择角色1/2/3”(新用户入网流程就是这样)
                if((proDef.getProcessName().contains("应用系统用户新增")  || proDef.getProcessName().contains("新用户")) && formTemplateVO.getLabel().contains("选择角色") && formTemplateVO.getType().contains("选")){
                    List<SysDic> list = sysDicService.list(new QueryWrapper<SysDic>().eq("status","角色"));
                    if(CollUtil.isNotEmpty(list)){
                        Map<String,String> map1 = new HashMap<>();
                        for(SysDic s : list){
                            map1.put(s.getFlag(),s.getName());
// jsonobject.tostring  JSONobject.toJSONString(object xx)


                        }
                        String datasourceStr = JSONObject.toJSONString(map1);
                        formTemplateVO.setDatasource(datasourceStr);
//                        SysDic sysDic = list.get(0);
//                        if(ObjectUtil.isNotEmpty(sysDic.getName()))
//                            formTemplateVO.setValue(sysDic.getName());
                    }
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
