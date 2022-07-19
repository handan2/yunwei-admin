package com.sss.yunweiadmin.controller;


import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.sss.yunweiadmin.common.result.ResponseResultWrapper;
import com.sss.yunweiadmin.common.utils.ProcessFormCustomTypeUtil;
import com.sss.yunweiadmin.model.entity.*;
import com.sss.yunweiadmin.model.vo.ProcessDefinitionVO;
import com.sss.yunweiadmin.model.vo.TreeSelectVO;
import com.sss.yunweiadmin.model.vo.ValueLabelVO;
import com.sss.yunweiadmin.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 功能：用于保存、编辑、浏览 流程定义信息，自定义表单，流程设计
 *
 * @author 任勇林
 * @since 2021-03-21
 */
@RestController
@RequestMapping("/processDefinition")
@ResponseResultWrapper
public class ProcessDefinitionController {
    @Autowired
    ProcessDefinitionService processDefinitionService;
    @Autowired
    SysDicService sysDicService;
    @Autowired
    ProcessFormCustomTypeService processFormCustomTypeService;
    @Autowired
    ProcessFormTemplateService processFormTemplateService;
    @Autowired
    ProcessDefinitionTaskService processDefinitionTaskService;
    @Autowired
    ProcessDefinitionEdgeService processDefinitionEdgeService;
    @Autowired
    HttpSession httpSession;


    @GetMapping("getOneCustomTableIdByProcDefId")//20220702加,用于ProcessFormForEndAndStart.jsx
    public int getOneCustomTableIdByProcDefId(Integer processDefId) {
        //只取一个：约定审批发起的流程只能有一个自定义表
        ProcessFormTemplate templateForCustomType =  processFormTemplateService.list(new QueryWrapper<ProcessFormTemplate>().eq("flag","表类型").eq("process_definition_id", processDefId)).get(0);
        if(ObjectUtil.isEmpty(templateForCustomType))
            throw new RuntimeException("该流程没有配置自定义表，请联系管理员！");
       String [] a = templateForCustomType.getType().split("\\.");
        return  Integer.parseInt(a[0]);

    }

    //获取所有下拉类型
    @GetMapping("getTypeVL")
    public List<ValueLabelVO> getType() {
        List<SysDic> list1 = sysDicService.list(new QueryWrapper<SysDic>().eq("flag", "下拉类型").orderByAsc("sort"));
        List<ProcessFormCustomType> list2 = processFormCustomTypeService.list(new QueryWrapper<ProcessFormCustomType>().eq("status", "正常").orderByAsc("sort"));
        List<ValueLabelVO> result1 = list1.stream().map(item -> new ValueLabelVO(item.getName(), item.getName())).collect(Collectors.toList());
        List<ValueLabelVO> result2 = list2.stream().map(item -> new ValueLabelVO(item.getId() + "." + item.getName(), item.getName())).collect(Collectors.toList());
        result1.addAll(result2);
        return result1;
    }

    //获取基本类型
    @GetMapping("getBaseTypeVL")
    public List<ValueLabelVO> getBaseType() {
        List<SysDic> list = sysDicService.list(new QueryWrapper<SysDic>().eq("flag", "下拉类型").orderByAsc("sort"));
        List<ValueLabelVO> result = list.stream().map(item -> new ValueLabelVO(item.getName(), item.getName())).collect(Collectors.toList());
        return result;
    }

    /**
     * tableNameArr=[16.计算机信息表，xx.yyyy]
     * 返回“自定义表类型/custormType”构成的森林（用于自定表单设计时设置变更字段时要用到的的下拉选择内容）：其实就是二次结构，一级是表名，二级是属性
     * 采取“链表”结构存储“二级结构”；这种结构只记录其头部/根结点即可，因为是多链，其头部结点构成List
     * @author 任勇林/注释
     * @since 2022-04-28
     */

    @GetMapping("getTreeByTableNames")
    public List<TreeSelectVO> getTreeByTableNames(String[] tableNameArr) {
        List<TreeSelectVO> treeList = Lists.newArrayList();
        List<Integer> tableIdList = Arrays.stream(tableNameArr).map(item -> Integer.parseInt(item.split("\\.")[0])).collect(Collectors.toList());
        List<ProcessFormCustomType> list = processFormCustomTypeService.list(new QueryWrapper<ProcessFormCustomType>().in("id", tableIdList).eq("status", "正常").orderByAsc("sort"));
        for (ProcessFormCustomType processFormCustomType : list) {
            TreeSelectVO parent = new TreeSelectVO();
            parent.setTitle(processFormCustomType.getName());
            parent.setValue(processFormCustomType.getName());
            parent.setKey(processFormCustomType.getName());

            String props = processFormCustomType.getProps();
            Map<String, List<AsConfig>> map = ProcessFormCustomTypeUtil.parseProps2(props);
            for (Map.Entry<String, List<AsConfig>> entry : map.entrySet()) {
                String enTableName = entry.getKey();
                for (AsConfig asConfig : entry.getValue()) {
                    TreeSelectVO child = new TreeSelectVO();
                    child.setTitle(asConfig.getZhColumnName());
                    child.setValue(processFormCustomType.getName() + "." + asConfig.getZhColumnName());
                    child.setKey(processFormCustomType.getId() + "." + processFormCustomType.getName() + "." + enTableName + "." + asConfig.getEnColumnName() + "," + asConfig.getType());
                    if (parent.getChildren() != null) {
                        parent.getChildren().add(child);
                    } else {
                        parent.setChildren(Lists.newArrayList(child));
                    }
                }
            }
            treeList.add(parent);
        }

        return treeList;
    }

    //20211114
    private boolean processVisable(String idListString) {
        List<String> definitionRoleIdList = Stream.of(idListString.split(",")).collect(Collectors.toList());
        List<Integer> definitionRoleIdListToInt = definitionRoleIdList.stream().map(value -> Integer.valueOf(value)).collect(Collectors.toList());
        //获取用户的roleIDList
        SysUser user = (SysUser) httpSession.getAttribute("user");
        if (ObjectUtil.isNotEmpty(user)) {
            List<Integer> roleIdList = user.getRoleIdList();
            definitionRoleIdListToInt.retainAll(roleIdList);
            if (ObjectUtil.isNotEmpty(definitionRoleIdListToInt)) {
                return true;
            } else
                return false;

        }
        //默许没登陆时，先显示全部吧
        return true;
    }
    //20220626加,L和V一样：都是Label
    @GetMapping("getProcessDefLV")
    public List<ValueLabelVO> getProcessDefLV() {
       List<ProcessDefinition> list  = processDefinitionService.list(new QueryWrapper<ProcessDefinition>().eq("status","启用").eq("have_display","是"));
       // Map<String,Integer> map = list.stream().collect(Collectors.toMap(ProcessDefinition::getProcessName,ProcessDefinition::getId));
        return list.stream().map(item->new ValueLabelVO(item.getProcessName(),item.getProcessName())).collect(Collectors.toList());
    }
    @GetMapping("list")
    public IPage<ProcessDefinition> list(int currentPage, int pageSize, String processName, String processType) {
        QueryWrapper<ProcessDefinition> queryWrapper = new QueryWrapper<ProcessDefinition>().eq("have_display", "是").orderByAsc("sort");
        if (ObjectUtil.isNotEmpty(processName)) {
            queryWrapper.like("process_name", processName);
        }
        if (ObjectUtil.isNotEmpty(processType)) {
            queryWrapper.like("process_type", processType);
        }
        // 20211114查了两遍DB;20220715 这个ProcessDefiniton/role_id应改成 role_idlist
        List<ProcessDefinition> processDefinitionListLegal =processDefinitionService.list().stream().filter(item -> this.processVisable(item.getRoleId())).collect(Collectors.toList());
        //第一遍，取出合法授权defIdList，第二遍读Page
        List<Integer> processDefinitionIdListLegal = processDefinitionListLegal.stream().map(item -> item.getId()).collect(Collectors.toList());
        if (ObjectUtil.isNotEmpty(processDefinitionIdListLegal)) {

            queryWrapper.in("id", processDefinitionIdListLegal);
        }
        //只读启用状态的流程定义
        queryWrapper.eq("status","启用");
        IPage<ProcessDefinition> page1 = processDefinitionService.page(new Page<>(currentPage, pageSize), queryWrapper);
        page1.getRecords().forEach(item -> item.setBpmnXml(""));
        return page1;
    }

    @GetMapping("get")
    public ProcessDefinition getById(String processDefinitionId) {
        return processDefinitionService.getById(processDefinitionId);
    }
    @GetMapping("getByName")
    public ProcessDefinition getByName(String processDefinitionName) {
        List<ProcessDefinition> list = processDefinitionService.list(new QueryWrapper<ProcessDefinition>().like("process_name",processDefinitionName));
        if(CollUtil.isNotEmpty(list))
            return list.get(0);
        else
            return null;
    }
    @PostMapping("add")
    public boolean add(@RequestBody ProcessDefinitionVO processDefinitionVO) {
        return processDefinitionService.add(processDefinitionVO);
    }

    //获取流程定义时的3个model/map 数据
    @GetMapping("getProcessDefinitionVO")
    public ProcessDefinitionVO getProcessDefinitionVO(String processDefinitionId) {
        ProcessDefinitionVO processDefinitionVO = new ProcessDefinitionVO();

        ProcessDefinition processDefinition = processDefinitionService.getById(processDefinitionId);

        List<ProcessFormTemplate> formList = processFormTemplateService.list(new QueryWrapper<ProcessFormTemplate>().eq("process_definition_id", processDefinitionId));
        LinkedHashMap<String, ArrayList<ProcessFormTemplate>> formTemplateMap = new LinkedHashMap<>();
        formTemplateMap.put("firstData", new ArrayList<>());
        for (ProcessFormTemplate processFormTemplate : formList) {
            if ((processFormTemplate.getFlag().equals("基本类型") || processFormTemplate.getFlag().equals("表类型") || processFormTemplate.getFlag().equals("字段变更类型")) && Strings.isNullOrEmpty(processFormTemplate.getGroupParentLabel())) {
                formTemplateMap.get("firstData").add(processFormTemplate);
            } else if (processFormTemplate.getFlag().equals("字段组类型")) {
                formTemplateMap.get("firstData").add(processFormTemplate);
            } else {
                if (formTemplateMap.get(processFormTemplate.getGroupParentLabel()) != null) {
                    formTemplateMap.get(processFormTemplate.getGroupParentLabel()).add(processFormTemplate);
                } else {
                    ArrayList<ProcessFormTemplate> list = Lists.newArrayList();
                    list.add(processFormTemplate);
                    formTemplateMap.put(processFormTemplate.getGroupParentLabel(), list);
                }
            }
        }
        List<ProcessDefinitionTask> taskList = processDefinitionTaskService.list(new QueryWrapper<ProcessDefinitionTask>().eq("process_definition_id", processDefinitionId));
        List<ProcessDefinitionEdge> edgeList = processDefinitionEdgeService.list(new QueryWrapper<ProcessDefinitionEdge>().eq("process_definition_id", processDefinitionId));

        processDefinitionVO.setProcessDefinition(processDefinition);
        processDefinitionVO.setFormTemplateMap(formTemplateMap);
        processDefinitionVO.setTaskList(taskList);
        processDefinitionVO.setEdgeList(edgeList);

        return processDefinitionVO;
    }

    @PostMapping("edit")
    public boolean edit(@RequestBody ProcessDefinitionVO processDefinitionVO) {
        return processDefinitionService.edit(processDefinitionVO);
    }

    @GetMapping("delete")
    public boolean delete(Integer processDefinitionId) {
        return processDefinitionService.delete(processDefinitionId);
    }

    @GetMapping("copy")
    public boolean copy(Integer processDefinitionId) {
        return processDefinitionService.copy(processDefinitionId);
    }
}