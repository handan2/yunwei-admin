package com.sss.yunweiadmin.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.sss.yunweiadmin.common.utils.ProcessFormCustomTypeUtil;
import com.sss.yunweiadmin.model.entity.*;
import com.sss.yunweiadmin.mapper.ProcessFormTemplateMapper;
import com.sss.yunweiadmin.model.vo.CheckTaskVO;
import com.sss.yunweiadmin.model.vo.FormTemplateVO;
import com.sss.yunweiadmin.model.vo.TableTypeVO;
import com.sss.yunweiadmin.service.*;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    @Autowired
    ProcessInstanceDataService processInstanceDataService;
    @Autowired
    ProcessFormValue1Service processFormValue1Service;
    @Autowired
    ProcessDefinitionTaskService processDefinitionTaskService;
    @Autowired
    ProcessFormCustomTypeService processFormCustomTypeService;
    @Autowired
    AsDeviceCommonService asDeviceCommonService;

    @Override
    public Map<String, String> getLabelIdMapForItemByAjax(Integer processDefinitionId, String hideGroupIds) {
        if(ObjectUtil.isEmpty(processDefinitionId))
            return null;
        ProcessDefinition processDefinition = processDefinitionService.getById(processDefinitionId);
        List<ProcessFormTemplate> list = this.list(new QueryWrapper<ProcessFormTemplate>().eq("process_definition_id", processDefinitionId));
        Map<String, String> map = Maps.newHashMap();
        List<String> hideGroupMemberLabelList = new ArrayList<>();
        //20230225 暂时把屏敝隐藏字段组（里的字段）这段代码注释了：因为有的“用于排他网关的隐藏字段”放在隐藏字段组后（因此处处理）无法在前端自动填充赋值
//        if (StrUtil.isNotEmpty(hideGroupIds)) {
//            String[] hideGroupIdArr = hideGroupIds.split(",");
//            List<String> idList = Stream.of(hideGroupIdArr).collect(Collectors.toList());
//            idList.stream().forEach(item -> {
//                ProcessFormTemplate hideGroup = this.getById(item);//验证下，item/id 此时是字符串会不会有问题？好像可以
//                if (ObjectUtil.isNotEmpty(hideGroup))
//                    hideGroupMemberLabelList.add(hideGroup.getLabel());
//            });
//        }
        for (ProcessFormTemplate processFormTemplate : list) {
            if (CollUtil.isNotEmpty(hideGroupMemberLabelList)) {
                if (hideGroupMemberLabelList.contains(processFormTemplate.getGroupParentLabel()))//20220821排除掉隐藏字段组内的成员
                    continue;
            }
            if ("字段变更类型".equals(processFormTemplate.getFlag())) {
                map.put(processFormTemplate.getLabel().split("\\.")[1], processFormTemplate.getId().toString());
            } else if ("基本类型".equals(processFormTemplate.getFlag())) {
                map.put(processFormTemplate.getLabel(), processFormTemplate.getId().toString());
            }
        }
        //202207331把自定义表字段也加上
        List<TableTypeVO> list1 = new ArrayList<>();
        Map<Integer, List<TableTypeVO>> mapForTableTypeVOList = this.getTableTypeVO(processDefinitionId);
        for (Map.Entry<Integer, List<TableTypeVO>> entry : mapForTableTypeVOList.entrySet()) {
            list1.addAll(entry.getValue());
        }
        list1.stream().forEach(item -> {//20220905 对外设入网流程，两类设备的“涉密级别”做个区分，计算机类为“table_涉密级别2”；
            if(processDefinition.getProcessName().contains("外设声像及办公自动化申领")||processDefinition.getProcessName().contains("外设声像及办公自动化变更")){
                if(item.getLabel().contains("涉密级别") && item.getLabel().contains("计算机"))
                    map.put("table_计算机_" + item.getLabel().split("\\.")[1], item.getName());
                else if(item.getLabel().contains("涉密级别") && item.getLabel().contains("外设"))
                    map.put("table_外设_" + item.getLabel().split("\\.")[1], item.getName());
                else if(item.getLabel().contains("设备编号") && item.getLabel().contains("计算机"))
                    map.put("table_计算机_" + item.getLabel().split("\\.")[1], item.getName());
                else if(item.getLabel().contains("设备编号") && item.getLabel().contains("外设"))
                    map.put("table_外设_" + item.getLabel().split("\\.")[1], item.getName());
                else
                    map.put("table_" + item.getLabel().split("\\.")[1], item.getName());
            } else if(processDefinition.getProcessName().contains("密钥更换")){//20230112t密钥更换流程；不同于上面那个外设流程:本流程把外有字段都二类区分
                if(item.getLabel().contains("密钥信息表2"))
                    map.put("table_密钥2_" + item.getLabel().split("\\.")[1], item.getName());
                else if(item.getLabel().contains("密钥信息表."))//注意加了个点“.”
                    map.put("table_密钥_" + item.getLabel().split("\\.")[1], item.getName());
                else
                    map.put("table_" + item.getLabel().split("\\.")[1], item.getName());
            }
            if(!processDefinition.getProcessName().contains("密钥更换") && !processDefinition.getProcessName().contains("外设声像及办公自动化申领")  && !processDefinition.getProcessName().contains("外设声像及办公自动化变更") )//密钥更换流程两个设备因为是同样基本类型（虽然自定义表名不一样）：暂只保留下面的设备编号属性吧
                map.put("table_" + item.getLabel().split("\\.")[1], item.getName());
        });
        return map;
    }

    @Override
    public Map<Integer, List<TableTypeVO>> getTableTypeVO(Integer processDefinitionId) {
        Map<Integer, List<TableTypeVO>> map = Maps.newTreeMap();
        //1.取出所有的表类型的名称
        List<ProcessFormTemplate> list = this.list(new QueryWrapper<ProcessFormTemplate>().eq("process_definition_id", processDefinitionId));
        List<Integer> tableIdList = list.stream().filter(item -> item.getFlag().equals("表类型")).map(item -> {
            String tableId = item.getType().split("\\.")[0];
            return Integer.parseInt(tableId);
        }).collect(Collectors.toList());
        if (CollUtil.isNotEmpty(list) && CollUtil.isNotEmpty(tableIdList)) {
            //2.根据表名称取出processFormCustomType
            List<ProcessFormCustomType> typeList = processFormCustomTypeService.list(new QueryWrapper<ProcessFormCustomType>().in("id", tableIdList));
            //3.
            for (ProcessFormCustomType processFormCustomType : typeList) {
                List<TableTypeVO> tmpList = Lists.newArrayList();

                String props = processFormCustomType.getProps();
                Map<String, List<AsConfig>> tmpMap = ProcessFormCustomTypeUtil.parseProps(props);//String代表英文table名称
                tmpMap.entrySet().stream().forEach(item -> {
                    item.getValue().forEach(item2 -> {
                        TableTypeVO tableTypeVO = new TableTypeVO();
                        tableTypeVO.setLabel(processFormCustomType.getName() + "." + item2.getZhColumnName());
                        tableTypeVO.setName(processFormCustomType.getId() + "." + processFormCustomType.getName() + "." + item.getKey() + "." + item2.getEnColumnName() + "." + item2.getId());
                        tmpList.add(tableTypeVO);
                    });
                });

                map.put(processFormCustomType.getId(), tmpList);
            }
        }
        return map;
    }


    /**
     * 表单模板树
     * 20220918从treeUtil.java拷贝到此
     * 20221109 返回的 List<FormTemplateVO>实际是一个“森林/多链表“：成员只是“第一层formItem”:其他的子成员（如第一层字段组内的成员）会成为对应VO的children成员
     */
    @Override
    public  List<FormTemplateVO> getFormTemplateTree(List<ProcessFormTemplate> initList,Integer processDefinitionId,String actProcessInstanceId) {
        if (CollUtil.isEmpty(initList)) throw new RuntimeException("集合为空！");
        //20221103
        String[] hideFormItemArr ;
        List<String> hideFormItemArrList = new ArrayList<>();
        ProcessDefinition processDefinition = processDefinitionService.getById(processDefinitionId);
/*        20221122 actProcessInstanceId.equals("-1"):代表非流程审批动作的（表单）查看/修改
          普通的流程审批环节：排除发起节点、授权发起节点、非流程审批动作的（表单）查看/修改*/
        if( !("-1").equals(actProcessInstanceId) && !processDefinition.getStartLimitedByCheck().equals("是")){ //20221117将“被授权流程”跳过
            if(ObjectUtil.isNotEmpty(actProcessInstanceId)){
                CheckTaskVO checkTaskVO = processInstanceDataService.getCheckTaskVO(processDefinitionId,actProcessInstanceId);
                if(ObjectUtil.isNotEmpty(checkTaskVO.getHideItemLabels())) {
                    hideFormItemArr = checkTaskVO.getHideItemLabels().split(",");
                    //思考：如何保证流程定义修改后（所有的formitem的ID值都会变化）"隐藏字段组"机制还能生效?：是因为真正起作用的是hideGroupLabels,而不是hideGroupId???y
                    hideFormItemArrList = Arrays.asList(hideFormItemArr);//.contains()
                }
            } else {  //20221104如果actProcessInstanceId为null：发起流程节点
                String hideFormItemStr = processDefinitionTaskService.getOne(new QueryWrapper<ProcessDefinitionTask>().eq("task_type","bpmn:startTask").eq("process_definition_id",processDefinitionId)).getHideItemLabels();
                if(ObjectUtil.isNotEmpty(hideFormItemStr)){
                    hideFormItemArr = hideFormItemStr.split(",");
                    hideFormItemArrList = Arrays.asList(hideFormItemArr);
                }
            }

        }
        //再把 发起节点的流程处理下（这里也不含被授权流程：因为被授权流程发起节点也是有actProcessInstance的）; 约定了”发起“这个发起节点名称格式
//        if(ObjectUtil.isEmpty(actProcessInstanceId)){
//            ProcessDefinitionTask task = processDefinitionTaskService.getOne(new QueryWrapper<ProcessDefinitionTask>().eq("task_type","bpmn:startTask").notLike("task_name","发起人处理").eq("process_definition_id",processDefinitionId));
//            if(ObjectUtil.isNotEmpty(task.getHideItemLabels())) {
//                hideFormItemArr = task.getHideItemLabels().split(",");
//                hideFormItemArrList.addAll(Arrays.asList(hideFormItemArr));
//            }
//        }
        List<FormTemplateVO> treeList = Lists.newArrayList();
        //<templateID,templateVO>:只是方法中用于查找方便设的
        Map<Integer, FormTemplateVO> map = Maps.newHashMap();
        //字段组类型的template<template.Label(字段组名),templateId>，作用同上
        Map<String, Integer> groupMap = Maps.newHashMap();

        for (ProcessFormTemplate processFormTemplate : initList) {//组装两类map
            //20221103 隐藏字段功能：暂只扫描“基本类型”的formItem; 20221120 添加"隔离符"（别忘了下面还有段代码也需要一样的处理）
           //20221217 增加表类型隐藏
            if(CollUtil.isNotEmpty(hideFormItemArrList)) {
                if ((processFormTemplate.getFlag().equals("表类型") || processFormTemplate.getFlag().equals("基本类型") || processFormTemplate.getFlag().equals("隔离符")) && hideFormItemArrList.contains(processFormTemplate.getLabel()))
                    continue;
            }
            FormTemplateVO formTemplateVO = new FormTemplateVO();
            BeanUtils.copyProperties(processFormTemplate, formTemplateVO);
            //20220918todo 这里添判断下(processFormTemplate.type===下拉单选不可编辑||下拉单选可编辑)&&select变更字段的value值(选项字段)是不是要设置数据源，是的话，做下处理
            if(StrUtil.isNotEmpty(formTemplateVO.getDatasource())){
                ProcessDefinition proDef = processDefinitionService.getById(formTemplateVO.getProcessDefinitionId());
                //注意formTemplateVO.getLabel().contains("选择角色")：可能有“选择角色1/2/3”(新用户入网流程就是这样)
                String datasourceStr = "";
                if((proDef.getProcessName().contains("应用系统用户新增")  || proDef.getProcessName().contains("新用户") || proDef.getProcessName().contains("应用系统用户角色变更")) && formTemplateVO.getLabel().contains("初始角色") && formTemplateVO.getType().contains("选")){
                    List<SysDic> list = sysDicService.list(new QueryWrapper<SysDic>().eq("status","角色"));
                    if(CollUtil.isNotEmpty(list)){
                        Map<String,String> map1 = new HashMap<>();
                        for(SysDic s : list){
                            map1.put(s.getFlag(),s.getName());
                        }
                        datasourceStr = JSONObject.toJSONString(map1);//20221103回看：把map转成了jsonString传给前端，可能map类型不能传到前端（或被转成对象数组？）：有时间再研
                    }
                } else if(proDef.getProcessName().contains("服务器操作") && formTemplateVO.getLabel().contains("服务器名称") && formTemplateVO.getType().contains("选")){
                    List<AsDeviceCommon> serverList = asDeviceCommonService.list(new QueryWrapper<AsDeviceCommon>().eq("type_id",8).eq("net_type","国密网").eq("state","在用"));
                    if(CollUtil.isNotEmpty(serverList)){
                        datasourceStr = serverList.stream().map(s -> s.getName() + "(" + s.getIp() + ")").collect(Collectors.joining(","));
                    }
                }
                if(ObjectUtil.isNotEmpty(datasourceStr))
                    formTemplateVO.setDatasource(datasourceStr);
            }
            map.put(formTemplateVO.getId(), formTemplateVO);

            if (formTemplateVO.getFlag().equals("字段组类型")) {
                groupMap.put(formTemplateVO.getLabel(), formTemplateVO.getId());
            }
        }
        //20221117针对“重装操作系统/硬件维修”这种“被授权”流程：读“前置流程实例”的value1,并赋给现流程实例的相关template的value值（作为其默认值）
        //限制授权发起流程的发起节占;好像没限制住是发起节点：后续再加强判断条件todo
        if(processDefinition.getStartLimitedByCheck().equals("是") && ObjectUtil.isNotEmpty(actProcessInstanceId) &&  !actProcessInstanceId.equals("-1") ){
            ProcessInstanceData preProInstData = processInstanceDataService.getOne(new QueryWrapper<ProcessInstanceData>().eq("act_process_instance_id",actProcessInstanceId));
            ProcessFormValue1 preValue1 = processFormValue1Service.getOne(new QueryWrapper<ProcessFormValue1>().eq("act_process_instance_id",actProcessInstanceId));
            String value1_str = preValue1.getValue();
            JSONObject jsonObject = JSONObject.parseObject(value1_str);
            System.out.println(jsonObject);
            //20221121 todo利用这个map给这些option填充数据：todo记录在日志中
            //注意这里要查“老流程”的map
            Map<String, String>  labelIdMapForItemByAjax = this.getLabelIdMapForItemByAjax(preProInstData.getProcessDefinitionId(),null);
            //读取并组装开关变量
            //对于授权发起流程：再遍历一遍，给相应formitem赋初值；20230223这段代码注释掉（因为维修流程里不引用前置流程的开关量了）：暂不删除
//            for (Map.Entry<Integer, FormTemplateVO> entry : map.entrySet()) {
//                if(entry.getValue().getLabel().contains("更换硬盘")) {
//                    String itemId = labelIdMapForItemByAjax.get("更换硬盘");
//                    if(ObjectUtil.isNotEmpty(itemId)){
//                        entry.getValue().setDefaultValue(jsonObject.getString(itemId));
//                    }
//                } else if(entry.getValue().getLabel().contains("更换网卡")) {
//                    String itemId = labelIdMapForItemByAjax.get("更换网卡");
//                    if(ObjectUtil.isNotEmpty(itemId)){
//                        entry.getValue().setDefaultValue(jsonObject.getString(itemId));
//                    }
//                } else if(entry.getValue().getLabel().contains("更换内存")) {
//                    String itemId = labelIdMapForItemByAjax.get("更换内存");
//                    if(ObjectUtil.isNotEmpty(itemId)){
//                        entry.getValue().setDefaultValue(jsonObject.getString(itemId));
//                    }
//                }  else if(entry.getValue().getLabel().contains("重装操作系统")) {
//                    String itemId = labelIdMapForItemByAjax.get("重装操作系统");
//                    if(ObjectUtil.isNotEmpty(itemId)){
//                        entry.getValue().setDefaultValue(jsonObject.getString(itemId));
//                    }
//                } else if(entry.getValue().getLabel().contains("出所维修")) {
//                    String itemId = labelIdMapForItemByAjax.get("出所维修");
//                    if(ObjectUtil.isNotEmpty(itemId)){
//                        entry.getValue().setDefaultValue(jsonObject.getString(itemId));
//                    }
//                }
//            }


        }


        for (ProcessFormTemplate processFormTemplate : initList) {//又遍历了一遍，只为找到“森林”根/(第一层)结点List:可以优化:至少这个循环里应遍历processFormTemplateVOList的，当然前面那个map类型也需要调整：暂不研
            //20221103 隐藏字段功能：暂只扫描“基本类型”的formItem；20221120添加隔离符
            if(CollUtil.isNotEmpty(hideFormItemArrList)) {
                if ((processFormTemplate.getFlag().equals("表类型") || processFormTemplate.getFlag().equals("基本类型")|| processFormTemplate.getFlag().equals("隔离符")) && hideFormItemArrList.contains(processFormTemplate.getLabel()))
                    continue;
            }
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
