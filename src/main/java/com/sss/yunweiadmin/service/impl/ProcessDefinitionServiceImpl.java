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

import java.util.ArrayList;
import java.util.List;

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

        List<ProcessFormTemplate> formTemplateList = processDefinitionVO.getFormTemplateList();
        List<ProcessDefinitionTask> taskList = processDefinitionVO.getTaskList();
        List<ProcessDefinitionEdge> edgeList = processDefinitionVO.getEdgeList();

        flag1 = this.save(processDefinition);

        formTemplateList.forEach(item -> {
            item.setProcessDefinitionId(processDefinition.getId());
            //value的中文逗号 转为 英文逗号
            if (!Strings.isNullOrEmpty(item.getValue())) {
                item.setValue(item.getValue().replaceAll("，", ","));
            }
        });
        flag2 = processFormTemplateService.saveBatch(formTemplateList);


        taskList.forEach(item -> {
            item.setProcessDefinitionId(processDefinition.getId());
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
    public boolean edit(ProcessDefinitionVO processDefinitionVO) {
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
            processDefinitionVO.getProcessDefinition().setDeployId(null);
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
        //是否发起了流程实例
        if (ObjectUtil.isNotEmpty(deployId)) {
            //已经发起了流程实例
            List<ProcessInstanceData> dataList = processInstanceDataService.list(new QueryWrapper<ProcessInstanceData>().ne("process_status", "完成").eq("process_definition_id", processDefinitionId));
            if (ObjectUtil.isNotEmpty(dataList)) {
                for (ProcessInstanceData processInstanceData : dataList) {
                    processInstanceDataService.delete(processInstanceData);
                }
            }
            workFlowBean.deleteDeploy(deployId);
        }
        //删除ProcessDefinition、ProcessDefinitionTask、ProcessDefinitionEdge、ProcessFormTemplate
        this.removeById(processDefinitionId);
        processDefinitionTaskService.remove(new QueryWrapper<ProcessDefinitionTask>().eq("process_definition_id", processDefinitionId));
        processDefinitionEdgeService.remove(new QueryWrapper<ProcessDefinitionEdge>().eq("process_definition_id", processDefinitionId));
        processFormTemplateService.remove(new QueryWrapper<ProcessFormTemplate>().eq("process_definition_id", processDefinitionId));
        //是否有流程定义的版本
        if (ObjectUtil.isNotEmpty(processDefinition.getBaseId())) {
            //回退到上一个版本
            Integer beforeId = processDefinition.getBeforeId();
            ProcessDefinition beforeProcessDefinition = this.getById(beforeId);
            beforeProcessDefinition.setHaveDisplay("是");
            this.updateById(beforeProcessDefinition);
        }

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

