package com.sss.yunweiadmin.controller;


import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.sss.yunweiadmin.common.result.ResponseResultWrapper;
import com.sss.yunweiadmin.common.utils.ProcessFormCustomTypeUtil;
import com.sss.yunweiadmin.common.utils.SpringUtil;
import com.sss.yunweiadmin.common.utils.TreeUtil;
import com.sss.yunweiadmin.model.entity.*;
import com.sss.yunweiadmin.model.vo.*;
import com.sss.yunweiadmin.service.*;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * <p>
 * 自定义表单模板 前端控制器
 * </p>
 *
 * @author 任勇林
 * @since 2021-04-14
 */
@RestController
@RequestMapping("/processFormTemplate")
@ResponseResultWrapper
public class ProcessFormTemplateController {
    @Autowired
    private ProcessFormTemplateService processFormTemplateService;
    @Autowired
    private ProcessFormCustomTypeService processFormCustomTypeService;
    @Autowired
    private AsConfigService asConfigService;
    @Autowired
    private AsTypeService asTypeService;
    @Autowired
    private AsDeviceCommonService asDeviceCommonService;
    @Autowired
    private ProcessDefinitionService processDefinitionService;




    //20220521加， 应该已不用
    @GetMapping("getGroupKT")
    public List<KeyTitleVO> getGroupKT(Integer processDefinitionId) {
        List<ProcessFormTemplate> list = processFormTemplateService.list(new QueryWrapper<ProcessFormTemplate>().eq("process_definition_id", processDefinitionId).eq("type", "字段组"));
        return list.stream().map(item -> new KeyTitleVO(item.getId(), item.getLabel())).collect(Collectors.toList());
    }

    /**
     * 20211201这个是发起流程实例时获取所有的temploate记录（并以id/label/name/type/flag属性组装成基本成员）返回给页面的数据
     * 参考这里解析template表的逻辑新写一个action来传变更字段相应的map（id/label）集合返回给前端:已
     */

    @GetMapping("getFormTemplateTree")
    public List<FormTemplateVO> getFormTemplateTree(Integer processDefinitionId,String actProcessInstanceId) {//20221103 添加Integer actProcessInstanceId


        List<ProcessFormTemplate> list = processFormTemplateService.list(new QueryWrapper<ProcessFormTemplate>().eq("process_definition_id", processDefinitionId));
        return processFormTemplateService.getFormTemplateTree(list,processDefinitionId,actProcessInstanceId);
        // return TreeUtil.getFormTemplateTree(list);
    }

    //获取表单template对应的变更字段/基本字段/自定义表字段 的（label/ID）map;如果是自定义表的字段：label名前加'table_',变更字段：label名直接是“涉密级别”这种（前面没有自定义表名）
    @GetMapping("getLabelIdMapForItemByAjax")
    public Map<String, String> getLabelIdMapForItemByAjax(Integer processDefinitionId, String hideGroupIds) {
        return processFormTemplateService.getLabelIdMapForItemByAjax(processDefinitionId,hideGroupIds);
    }

    //用于给tree组件赋值:获得需要在提交时可供界面上选择来显示的group
    @GetMapping("getFormTemplateGroupTreeForSelect")
    public List<TreeSelectVO> getFormTemplateGroupTreeForSelect(Integer processDefinitionId) {
        List<ProcessFormTemplate> list1 = processFormTemplateService.list(new QueryWrapper<ProcessFormTemplate>().eq("process_definition_id", processDefinitionId).eq("type", "字段组"));
        //20220517todo要判断空；20221202先注释掉
//        if (CollUtil.isEmpty(list1)) {
//            throw new RuntimeException("自定义字段为空，该流程定义可能不存在");
//        }
        Map<String, String> map1 = list1.stream().collect(Collectors.toMap(ProcessFormTemplate::getLabel, ProcessFormTemplate::getHaveGroupSelect));

        List<ProcessFormTemplate> list2 = processFormTemplateService.list(new QueryWrapper<ProcessFormTemplate>().eq("process_definition_id", processDefinitionId).eq("type", "字段组").eq("have_group_select", "是"));
        Map<String, Integer> map2 = list2.stream().collect(Collectors.toMap(ProcessFormTemplate::getLabel, ProcessFormTemplate::getId));
        //
        List<TreeDTO> list3 = Lists.newArrayList();
        for (ProcessFormTemplate processFormTemplate : list2) {
            if (!Strings.isNullOrEmpty(processFormTemplate.getGroupParentLabel())) {
                //有父字段组,判断父字段组的haveGroupSelect
                String groupParentLabel = processFormTemplate.getGroupParentLabel();
                String haveGroupSelect = map1.get(groupParentLabel);
                if (haveGroupSelect.equals("是")) {
                    TreeDTO treeDTO = new TreeDTO();
                    treeDTO.setId(processFormTemplate.getId());
                    treeDTO.setName(processFormTemplate.getLabel());
                    treeDTO.setPid(map2.get(processFormTemplate.getGroupParentLabel()));
                    list3.add(treeDTO);
                }
            } else {
                TreeDTO treeDTO = new TreeDTO();
                treeDTO.setId(processFormTemplate.getId());
                treeDTO.setName(processFormTemplate.getLabel());
                treeDTO.setPid(0);
                list3.add(treeDTO);
            }
        }
        if (CollUtil.isEmpty(list3)) {
            return Lists.newArrayList();
        } else {
            return TreeUtil.getTreeSelectVO(list3);
        }
    }

    //根据已选择的字段组id,筛选出所有需要被显示的(这里指嵌套在选定字段组内的其他字段组)字段组id 。。。包含：用户界面可选字段组且已选择的（对应checkGroupIdArr）&&不可选字段组
    @GetMapping("getSelectGroupIdList")
    public Set<Integer> getSelectGroupIdList(Integer processDefinitionId, Integer[] checkGroupIdArr) {
        List<ProcessFormTemplate> list = processFormTemplateService.list(new QueryWrapper<ProcessFormTemplate>().eq("process_definition_id", processDefinitionId).eq("type", "字段组"));
        Map<String, ProcessFormTemplate> map = list.stream().collect(Collectors.toMap(ProcessFormTemplate::getLabel, ProcessFormTemplate -> ProcessFormTemplate));
        //20220704 实际这个checkGroupIdArr是应该判下空，不过Action机制：前端传null到actioon时：也会初始化一个“空对象”
        Set<Integer> selectGroupIdSet = Stream.of(checkGroupIdArr).collect(Collectors.toSet());
        System.out.println(selectGroupIdSet.size());
        //先将 have_group_select=否 放入checkGroupIdArr
        selectGroupIdSet.addAll(list.stream().filter(item -> item.getHaveGroupSelect().equals("否")).map(ProcessFormTemplate::getId).collect(Collectors.toSet()));
        //根据checkGroupIdArr，继续找出有父子关系的需要显示的父id
        List<ProcessFormTemplate> list2 = list.stream().filter(item -> selectGroupIdSet.contains(item.getId())).collect(Collectors.toList());
        for (ProcessFormTemplate processFormTemplate : list2) {
            String groupParentLabel = processFormTemplate.getGroupParentLabel();
            if (!Strings.isNullOrEmpty(groupParentLabel)) {
                try {
                    ProcessFormTemplate tmp = map.get(groupParentLabel);
                    selectGroupIdSet.add(tmp.getId());
                    //继续向上寻找
                    String groupParentLabel2 = tmp.getGroupParentLabel();
                    while (true) {
                        ProcessFormTemplate tmp2 = map.get(groupParentLabel2);
                        if (tmp2 != null) {
                            selectGroupIdSet.add(tmp2.getId());
                            groupParentLabel2 = tmp2.getGroupParentLabel();
                        } else {
                            break;
                        }
                    }
                } catch (Exception e) {
                    throw new RuntimeException("此处代码有错误。。。");
                }
            }
        }
        return selectGroupIdSet;
    }

    //用于自定义表相关字段的渲染;格式：<自定义表的ID,对应的字段VOList<VO: "label": "计算机信息表.设备编号","name": "16.计算机信息表.as_device_common.no.75">>//自定义表的记录相应源表/字段的那个字段叫props:里面是json格式;
    //示例中最后那个“75”代表as_config中的对应ID
    //asConfigController也有同名getTableTypeVO方法
    @GetMapping("getTableTypeVO")
    public Map<Integer, List<TableTypeVO>> getTableTypeVO(Integer processDefinitionId) {
        return processFormTemplateService.getTableTypeVO(processDefinitionId);
//        Map<Integer, List<TableTypeVO>> map = Maps.newTreeMap();
//        //1.取出所有的表类型的名称
//        List<ProcessFormTemplate> list = processFormTemplateService.list(new QueryWrapper<ProcessFormTemplate>().eq("process_definition_id", processDefinitionId));
//        List<Integer> tableIdList = list.stream().filter(item -> item.getFlag().equals("表类型")).map(item -> {
//            String tableId = item.getType().split("\\.")[0];
//            return Integer.parseInt(tableId);
//        }).collect(Collectors.toList());
//        if (CollUtil.isNotEmpty(list) && CollUtil.isNotEmpty(tableIdList)) {
//            //2.根据表名称取出processFormCustomType
//            List<ProcessFormCustomType> typeList = processFormCustomTypeService.list(new QueryWrapper<ProcessFormCustomType>().in("id", tableIdList));
//            //3.
//            for (ProcessFormCustomType processFormCustomType : typeList) {
//                List<TableTypeVO> tmpList = Lists.newArrayList();
//
//                String props = processFormCustomType.getProps();
//                Map<String, List<AsConfig>> tmpMap = ProcessFormCustomTypeUtil.parseProps(props);//String代表英文table名称
//                tmpMap.entrySet().stream().forEach(item -> {
//                    item.getValue().forEach(item2 -> {
//                        TableTypeVO tableTypeVO = new TableTypeVO();
//                        tableTypeVO.setLabel(processFormCustomType.getName() + "." + item2.getZhColumnName());
//                        tableTypeVO.setName(processFormCustomType.getId() + "." + processFormCustomType.getName() + "." + item.getKey() + "." + item2.getEnColumnName() + "." + item2.getId());
//                        tmpList.add(tableTypeVO);
//                    });
//                });
//
//                map.put(processFormCustomType.getId(), tmpList);
//            }
//        }
//        return map;
    }

    //20211129获取流程实例中自定义表各字段的相应实例数据; 格式<"16.计算机信息表.as_device_common.no.75","J0601111">；这个函数只有在流程发起时选择资产后的填充相应字段时被调用
    //20220408这个方法和ProcessFormCustomInstController的设置初衷有关联，todo后续研究需要不需要整合
    //20220616这个方法只有在流程中“选择资产”时执行，纯ajax处理函数
    @GetMapping("getTableTypeInstData")
    public TableTypeInstDataVO getTableTypeInstData(Integer customTableId, Integer asDeviceCommonId, Integer processDefinitionId) {
        TableTypeInstDataVO VO = new TableTypeInstDataVO();
        Map<String, String> map = Maps.newTreeMap();
        List<DiskForHisForProcess> diskList = null;//20220621 List<AsDeviceCommon改造成 List<DiskForHisForProcess>
        List<TableTypeVO> listForTableTypeVO = this.getTableTypeVO(processDefinitionId).get(customTableId);//20220408getTableTypeVO:返回 Map<Integer, List<TableTypeVO>>；map也能用get方法
        for (TableTypeVO item : listForTableTypeVO) {
            //1.资产哦.as_device_common.no.1
            String name = item.getName();//"name": "16.计算机信息表.as_device_common.no.75"
            String[] arr = name.split("\\.");
            if ("disk_for_render".equals(arr[3])) {//20220616加：自定义字段有“硬盘渲染标识时”给前端传disk数据，还是放前端吧
                List<AsDeviceCommon> list = asDeviceCommonService.list(new QueryWrapper<AsDeviceCommon>().eq("host_as_id", asDeviceCommonId).like("name", "硬盘"));
                if (list != null) {
                    String hostAsNo = asDeviceCommonService.getById(asDeviceCommonId).getNo();
                    diskList = list.stream().map(item1 -> {
                        DiskForHisForProcess itemNew = new DiskForHisForProcess();
                        BeanUtils.copyProperties(item1, itemNew);
                        itemNew.setAsId(item1.getId());
                        itemNew.setHostAsId(item1.getHostAsId());
                        itemNew.setHostAsNo(hostAsNo);
                        itemNew.setId(null);
                        return itemNew;
                    }).collect(Collectors.toList());
                }

            }
            String serviceName = StrUtil.toCamelCase(arr[2]) + "ServiceImpl";
            IService service = (IService) SpringUtil.getBean(serviceName);
            Object obj = null;
            if (arr[2].equals("as_device_common")) {
                obj = service.getById(asDeviceCommonId);
            } else {
                obj = service.getOne(new QueryWrapper<Object>().eq("as_id", asDeviceCommonId));
            }
            if (obj != null) {
                //读实例中相应自定义表的具体字段值：这里的toString()是把所有字段不管啥类型都转成string
                Object o = ReflectUtil.getFieldValue(obj, StrUtil.toCamelCase(arr[3]));
                if (ObjectUtil.isNotEmpty(o)) {//资产表中不能有NULL值否则会报错:这里加了判断
                    String value = o.toString();
                    if (!Strings.isNullOrEmpty(value)) {
                        if ("type_id".equals(arr[3])) {
                            String typeName = asTypeService.getById(Integer.valueOf(value)).getName();
                            map.put(name, typeName);
                        } else
                            map.put(name, value);
                    }
                }
            }
        }

        VO.setMap(map);
        VO.setDiskList(diskList);
        return VO;
    }
}
