package com.sss.yunweiadmin.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.common.base.Strings;
import com.sss.yunweiadmin.bean.WorkFlowBean;
import com.sss.yunweiadmin.mapper.ProcessDefinitionMapper;
import com.sss.yunweiadmin.model.entity.*;
import com.sss.yunweiadmin.model.vo.ProcessDefinitionVO;
import com.sss.yunweiadmin.service.*;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 * 流程定义时的基本表，用于保存 流程名称，自定义表单布局 服务实现类
 * </p>
 *
 * @author 任勇林
 * @since 2021-09-03
 */
@Service
public class ProcessDefinitionServiceImpl extends ServiceImpl<ProcessDefinitionMapper, ProcessDefinition> implements ProcessDefinitionService {
    @Autowired
    ProcessFormTemplateService processFormTemplateService;
    @Autowired
    ProcessDefinitionTaskService processDefinitionTaskService;
    @Autowired
    ProcessDefinitionEdgeService processDefinitionEdgeService;
    @Autowired
    ProcessInstanceDataService processInstanceDataService;
    @Autowired
    WorkFlowBean workFlowBean;

    @Override
    public boolean add(ProcessDefinitionVO processDefinitionVO) {
        boolean flag1, flag2, flag3, flag4;
        ProcessDefinition processDefinition = processDefinitionVO.getProcessDefinition();
        processDefinition.setHaveDisplay("是");
        //将流程名称中的空格符 去掉
        processDefinition.setProcessName(processDefinition.getProcessName().replaceAll("\\s|\\t", ""));
        //20220426 解决流程实例被删除后，将对应的流程定义编辑后产生的新定义仍具有deployID/在发起流程实例时被误识别成已部署
        processDefinition.setDeployId(null);
        List<ProcessFormTemplate> formTemplateList = processDefinitionVO.getFormTemplateList();
        List<ProcessDefinitionTask> taskList = processDefinitionVO.getTaskList();
        List<ProcessDefinitionEdge> edgeList = processDefinitionVO.getEdgeList();
        flag1 = this.save(processDefinition);

        //存储hideGroupIDS/labels ;
        formTemplateList.forEach(item -> {
            item.setProcessDefinitionId(processDefinition.getId());
            //value的中文逗号 转为 英文逗号
            if (!Strings.isNullOrEmpty(item.getValue())) {
                item.setValue(item.getValue().replaceAll("，", ","));
            }
        });
        flag2 = processFormTemplateService.saveBatch(formTemplateList);
        //20220526 只能遍历两遍formTemplateList：只有保存DB后才有id值
        Map<String, String> groupLabelIdStringMap = new HashMap<>();
        formTemplateList.forEach(item -> {
            //202000526
            if ("字段组类型".equals(item.getFlag())) {
                groupLabelIdStringMap.put(item.getLabel(), item.getId().toString());
            }
        });
        /* 20220525在这后面添加tasklist里hideGroupIds字段的更新:根据labelList来更新idList:因为idList会经常变（）
        好像有一个问题，新建流程定义时，template并没有写入DB，那么hideGroupIDS对应的穿梭框数据来源就不能是DB了，应该是map;
        对于修改时：无论是有实例的修改（会把老template数据删除）还是保留老的template再新建template数据，原先记录的hideGroupIDS都需要更新*/
        taskList.forEach(item -> {
            if (ObjectUtil.isNotEmpty(item.getHideGroupLabel())) {
                String[] hideGroupLabelArr = item.getHideGroupLabel().split(",");
                List<String> newHideGroupLabelStringList = new ArrayList<>();
                List<String> newHideGroupIdStringList = new ArrayList<>();
                item.setHideGroupIds("");//先清空
                item.setHideGroupLabel("");
                Arrays.stream(hideGroupLabelArr).forEach(tmp -> {
                    if (ObjectUtil.isNotEmpty(groupLabelIdStringMap.get(tmp))) {//过滤掉那些已经"过时"的groupID/label
                        newHideGroupIdStringList.add(groupLabelIdStringMap.get(tmp));
                        newHideGroupLabelStringList.add(tmp);
                    }
                });
                item.setHideGroupIds(newHideGroupIdStringList.stream().collect(Collectors.joining(",")));
                item.setHideGroupLabel(newHideGroupLabelStringList.stream().collect(Collectors.joining(",")));
            }
            item.setProcessDefinitionId(processDefinition.getId());//给新增的task定义指定definitionID
        });


        flag3 = processDefinitionTaskService.saveBatch(taskList);

        if (ObjectUtil.isNotEmpty(edgeList)) {
            edgeList.forEach(item -> {
                item.setProcessDefinitionId(processDefinition.getId());
            });
            flag4 = processDefinitionEdgeService.saveBatch(edgeList);
        } else {
            flag4 = true;
        }

        return flag1 && flag2 && flag3 && flag4;
    }

    @Override
    public boolean edit(ProcessDefinitionVO processDefinitionVO) {//流程定义编辑后都会导致新增一条流程定义记录：故最后都会调用this.adds()
        Integer processDefinitionId = processDefinitionVO.getProcessDefinition().getId();
        List<ProcessInstanceData> list = processInstanceDataService.list(new QueryWrapper<ProcessInstanceData>().eq("process_definition_id", processDefinitionId));
        if (CollUtil.isEmpty(list)) {
            //先删除，process_definition_info、process_definition_task、process_definition_edge、process_form_template
            this.removeById(processDefinitionId);
            processDefinitionTaskService.remove(new QueryWrapper<ProcessDefinitionTask>().eq("process_definition_id", processDefinitionId));
            processDefinitionEdgeService.remove(new QueryWrapper<ProcessDefinitionEdge>().eq("process_definition_id", processDefinitionId));
            processFormTemplateService.remove(new QueryWrapper<ProcessFormTemplate>().eq("process_definition_id", processDefinitionId));
            //后插入
            return this.add(processDefinitionVO);
        } else {
            //根据processDefinitionId，更新一下processdefiniton
            ProcessDefinition processDefinition = this.getById(processDefinitionId);
            processDefinition.setHaveDisplay("否");
            this.updateById(processDefinition);
            //插入页面数据
            processDefinitionVO.getProcessDefinition().setId(null);
            processDefinitionVO.getProcessDefinition().setDeployId(null);//20220426todo断点
            processDefinitionVO.getProcessDefinition().setBeforeId(processDefinition.getId());
            if (processDefinition.getBaseId() == null) {
                //第一次修改
                processDefinitionVO.getProcessDefinition().setBaseId(processDefinition.getId());
            } else {
                //第二、三、N次修改
                processDefinitionVO.getProcessDefinition().setBaseId(processDefinition.getBaseId());
            }
            return this.add(processDefinitionVO);
        }
    }

    @Override
    public boolean delete(Integer processDefinitionId) {
        ProcessDefinition processDefinition = this.getById(processDefinitionId);
        String deployId = processDefinition.getDeployId();

        //deployId有值：代表曾发起过流程实例
        if (ObjectUtil.isNotEmpty(deployId)) {
            //20221105对有实例的流程定义名称标记下、界面上也不再让他显示
            processDefinition.setProcessName(processDefinition.getProcessName() + "(旧)");
            processDefinition.setHaveDisplay("否");
            processDefinition.setDeployId("");
            this.updateById(processDefinition);
            //对未完成的流程实例直接删除
            processInstanceDataService.remove(new QueryWrapper<ProcessInstanceData>().ne("process_status", "完成").eq("process_definition_id", processDefinitionId));
            List<ProcessInstanceData> dataList = processInstanceDataService.list(new QueryWrapper<ProcessInstanceData>().eq("process_status", "完成").eq("process_definition_id", processDefinitionId));
            if (CollUtil.isNotEmpty(dataList)) {//含有已完成实例
                for (ProcessInstanceData processInstanceData : dataList) {
                    processInstanceData.setActProcessInstanceId("");
                    processInstanceDataService.updateById(processInstanceData);
                }
                workFlowBean.deleteDeploy(deployId);
                return true;
            }
            workFlowBean.deleteDeploy(deployId);
        }
        //删除ProcessDefinition、ProcessDefinitionTask、ProcessDefinitionEdge、ProcessFormTemplate
        this.removeById(processDefinitionId);
        processDefinitionTaskService.remove(new QueryWrapper<ProcessDefinitionTask>().eq("process_definition_id", processDefinitionId));
        processDefinitionEdgeService.remove(new QueryWrapper<ProcessDefinitionEdge>().eq("process_definition_id", processDefinitionId));
        processFormTemplateService.remove(new QueryWrapper<ProcessFormTemplate>().eq("process_definition_id", processDefinitionId));
        //恢复老版本：20220817我觉得没必要：先注释这段逻辑
//        if (ObjectUtil.isNotEmpty(processDefinition.getBaseId())) {
//            //回退到上一个版本
//            Integer beforeId = processDefinition.getBeforeId();
//            ProcessDefinition beforeProcessDefinition = this.getById(beforeId);
//            beforeProcessDefinition.setHaveDisplay("是");
//            this.updateById(beforeProcessDefinition);
//        }

        return true;
    }

    private void copy(ProcessDefinition oldProcessDefinition) {
        //复制ProcessDefinition、ProcessDefinitionTask、ProcessDefinitionEdge、ProcessFormTemplate
        ProcessDefinition newProcessDefinition = new ProcessDefinition();
        BeanUtils.copyProperties(oldProcessDefinition, newProcessDefinition);
        newProcessDefinition.setId(null);
        this.save(newProcessDefinition);
        Integer newProcessDefinitionId = newProcessDefinition.getId();
        //
        List<ProcessDefinitionTask> oldTaskList = processDefinitionTaskService.list(new QueryWrapper<ProcessDefinitionTask>().eq("process_definition_id", oldProcessDefinition.getId()));
        List<ProcessDefinitionTask> newTaskList = new ArrayList<>();
        for (ProcessDefinitionTask oldProcessDefinitionTask : oldTaskList) {
            ProcessDefinitionTask newProcessDefinitionTask = new ProcessDefinitionTask();
            BeanUtils.copyProperties(oldProcessDefinitionTask, newProcessDefinitionTask);
            newProcessDefinitionTask.setProcessDefinitionId(newProcessDefinitionId);
            newTaskList.add(newProcessDefinitionTask);
        }
        processDefinitionTaskService.saveBatch(newTaskList);
        //
        List<ProcessDefinitionEdge> oldEdgeList = processDefinitionEdgeService.list(new QueryWrapper<ProcessDefinitionEdge>().eq("process_definition_id", oldProcessDefinition.getId()));
        if (ObjectUtil.isNotEmpty(oldEdgeList)) {
            List<ProcessDefinitionEdge> newEdgeList = new ArrayList<>();
            for (ProcessDefinitionEdge oldProcessDefinitionEdge : oldEdgeList) {
                ProcessDefinitionEdge newProcessDefinitionEdge = new ProcessDefinitionEdge();
                BeanUtils.copyProperties(oldProcessDefinitionEdge, newProcessDefinitionEdge);
                newProcessDefinitionEdge.setProcessDefinitionId(newProcessDefinitionId);
                newEdgeList.add(newProcessDefinitionEdge);
            }
            processDefinitionEdgeService.saveBatch(newEdgeList);
        }
        //
        List<ProcessFormTemplate> oldTemplateList = processFormTemplateService.list(new QueryWrapper<ProcessFormTemplate>().eq("process_definition_id", oldProcessDefinition.getId()));
        List<ProcessFormTemplate> newTemplateList = new ArrayList<>();
        for (ProcessFormTemplate oldProcessFormTemplate : oldTemplateList) {
            ProcessFormTemplate newProcessFormTemplate = new ProcessFormTemplate();
            BeanUtils.copyProperties(oldProcessFormTemplate, newProcessFormTemplate);
            newProcessFormTemplate.setProcessDefinitionId(newProcessDefinitionId);
            newTemplateList.add(newProcessFormTemplate);
        }
        processFormTemplateService.saveBatch(newTemplateList);
    }

    @Transactional
    @Override
    public boolean copy(Integer processDefinitionId) {
        ProcessDefinition oldProcessDefinition = this.getById(processDefinitionId);
        //复制ProcessDefinition、ProcessDefinitionTask、ProcessDefinitionEdge、ProcessFormTemplate
        ProcessDefinition newProcessDefinition = new ProcessDefinition();
        BeanUtils.copyProperties(oldProcessDefinition, newProcessDefinition);
        newProcessDefinition.setId(null);
        newProcessDefinition.setProcessName(oldProcessDefinition.getProcessName() + "-副本");
        newProcessDefinition.setBeforeId(null);
        newProcessDefinition.setBaseId(null);
        newProcessDefinition.setDeployId(null);
        this.save(newProcessDefinition);
        Integer newProcessDefinitionId = newProcessDefinition.getId();
        //
        List<ProcessDefinitionTask> oldTaskList = processDefinitionTaskService.list(new QueryWrapper<ProcessDefinitionTask>().eq("process_definition_id", oldProcessDefinition.getId()));
        List<ProcessDefinitionTask> newTaskList = new ArrayList<>();
        for (ProcessDefinitionTask oldProcessDefinitionTask : oldTaskList) {
            ProcessDefinitionTask newProcessDefinitionTask = new ProcessDefinitionTask();
            BeanUtils.copyProperties(oldProcessDefinitionTask, newProcessDefinitionTask);
            newProcessDefinitionTask.setProcessDefinitionId(newProcessDefinitionId);
            newTaskList.add(newProcessDefinitionTask);
        }
        processDefinitionTaskService.saveBatch(newTaskList);
        //
        List<ProcessDefinitionEdge> oldEdgeList = processDefinitionEdgeService.list(new QueryWrapper<ProcessDefinitionEdge>().eq("process_definition_id", oldProcessDefinition.getId()));
        if (ObjectUtil.isNotEmpty(oldEdgeList)) {
            List<ProcessDefinitionEdge> newEdgeList = new ArrayList<>();
            for (ProcessDefinitionEdge oldProcessDefinitionEdge : oldEdgeList) {
                ProcessDefinitionEdge newProcessDefinitionEdge = new ProcessDefinitionEdge();
                BeanUtils.copyProperties(oldProcessDefinitionEdge, newProcessDefinitionEdge);
                newProcessDefinitionEdge.setProcessDefinitionId(newProcessDefinitionId);
                newEdgeList.add(newProcessDefinitionEdge);
            }
            processDefinitionEdgeService.saveBatch(newEdgeList);
        }
        //
        List<ProcessFormTemplate> oldTemplateList = processFormTemplateService.list(new QueryWrapper<ProcessFormTemplate>().eq("process_definition_id", oldProcessDefinition.getId()));
        List<ProcessFormTemplate> newTemplateList = new ArrayList<>();
        for (ProcessFormTemplate oldProcessFormTemplate : oldTemplateList) {
            ProcessFormTemplate newProcessFormTemplate = new ProcessFormTemplate();
            BeanUtils.copyProperties(oldProcessFormTemplate, newProcessFormTemplate);
            newProcessFormTemplate.setProcessDefinitionId(newProcessDefinitionId);
            newTemplateList.add(newProcessFormTemplate);
        }
        processFormTemplateService.saveBatch(newTemplateList);

        return true;
    }
}

