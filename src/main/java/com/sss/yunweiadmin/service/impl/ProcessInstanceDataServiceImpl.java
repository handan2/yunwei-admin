package com.sss.yunweiadmin.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONArray;
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
import org.apache.xmlbeans.impl.schema.XQuerySchemaTypeSystem;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
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
    @Autowired
    AsTypeServiceImpl asTypeService;
    @Autowired
    DiskForHisForProcessService diskForHisForProcessService;


    private void saveProcessFormCustomInst(String jsonStr, Integer processInstanceId, List<ProcessFormValue2> processFormValue2List) {
        JSONObject jsonObject = JSONObject.parseObject(jsonStr);

        //20220601改成直接读value2List成生map的方法：  map格式：{16=102002, 17=102004}:用于遍历每个自定义表字段时查找对象资产id(来进一步查询资产类型)
        if (CollUtil.isEmpty(processFormValue2List))
            return;
        //20220601vaule1/value2表中的actProcessInstanceId字段为啥设置成vachar暂不研
        Map<String, String> map = processFormValue2List.stream().collect(Collectors.toMap(t -> String.valueOf(t.getCustomTableId()), t -> String.valueOf(t.getAsId())));

        //原先map的获取方法
//        Map<String, String> map = new HashMap<>();
//        JSONArray jsonAssetArray = jsonObject.getJSONArray("asset"); //20220506json数组有一个专有类型 JSONArray,对应的(在jsonObject里)取值也是一个专用函数
//        //20220528加判断空
//        if (CollUtil.isEmpty(jsonAssetArray))
//            return;
//        jsonAssetArray.stream().forEach(item -> {
//            JSONObject itemJson = (JSONObject) item;//注意：关于强转“直接用后半句(JSONObject)item.getString”是不行的，可能因为那个括号最后执行吧，也不是：todo记录
//            System.out.println(item);
//            map.put(itemJson.getString("customTableId"), itemJson.getString("asId"));
//        });


        List<ProcessFormCustomInst> processFormCustomInstList = new ArrayList<>();
        //List<ProcessFormCustomInst> processFormCustomInstList_old = processFormCustomInstService.list(new QueryWrapper<ProcessFormCustomInst>().eq("process_instance_data_id",checkProcessVO.getProcessInstanceDataId()));
        for (Map.Entry entry : jsonObject.entrySet()) {
            if (entry.getKey().toString().contains(".")) {//通过"."识别自定表字段
                String[] keyArray = entry.getKey().toString().split("\\.");
                ProcessFormCustomInst processFormCustomInst = new ProcessFormCustomInst();
                processFormCustomInst.setTableName(keyArray[2]);
                processFormCustomInst.setColumnType("自定义表字段");
                processFormCustomInst.setColumnName(keyArray[3]);
                processFormCustomInst.setColumnValue(ObjectUtil.isNotEmpty(entry.getValue()) ? entry.getValue().toString() : "");
                processFormCustomInst.setProcessInstanceDataId(processInstanceId);
                String assetId = map.get(keyArray[0]);
                processFormCustomInst.setAsId(Integer.parseInt(assetId));
                Integer type_id = asDeviceCommonService.getById(assetId).getTypeId();
                Integer Level2AsTypeId = asTypeService.getLevel2AsTypeById(type_id).getId();//20220510换成asid todo断点
                processFormCustomInst.setAssetTypeId(Level2AsTypeId);
                processFormCustomInstList.add(processFormCustomInst);
            }
        }
        processFormCustomInstService.remove(new QueryWrapper<ProcessFormCustomInst>().eq("process_instance_data_id", processInstanceId));
        processFormCustomInstService.saveBatch(processFormCustomInstList);
    }

    private String getProcessName(StartProcessVO startProcessVO, ProcessDefinition processDefinition) {
        String name = null;
        String type = processDefinition.getProcessNameType();
        if (type.equals("流程定义名称")) {
            name = processDefinition.getProcessName();
        } else if (type.equals("用户名的流程定义名称")) {
            if (startProcessVO.getValue1().getCommitterType().equals("给本人申请")) {//此时startProcessVO.getCommitterName()为空
                SysUser user = (SysUser) httpSession.getAttribute("user");
                name = user.getDisplayName() + "的" + processDefinition.getProcessName();
            } else {
                name = startProcessVO.getValue1().getCommitterName() + "的" + processDefinition.getProcessName();
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
        BeanUtils.copyProperties(startProcessVO.getValue1(), processFormValue1);
        processFormValue1Service.save(processFormValue1);
        startProcessVO.getValue1().setId(processFormValue1.getId());


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
        ProcessInstance actProcessInstance = workFlowBean.startProcessInstance(actProcessName, processFormValue1.getId());
        //更新processFormValue1
        processFormValue1.setActProcessInstanceId(actProcessInstance.getId());
        processFormValue1Service.updateById(processFormValue1);
        //保存processFormValue2
        List<ProcessFormValue2> processFormValue2List = startProcessVO.getValue2List();
        //20220528加判空

        if (CollUtil.isNotEmpty(processFormValue2List)) {
            processFormValue2List.forEach(item -> {
                item.setProcessDefinitionId(startProcessVO.getValue1().getProcessDefinitionId());
                item.setActProcessInstanceId(actProcessInstance.getId());
                item.setFormValue1Id(startProcessVO.getValue1().getId());
            });
            processFormValue2Service.saveBatch(processFormValue2List);
        }
        //nextUserVO放入session
        if (startProcessVO.getHaveNextUser().equals("是")) {
            NextUserVO nextUserVO = new NextUserVO(startProcessVO.getOperatorType(), startProcessVO.getOperatorTypeValue(), startProcessVO.getHaveNextUser());
            httpSession.setAttribute("nextUserVO", nextUserVO);
        }
        //第二个ActTask创建；20220608 startEvent执行完后activeTask，一般只会有一activeTask(即“发起者”那个节点)&&流程的发起者一般也同样是“发起者”那个处理候选人：直接查activeTask即可：不过暂不改
        List<Task> myTaskList = workFlowBean.getMyTask(actProcessInstance.getId());
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
        processInstanceData.setActProcessInstanceId(actProcessInstance.getId());
        processInstanceData.setProcessStatus("审批中");
        //断点20211211 instanceDataid为0 到底有何意义，为何现在单单在我的用户变更流程（最后一个/第二节点是发起人）提交时会报锳！！？？？
        Map<String, String> stepMap = workFlowBean.getCurrentStep(processDefinition.getId(), 0, actProcessInstance.getId(), myTask.getTaskDefinitionKey());
        processInstanceData.setDisplayCurrentStep(stepMap.get("displayName"));
        processInstanceData.setLoginCurrentStep(stepMap.get("loginName"));
        SysUser user = (SysUser) httpSession.getAttribute("user");
        SysDept dept = sysDeptService.getById(user.getDeptId());
        processInstanceData.setDisplayName(user.getDisplayName());
        processInstanceData.setLoginName(user.getLoginName());
        processInstanceData.setDeptName(dept.getName());
        processInstanceData.setStartDatetime(LocalDateTime.now());
        processInstanceDataService.save(processInstanceData);//保存成功后mybatis会把主键/ID传回参数processInstanceData中

        //20220510
        this.saveProcessFormCustomInst(startProcessVO.getValue1().getValue(), processInstanceData.getId(), processFormValue2List);

        //插入流程节点数据
        ProcessInstanceNode processInstanceNode = new ProcessInstanceNode();
        processInstanceNode.setProcessInstanceDataId(processInstanceData.getId());
        processInstanceNode.setTaskDefKey(myTask.getTaskDefinitionKey());
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
            processInstanceNode.setOperatorType(startProcessVO.getOperatorType());
            processInstanceNode.setOperatorTypeValue(startProcessVO.getOperatorTypeValue());
            processInstanceNode.setOperatorTypeLabel(startProcessVO.getOperatorTypeLabel());
        }
        processInstanceNodeService.save(processInstanceNode);

        //20220620 保存硬盘信息
        List<DiskForHisForProcess> diskListForHisForProcess = startProcessVO.getDiskListForHisForProcess();
        //目前没有“删除”,第一次提交只有（对DiskForHisForProcess表来说）新增，对AsDeviceCommon表来说有“新增/编辑”
        //  List<DiskForHisForProcess> diskListForHisForProcessForDel = diskListForHisForProcess.stream().filter(item -> item.getTemp().equals("删除")).collect(Collectors.toList());
        //  List<DiskForHisForProcess> diskListForHisForProcessForSave = diskListForHisForProcess.stream().filter(item -> !(item.getTemp().equals("删除"))).collect(Collectors.toList());
        if (diskListForHisForProcess != null) {
//            if (diskListForHisForProcessForDel != null)
//                diskForHisForProcessService.removeByIds(diskListForHisForProcessForDel.stream().map(AsDeviceCommon::getId).collect(Collectors.toList()));
            DiskForHisForProcess disk = diskListForHisForProcess.get(0);
            //String hostAsNo = asDeviceCommonService.getById(disk.getHostAsId()).getNo();
            if (diskListForHisForProcess != null) {
                diskListForHisForProcess.forEach(item -> {
                    item.setProcessInstanceDataId(processInstanceData.getId());
                });
                diskForHisForProcessService.saveBatch(diskListForHisForProcess);//20220614这个flag设置有点小问题：暂不改
            }
        }


        //变更字段
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
            NextUserVO nextUserVO = new NextUserVO(checkProcessVO.getOperatorType(), checkProcessVO.getOperatorTypeValue(), checkProcessVO.getHaveNextUser());
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
        if (workFlowBean.isFinish(processInstanceData.getActProcessInstanceId())) {//2020621finish应改成isFinish
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
            String currentTaskDefKey = workFlowBean.getActiveTask(processInstanceData.getActProcessInstanceId()).get(0).getTaskDefinitionKey();
            if (workFlowBean.getPreCurrentTaskEdge(processInstanceData.getProcessDefinitionId(), myTask.getTaskDefinitionKey(), currentTaskDefKey) != null) {
                processInstanceData.setProcessStatus("退回");
            } else {
                //2021117对于处理节点，要改成“处理中”，这个值有利于前端动态渲染“审批/处理按钮”名字时判断
                if (stepMap.get("displayName").contains("处理")) {
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
        processInstanceNode.setTaskDefKey(myTask.getTaskDefinitionKey());
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
            processInstanceNode.setOperatorType(checkProcessVO.getOperatorType());
            processInstanceNode.setOperatorTypeValue(checkProcessVO.getOperatorTypeValue());
            processInstanceNode.setOperatorTypeLabel(checkProcessVO.getOperatorTypeLabel());
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
//20220531 todo加判断，读节点信息，节点属性里是设置有权更改资产号的也要保存value1:貌似更改节点的逻辑还没记录在node表中/界面配置里也没加呢
        //processFormValue1.value
        ProcessFormValue1 processFormValue1 = processFormValue1Service.getOne(new QueryWrapper<ProcessFormValue1>().eq("act_process_instance_id", actProcessInstanceId));
        // if (checkProcessVO.getHaveEditForm().equals("是")) {
        processFormValue1.setValue(checkProcessVO.getValue1().getValue());//20220531todo在checkVo增加vaule2List(同stratVo那种)，
        processFormValue1Service.updateById(processFormValue1);
        //}

        //20220531加
        //保存processFormValue2
        List<ProcessFormValue2> processFormValue2List = checkProcessVO.getValue2List();
        //20220528加判空

        if (CollUtil.isNotEmpty(processFormValue2List)) {

            processFormValue2Service.remove(new QueryWrapper<ProcessFormValue2>().eq("act_process_instance_id", actProcessInstanceId));
            processFormValue2List.forEach(item -> {
                item.setProcessDefinitionId(processFormValue1.getProcessDefinitionId());
                item.setActProcessInstanceId(actProcessInstanceId);
                item.setFormValue1Id(processFormValue1.getId());
            });
            processFormValue2Service.saveBatch(processFormValue2List);
        }


        //20220621 保存硬盘信息  todo断点:先把DiskForHisForProcess表的记录更改了：  asDeviceCommons 要等到流程结束时更改
        //先删除DiskForHisForProcess表原记录
        diskForHisForProcessService.remove(new QueryWrapper<DiskForHisForProcess>().eq("process_instance_data_id", processInstanceData.getId()));


        List<DiskForHisForProcess> diskListForHis = checkProcessVO.getDiskListForHisForProcess();
        // List<AsDeviceCommon> diskListForHisForDel = diskListForHis.stream().filter(item -> item.getTemp().equals("删除")).collect(Collectors.toList());
//        List<DiskForHisForProcess> diskListForHisForSave = diskListForHis.stream().filter(item -> (item.getFlag().equals("新增"))).collect(Collectors.toList());
//        List<DiskForHisForProcess> diskListForHisForUpdate = diskListForHis.stream().filter(item -> (item.getFlag().equals("修改"))).collect(Collectors.toList());
//        List<DiskForHisForProcess> diskListForHisNew = new ArrayList<>();
        List<AsDeviceCommon> asDeviceCommonList = null;
        if (diskListForHis != null) {

            AsDeviceCommon asDeviceCommon = asDeviceCommonService.getById(diskListForHis.get(0).getHostAsId());
            diskListForHis.stream().forEach(item -> {

                // DiskForHisForProcess itemNew = new DiskForHisForProcess();
             //因为
               // item.setProcessInstanceDataId(processInstanceData.getId());
                if (workFlowBean.isFinish(processInstanceData.getActProcessInstanceId())) {
                    AsDeviceCommon aaa = new AsDeviceCommon();
                    BeanUtils.copyProperties(item, aaa);
                    // BeanUtils.copyProperties(item, itemNew);
                    if (item.getFlag().equals("新增")) {
                        aaa.setUserName(asDeviceCommon.getUserName());
                        aaa.setUserDept(asDeviceCommon.getUserDept());
                        aaa.setUserMiji(asDeviceCommon.getUserMiji());
                        aaa.setNetType(asDeviceCommon.getNetType());
                        aaa.setName("硬盘");
                        aaa.setTypeId(25);
                        asDeviceCommonService.save(aaa);
                        item.setAsId(aaa.getId());
                    } else if (item.getFlag().equals("修改")) {
                        aaa.setId(item.getAsId());
                        asDeviceCommonService.updateById(aaa);
                    }
                }

            });

            diskForHisForProcessService.saveBatch(diskListForHis);
        }


        //20220510
        this.saveProcessFormCustomInst(checkProcessVO.getValue1().getValue(), processInstanceData.getId(), processFormValue2List);
        //
        httpSession.removeAttribute("nextUserVO");

        return true;
    }

    @Override
    public boolean modifyProcessForm(ModifyProcessFormVO modifyProcessFormVO) {
        Integer processFormValue1Id = modifyProcessFormVO.getProcessFormValue1Id();
        //
        System.out.println(modifyProcessFormVO);
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
        //删除diskForHisForProcess
        diskForHisForProcessService.remove(new QueryWrapper<DiskForHisForProcess>().eq("process_instance_data_id", processInstanceData.getId()));
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
                    //
                    String columnNameTmp = (processFormTemplate.getName().split(",")[0]).split("\\.")[3];
                    String columnName = StrUtil.toCamelCase(columnNameTmp);
                    //根据变更字段ID取出processFormValue1中对应的值
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
