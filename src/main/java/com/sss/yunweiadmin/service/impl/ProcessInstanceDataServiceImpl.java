package com.sss.yunweiadmin.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.common.collect.Lists;
import com.sss.yunweiadmin.bean.BpmnToActivitiBean;
import com.sss.yunweiadmin.bean.WorkFlowBean;
import com.sss.yunweiadmin.common.utils.SpringUtil;
import com.sss.yunweiadmin.mapper.ProcessInstanceDataMapper;
import com.sss.yunweiadmin.model.entity.*;
import com.sss.yunweiadmin.model.vo.CheckProcessVO;
import com.sss.yunweiadmin.model.vo.ModifyProcessFormVO;
import com.sss.yunweiadmin.model.vo.NextUserVO;
import com.sss.yunweiadmin.model.vo.StartProcessVO;
import com.sss.yunweiadmin.service.*;
import org.activiti.engine.history.HistoricTaskInstance;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * 流程实例数据 服务实现类
 * </p>
 *
 * @author 任勇林
 * @since 2021-04-17
 */
@Service
public class ProcessInstanceDataServiceImpl extends ServiceImpl<ProcessInstanceDataMapper, ProcessInstanceData> implements ProcessInstanceDataService {
    @Autowired
    ProcessFormValue1Service processFormValue1Service;
    @Autowired
    ProcessFormValue2Service processFormValue2Service;
    @Autowired
    ProcessDefinitionService processDefinitionService;
    @Autowired
    ProcessInstanceDataService processInstanceDataService;
    @Autowired
    ProcessInstanceNodeService processInstanceNodeService;
    @Autowired
    HttpSession httpSession;
    @Autowired
    SysDeptService sysDeptService;
    @Autowired
    ProcessFormTemplateService processFormTemplateService;
    @Autowired
    ProcessInstanceChangeService processInstanceChangeService;
    @Autowired
    BpmnToActivitiBean bpmnToActivitiBean;
    @Autowired
    AsDeviceCommonService asDeviceCommonService;
    @Autowired
    WorkFlowBean workFlowBean;
    @Autowired
    AsConfigService asConfigService;
    @Autowired
    ProcessFormCustomInstService processFormCustomInstService;

    private String getProcessName(StartProcessVO startProcessVO, ProcessDefinition processDefinition) {
        String name = null;
        String type = processDefinition.getProcessNameType();
        if (type.equals("流程定义名称")) {
            name = processDefinition.getProcessName();
        } else if (type.equals("用户名的流程定义名称")) {
            if (startProcessVO.getUserType().equals("给本人申请")) {//此时startProcessVO.getUserName()为空
                SysUser user = (SysUser) httpSession.getAttribute("user");
                name = user.getDisplayName() + "的" + processDefinition.getProcessName();
            } else {
                name = startProcessVO.getUserName() + "的" + processDefinition.getProcessName();
            }
        } else if (type.equals("资产名称的流程定义名称")) {
            //资产名称
            List<ProcessFormValue2> value2List = startProcessVO.getValue2List();
            if (ObjectUtil.isNotEmpty(value2List)) {
                List<AsDeviceCommon> list = asDeviceCommonService.listByIds(value2List.stream().map(ProcessFormValue2::getAsId).collect(Collectors.toList()));
                String assetName = list.stream().map(AsDeviceCommon::getName).collect(Collectors.joining("/"));
                name = assetName + "的" + processDefinition.getProcessName();
            } else {
                name = processDefinition.getProcessName();
            }
        }
        return name;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public synchronized boolean start(StartProcessVO startProcessVO) {
        //保存processFormValue1
        ProcessFormValue1 processFormValue1 = new ProcessFormValue1();
        BeanUtils.copyProperties(startProcessVO, processFormValue1);
        processFormValue1Service.save(processFormValue1);
        //
        startProcessVO.setId(processFormValue1.getId());
        //todo解析processFormValue1，并且插入customFormInst表; 下面格式的value1在遍历KEY时，可以通过长度来过滤非自定表字段
        //{"1550":"秘密","1551":"内网","1552":"未分配","asset":[{"customTableId":16,"asId":102002}],"16.计算机信息表.as_device_common.no.75":"J0601111","16.计算机信息表.as_device_common.name.76":"桌面计算机","16.计算机信息表.as_device_common.type_id.74":"联网终端","16.计算机信息表.as_device_common.model.94":"M4500T","16.计算机信息表.as_device_common.sn.95":"111111111","16.计算机信息表.as_device_common.buy_date.97":"2015-01-02","16.计算机信息表.as_device_common.use_date.98":"2015-01-01","16.计算机信息表.as_device_common.fund_src.78":"折旧资金","16.计算机信息表.as_device_common.net_type.79":"试验网","16.计算机信息表.as_device_common.state.85":"在用","16.计算机信息表.as_device_common.user_name.89":"任勇林","16.计算机信息表.as_device_common.user_dept.90":"信息化中心"}


        System.out.println(processFormValue1.getValue());


        ProcessDefinition processDefinition = processDefinitionService.getById(processFormValue1.getProcessDefinitionId());
        String actProcessName = processDefinition.getProcessName() + "_" + processDefinition.getId();
        if (ObjectUtil.isEmpty(processDefinition.getDeployId())) {
            //activiti没有部署过
            String activitiXml = bpmnToActivitiBean.convert(processDefinition);
            Deployment deployment = workFlowBean.deploy(actProcessName, activitiXml);
            //更新deploy_id
            processDefinition.setDeployId(deployment.getId());
            processDefinitionService.updateById(processDefinition);
        }
        //启动流程；引发第一个ActTask创建
        ProcessInstance processInstance = workFlowBean.startProcessInstance(actProcessName, processFormValue1.getId());
        //更新processFormValue1
        processFormValue1.setActProcessInstanceId(processInstance.getId());
        processFormValue1Service.updateById(processFormValue1);
        //保存processFormValue2
        List<ProcessFormValue2> processFormValue2List = startProcessVO.getValue2List();
        processFormValue2List.forEach(item -> {
            item.setProcessDefinitionId(startProcessVO.getProcessDefinitionId());
            item.setActProcessInstanceId(processInstance.getId());
            item.setFormValue1Id(startProcessVO.getId());
        });
        if (ObjectUtil.isNotEmpty(processFormValue2List)) {
            processFormValue2Service.saveBatch(processFormValue2List);
        }
        //nextUserVO放入session
        if (startProcessVO.getHaveNextUser().equals("是")) {
            NextUserVO nextUserVO = new NextUserVO(startProcessVO.getType(), startProcessVO.getTypeValue(), startProcessVO.getHaveNextUser());
            httpSession.setAttribute("nextUserVO", nextUserVO);
        }
        //提交者完成任务；引发第二个ActTask创建
        List<Task> myTaskList = workFlowBean.getMyTask(processInstance.getId());
        Task myTask = myTaskList.get(0);
        if (ObjectUtil.isNotEmpty(startProcessVO.getButtonName())) {
            workFlowBean.completeTaskByButtonName(processDefinition.getId(), myTask, startProcessVO.getButtonName());
        } else {
            workFlowBean.completeTask(processDefinition.getId(), myTask);
        }
        //跳转到ActEventListener,设置下一个节点的处理人（如果下一个节点是当前节点<发起人>指定的，那会用到session/"nextUserVO"）
        //20211216注意以上代码总共会引发创建两个activiTask:发起人task及下一步的task:两次进入监听器
        //插入流程实例数据
        ProcessInstanceData processInstanceData = new ProcessInstanceData();
        processInstanceData.setProcessDefinitionId(processDefinition.getId());
        //20211203根据流程实例命名规则及表单提交者确定名称
        processInstanceData.setProcessName(getProcessName(startProcessVO, processDefinition));
        processInstanceData.setBusinessId(processFormValue1.getId());
        processInstanceData.setActProcessInstanceId(processInstance.getId());
        processInstanceData.setProcessStatus("审批中");
        //断点20211211 instanceDataid为0 到底有何意义，为何现在单单在我的用户变更流程（最后一个/第二节点是发起人）提交时会报锳！！？？？
        Map<String, String> stepMap = workFlowBean.getCurrentStep(processDefinition.getId(), 0, processInstance.getId(), myTask.getTaskDefinitionKey());
        processInstanceData.setDisplayCurrentStep(stepMap.get("displayName"));
        processInstanceData.setLoginCurrentStep(stepMap.get("loginName"));
        SysUser user = (SysUser) httpSession.getAttribute("user");
        SysDept dept = sysDeptService.getById(user.getDeptId());
        processInstanceData.setDisplayName(user.getDisplayName());
        processInstanceData.setLoginName(user.getLoginName());
        processInstanceData.setDeptName(dept.getName());
        processInstanceData.setStartDatetime(LocalDateTime.now());
        processInstanceDataService.save(processInstanceData);
        //插入流程节点数据
        ProcessInstanceNode processInstanceNode = new ProcessInstanceNode();
        processInstanceNode.setProcessInstanceDataId(processInstanceData.getId());
        processInstanceNode.setTaskId(myTask.getTaskDefinitionKey());
        processInstanceNode.setTaskName(myTask.getName());
        processInstanceNode.setDisplayName(user.getDisplayName());
        processInstanceNode.setLoginName(user.getLoginName());
        processInstanceNode.setDeptName(dept.getName());
        //
        LocalDateTime localDateTime = LocalDateTime.now();
        processInstanceNode.setStartDatetime(localDateTime);
        processInstanceNode.setEndDatetime(localDateTime);
        if (ObjectUtil.isNotEmpty(startProcessVO.getButtonName())) {
            processInstanceNode.setButtonName(startProcessVO.getButtonName());
        } else {
            processInstanceNode.setButtonName("提交");
        }
        if (ObjectUtil.isNotEmpty(startProcessVO.getHaveNextUser()) && startProcessVO.getHaveNextUser().equals("是")) {
            processInstanceNode.setType(startProcessVO.getType());
            processInstanceNode.setTypeValue(startProcessVO.getTypeValue());
            processInstanceNode.setTypeLabel(startProcessVO.getTypeLabel());
        }
        processInstanceNodeService.save(processInstanceNode);

        //变更字段 todo看发起流程时的属性变更的逻辑
        changeColumnForStart(processInstanceData, processFormValue1, processFormValue2List);
        //
        httpSession.removeAttribute("nextUserVO");

        return true;
    }

    @Override
    @Transactional
    public synchronized boolean handle(CheckProcessVO checkProcessVO) {
        ProcessInstanceData processInstanceData = processInstanceDataService.getById(checkProcessVO.getProcessInstanceDataId());
        String actProcessInstanceId = processInstanceData.getActProcessInstanceId();
        String processStatus = processInstanceData.getProcessStatus();
        //checkProcessVO放入session
        if (checkProcessVO.getHaveNextUser().equals("是")) {
            NextUserVO nextUserVO = new NextUserVO(checkProcessVO.getType(), checkProcessVO.getTypeValue(), checkProcessVO.getHaveNextUser());
            httpSession.setAttribute("nextUserVO", nextUserVO);
        }
        //取出我的一个任务
        List<Task> myTaskList = workFlowBean.getMyTask(actProcessInstanceId);
        Task myTask = myTaskList.get(0);
        //完成任务
        if (ObjectUtil.isNotEmpty(checkProcessVO.getButtonName())) {
            workFlowBean.completeTaskByButtonName(processInstanceData.getProcessDefinitionId(), myTask, checkProcessVO.getButtonName());
        } else {
            workFlowBean.completeTask(processInstanceData.getProcessDefinitionId(), myTask);
        }
        //跳转到ActEventListener,设置下一个节点的处理人
        //更新流程实例和处理变更字段
        if (workFlowBean.finish(processInstanceData.getActProcessInstanceId())) {
            processInstanceData.setEndDatetime(LocalDateTime.now());
            processInstanceData.setProcessStatus("完成");
            processInstanceData.setDisplayCurrentStep("");
            processInstanceData.setLoginCurrentStep("");
            //变更字段
            changeColumnForHandle(processInstanceData, "是");
        } else {
            Map<String, String> stepMap = workFlowBean.getCurrentStep(processInstanceData.getProcessDefinitionId(), processInstanceData.getId(), processInstanceData.getActProcessInstanceId(), myTask.getTaskDefinitionKey());
            processInstanceData.setDisplayCurrentStep(stepMap.get("displayName"));
            processInstanceData.setLoginCurrentStep(stepMap.get("loginName"));
            //流程状态
            String currentTaskId = workFlowBean.getActiveTask(processInstanceData.getActProcessInstanceId()).get(0).getTaskDefinitionKey();
            if (workFlowBean.getPreCurrentTaskEdge(processInstanceData.getProcessDefinitionId(), myTask.getTaskDefinitionKey(), currentTaskId) != null) {
                processInstanceData.setProcessStatus("退回");
            } else {
                //2021117对于处理节点，要改成“处理中”，这个值有利于前端动态渲染“审批/处理按钮”名字时判断
                if(stepMap.get("displayName").contains("处理")){
                    processInstanceData.setProcessStatus("处理中");
                } else processInstanceData.setProcessStatus("审批中");
            }
            //变更字段
            changeColumnForHandle(processInstanceData, "否");
        }
        processInstanceDataService.updateById(processInstanceData);
        //插入流程节点数据
        SysUser user = (SysUser) httpSession.getAttribute("user");
        SysDept dept = sysDeptService.getById(user.getDeptId());
        ProcessInstanceNode processInstanceNode = new ProcessInstanceNode();
        processInstanceNode.setProcessInstanceDataId(processInstanceData.getId());
        processInstanceNode.setTaskId(myTask.getTaskDefinitionKey());
        processInstanceNode.setTaskName(myTask.getName());
        processInstanceNode.setDisplayName(user.getDisplayName());
        processInstanceNode.setLoginName(user.getLoginName());
        processInstanceNode.setDeptName(dept.getName());
        //
        HistoricTaskInstance historicTaskInstance = workFlowBean.getHistoricTaskInstance(processInstanceData.getActProcessInstanceId(), myTask.getTaskDefinitionKey());
        Date startDateTime = historicTaskInstance.getStartTime();
        Date endDateTime = historicTaskInstance.getEndTime();
        //
        processInstanceNode.setStartDatetime(LocalDateTime.ofInstant(startDateTime.toInstant(), ZoneId.systemDefault()));
        processInstanceNode.setEndDatetime(LocalDateTime.ofInstant(endDateTime.toInstant(), ZoneId.systemDefault()));
        if (ObjectUtil.isNotEmpty(checkProcessVO.getButtonName())) {
            processInstanceNode.setButtonName(checkProcessVO.getButtonName());
        } else {
            processInstanceNode.setButtonName("提交");
        }
        if (ObjectUtil.isNotEmpty(checkProcessVO.getHaveNextUser()) && checkProcessVO.getHaveNextUser().equals("是")) {
            processInstanceNode.setType(checkProcessVO.getType());
            processInstanceNode.setTypeValue(checkProcessVO.getTypeValue());
            processInstanceNode.setTypeLabel(checkProcessVO.getTypeLabel());
        }
        if (ObjectUtil.isNotEmpty(checkProcessVO.getComment()) && checkProcessVO.getHaveComment().equals("是")) {
            processInstanceNode.setComment(checkProcessVO.getComment());
        } else {
            if (processInstanceNode.getButtonName().contains("同意")) {//20211117修改退回时，意见也是同意的问题
                processInstanceNode.setComment("同意");
            }
        }
        if (ObjectUtil.isNotEmpty(checkProcessVO.getHaveOperate()) && checkProcessVO.getHaveOperate().equals("是")) {
            processInstanceNode.setOperate(checkProcessVO.getOperate());
        }
        processInstanceNodeService.save(processInstanceNode);

        //processFormValue1.value
        if (checkProcessVO.getHaveEditForm().equals("是")) {
            ProcessFormValue1 processFormValue1 = processFormValue1Service.getOne(new QueryWrapper<ProcessFormValue1>().eq("act_process_instance_id", actProcessInstanceId).eq("process_definition_id", processInstanceData.getProcessDefinitionId()));
            processFormValue1.setValue(checkProcessVO.getValue());
            processFormValue1Service.updateById(processFormValue1);
        }

        //
        httpSession.removeAttribute("nextUserVO");

        return true;
    }

    @Override
    public boolean modifyProcessForm(ModifyProcessFormVO modifyProcessFormVO) {
        Integer processFormValue1Id = modifyProcessFormVO.getProcessFormValue1Id();
        //
        System.out.println( modifyProcessFormVO);
        ProcessFormValue1 processFormValue1 = processFormValue1Service.getById(processFormValue1Id);
        processFormValue1.setValue(modifyProcessFormVO.getValue());
        return processFormValue1Service.updateById(processFormValue1);
    }

    @Override
    public boolean delete(ProcessInstanceData processInstanceData) {
        //删除processInstanceData
        this.removeById(processInstanceData.getId());
        //删除processInstanceNode
        processInstanceNodeService.remove(new QueryWrapper<ProcessInstanceNode>().eq("process_instance_data_id", processInstanceData.getId()));
        //删除processInstanceChange
        processInstanceChangeService.remove(new QueryWrapper<ProcessInstanceChange>().eq("process_instance_data_id", processInstanceData.getId()));
        //删除processFormValue1
        processFormValue1Service.remove(new QueryWrapper<ProcessFormValue1>().eq("act_process_instance_id", processInstanceData.getActProcessInstanceId()).eq("process_definition_id", processInstanceData.getProcessDefinitionId()));
        //删除processFormValue2
        processFormValue2Service.remove(new QueryWrapper<ProcessFormValue2>().eq("act_process_instance_id", processInstanceData.getActProcessInstanceId()).eq("process_definition_id", processInstanceData.getProcessDefinitionId()));
        //删除流程实例
        workFlowBean.deleteProcessInstance(processInstanceData.getActProcessInstanceId());

        return true;
    }

    private void changeColumnForStart(ProcessInstanceData processInstanceData, ProcessFormValue1 processFormValue1, List<ProcessFormValue2> formValue2List) {
        if (CollUtil.isEmpty(formValue2List)) return;
        //
        List<AsConfig> asConfigList = asConfigService.list(new QueryWrapper<AsConfig>().select("distinct en_table_name,zh_table_name"));
        Map<String, String> asConfigMap = asConfigList.stream().collect(Collectors.toMap(AsConfig::getEnTableName, AsConfig::getZhTableName));
        //
        JSONObject jsonObject = JSONObject.parseObject(processFormValue1.getValue());
        Map<Integer, Integer> asIdMap = formValue2List.stream().collect(Collectors.toMap(ProcessFormValue2::getCustomTableId, ProcessFormValue2::getAsId));
        //取出所有的变更字段
        List<ProcessFormTemplate> formTemplateList = processFormTemplateService.list(new QueryWrapper<ProcessFormTemplate>().eq("process_definition_id", processInstanceData.getProcessDefinitionId()).eq("flag", "字段变更类型").orderByAsc("name"));
        //组装map
        Map<Integer, List<ProcessFormTemplate>> map = new HashMap<>();
        for (ProcessFormValue2 processFormValue2 : formValue2List) {
            List<ProcessFormTemplate> list = formTemplateList.stream().filter(item -> (item.getName().split("\\.")[0]).equals(processFormValue2.getCustomTableId() + "")).collect(Collectors.toList());
            map.put(processFormValue2.getCustomTableId(), list);
        }
        //遍历map
        for (Map.Entry<Integer, List<ProcessFormTemplate>> entry : map.entrySet()) {
            Integer asId = asIdMap.get(entry.getKey());
            //组装map2
            Map<String, List<ProcessFormTemplate>> map2 = new HashMap<>();
            for (ProcessFormTemplate processFormTemplate : entry.getValue()) {
                String tableName = processFormTemplate.getName().split("\\.")[2];
                if (map2.get(tableName) != null) {
                    map2.get(tableName).add(processFormTemplate);
                } else {
                    map2.put(tableName, Lists.newArrayList(processFormTemplate));
                }
            }
            //遍历map2
            List<ProcessInstanceChange> changeList = Lists.newArrayList();
            for (Map.Entry<String, List<ProcessFormTemplate>> entry2 : map2.entrySet()) {
                //as_device_common
                String tableNameTmp = entry2.getKey();
                String tableName = StrUtil.toCamelCase(tableNameTmp);
                //取出数据对象
                //20220414 todo再这里加入“流程实例表单快照表”
//                ProcessFormCustomInst processFormCustomInst = new ProcessFormCustomInst();
//                processFormCustomInst.setTableName(tableName);
//                processFormCustomInstService.save(processFormCustomInst);


                IService service = (IService) SpringUtil.getBean(tableName + "ServiceImpl");
                Object dbObject = null;
                if (tableNameTmp.equals("as_device_common")) {
                    dbObject = service.getById(asId);
                } else {
                    dbObject = service.getOne(new QueryWrapper<Object>().eq("as_id", asId));
                }
                //
                for (ProcessFormTemplate processFormTemplate : entry2.getValue()) {
                    //name,baomi_no
                    String columnNameTmp = (processFormTemplate.getName().split(",")[0]).split("\\.")[3];
                    String columnName = StrUtil.toCamelCase(columnNameTmp);
                    //取出processFormValue1中id对应的值
                    Integer id = processFormTemplate.getId();
                    String pageValue;
                    if (processFormTemplate.getType().equals("日期")) {//20220412之前版本里没对日期单独处理，todo问张强
                        pageValue = jsonObject.getString(id + "Date");
                    } else if (processFormTemplate.getType().equals("日期时间")) {
                        pageValue = jsonObject.getString(id + "Datetime");
                    } else {
                        pageValue = jsonObject.getString(id + "");
                    }
                    if (ObjectUtil.isNotEmpty(pageValue)) {
                        Object dbValueObj = ReflectUtil.getFieldValue(dbObject, columnName);
                        String dbValue = null;
                        if (ObjectUtil.isEmpty(dbValueObj)) {
                            dbValue = "";
                        } else {
                            dbValue = dbValueObj.toString();
                        }
                        if (!pageValue.equals(dbValue)) {
                            //变更字段
                            ProcessInstanceChange change = new ProcessInstanceChange();
                            change.setAsId(asId);
                            change.setProcessInstanceDataId(processInstanceData.getId());
                            change.setActProcessInstanceId(processInstanceData.getActProcessInstanceId());
                            change.setName(processFormTemplate.getLabel().split("\\.")[1]);
                            change.setOldValue(dbValue);
                            change.setNewValue(pageValue);
                            //
                            SysUser user = (SysUser) httpSession.getAttribute("user");
                            SysDept dept = sysDeptService.getById(user.getDeptId());
                            change.setDeptName(dept.getName());
                            change.setDisplayName(user.getDisplayName());
                            change.setLoginName(user.getLoginName());
                            change.setModifyDatetime(LocalDateTime.now());
                            change.setFlag("否");
                            change.setZhTableName(asConfigMap.get(tableNameTmp));

                            changeList.add(change);
                        }
                    }
                }
            }
            //
            if (ObjectUtil.isNotEmpty(changeList)) {
                processInstanceChangeService.saveBatch(changeList);
            }
        }
    }

    private void changeColumnForHandle(ProcessInstanceData processInstanceData, String flag) {
        //先删除
        processInstanceChangeService.remove(new QueryWrapper<ProcessInstanceChange>().eq("process_instance_data_id", processInstanceData.getId()));

        ProcessFormValue1 processFormValue1 = processFormValue1Service.getOne(new QueryWrapper<ProcessFormValue1>().eq("process_definition_id", processInstanceData.getProcessDefinitionId()).eq("act_process_instance_id", processInstanceData.getActProcessInstanceId()));
        List<ProcessFormValue2> formValue2List = processFormValue2Service.list(new QueryWrapper<ProcessFormValue2>().eq("form_value1_id", processFormValue1.getId()));
        if (CollUtil.isEmpty(formValue2List)) return;
        //
        List<AsConfig> asConfigList = asConfigService.list(new QueryWrapper<AsConfig>().select("distinct en_table_name,zh_table_name"));
        Map<String, String> asConfigMap = asConfigList.stream().collect(Collectors.toMap(AsConfig::getEnTableName, AsConfig::getZhTableName));
        //
        JSONObject jsonObject = JSONObject.parseObject(processFormValue1.getValue());
        Map<Integer, Integer> asIdMap = formValue2List.stream().collect(Collectors.toMap(ProcessFormValue2::getCustomTableId, ProcessFormValue2::getAsId));
        //取出所有的变更字段
        List<ProcessFormTemplate> formTemplateList = processFormTemplateService.list(new QueryWrapper<ProcessFormTemplate>().eq("process_definition_id", processInstanceData.getProcessDefinitionId()).eq("flag", "字段变更类型").orderByAsc("name"));
        //组装map
        Map<Integer, List<ProcessFormTemplate>> map = new HashMap<>();
        for (ProcessFormValue2 processFormValue2 : formValue2List) {
            List<ProcessFormTemplate> list = formTemplateList.stream().filter(item -> (item.getName().split("\\.")[0]).equals(processFormValue2.getCustomTableId() + "")).collect(Collectors.toList());
            map.put(processFormValue2.getCustomTableId(), list);
        }
        //遍历map
        for (Map.Entry<Integer, List<ProcessFormTemplate>> entry : map.entrySet()) {
            Integer asId = asIdMap.get(entry.getKey());
            //组装map2
            Map<String, List<ProcessFormTemplate>> map2 = new HashMap<>();
            for (ProcessFormTemplate processFormTemplate : entry.getValue()) {
                String tableName = processFormTemplate.getName().split("\\.")[2];
                if (map2.get(tableName) != null) {
                    map2.get(tableName).add(processFormTemplate);
                } else {
                    map2.put(tableName, Lists.newArrayList(processFormTemplate));
                }
            }
            //遍历map2
            List<ProcessInstanceChange> changeList = Lists.newArrayList();
            for (Map.Entry<String, List<ProcessFormTemplate>> entry2 : map2.entrySet()) {
                //as_device_common
                String tableNameTmp = entry2.getKey();
                String tableName = StrUtil.toCamelCase(tableNameTmp);
                //取出数据对象
                IService service = (IService) SpringUtil.getBean(tableName + "ServiceImpl");
                Object dbObject = null;
                if (tableNameTmp.equals("as_device_common")) {
                    dbObject = service.getById(asId);
                } else {
                    dbObject = service.getOne(new QueryWrapper<Object>().eq("as_id", asId));
                }
                //
                for (ProcessFormTemplate processFormTemplate : entry2.getValue()) {
                    //name,baomi_no
                    String columnNameTmp = (processFormTemplate.getName().split(",")[0]).split("\\.")[3];
                    String columnName = StrUtil.toCamelCase(columnNameTmp);
                    //取出processFormValue1中id对应的值
                    Integer id = processFormTemplate.getId();
                    String pageValue;
                    if (processFormTemplate.getType().equals("日期")) {
                        pageValue = jsonObject.getString(id + "Date");
                    } else if (processFormTemplate.getType().equals("日期时间")) {
                        pageValue = jsonObject.getString(id + "Datetime");
                    } else {
                        pageValue = jsonObject.getString(id + "");
                    }
                    if (ObjectUtil.isNotEmpty(pageValue)) {
                        Object dbValueObj = ReflectUtil.getFieldValue(dbObject, columnName);
                        String dbValue = null;
                        if (ObjectUtil.isEmpty(dbValueObj)) {
                            dbValue = "";
                        } else {
                            dbValue = dbValueObj.toString();
                        }
                        if (!pageValue.equals(dbValue)) {
                            //变更字段
                            ProcessInstanceChange change = new ProcessInstanceChange();
                            change.setAsId(asId);
                            change.setProcessInstanceDataId(processInstanceData.getId());
                            change.setActProcessInstanceId(processInstanceData.getActProcessInstanceId());
                            change.setName(processFormTemplate.getLabel().split("\\.")[1]);
                            change.setOldValue(dbValue);
                            change.setNewValue(pageValue);
                            //
                            SysUser user = (SysUser) httpSession.getAttribute("user");
                            SysDept dept = sysDeptService.getById(user.getDeptId());
                            change.setDeptName(dept.getName());
                            change.setDisplayName(user.getDisplayName());
                            change.setLoginName(user.getLoginName());
                            change.setModifyDatetime(LocalDateTime.now());
                            change.setFlag(flag);
                            change.setZhTableName(asConfigMap.get(tableNameTmp));

                            changeList.add(change);
                            //数据对象
                            if (flag.equals("是")) {
                                ReflectUtil.setFieldValue(dbObject, columnName, pageValue);
                            }
                        }
                    }
                }
                //数据对象
                if (flag.equals("是")) {
                    service.updateById(dbObject);
                }
            }
            //
            if (ObjectUtil.isNotEmpty(changeList)) {
                processInstanceChangeService.saveBatch(changeList);
            }
        }
    }
}
