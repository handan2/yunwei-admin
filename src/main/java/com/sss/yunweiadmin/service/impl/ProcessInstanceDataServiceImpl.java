package com.sss.yunweiadmin.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.map.MapUtil;
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
import com.sss.yunweiadmin.model.vo.*;
import com.sss.yunweiadmin.service.*;
import org.activiti.engine.TaskService;
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
    @Autowired
    SysUserService sysUserService;
    @Autowired
    SysRoleUserService sysRoleUserService;
    @Autowired
    ProcessDefinitionTaskService processDefinitionTaskService;

    private void setProVarListForChangeDept(ProcessDefinition processDefinition, ProcessInstanceData processInstanceData) {

          /*20220725 todo在这里添加部门变更的逻辑：在Start方法中，先判断流程名字是不是“变更”，然后判断是不是有部门名称，有的话设置两个流程变量#《currentDeptName，XXXX》,#《部门名称，changeDeptNmae
        这两个名称不相等的话，在此处读user表找出新部门的保密员ID,然后再找到“新部门保密员”（这个节点名要约定:新部门保密员）task定义结点，把这个用户信息给他加进去；新部门的部门领导由前一步的保密员选择，这里就不动了
        ; 排他网关上判断这两个是否相等
         */
        if (processDefinition.getProcessType().contains("变更") && !processDefinition.getProcessType().contains("用户")) {//20220725对信息设备及用户变更流程的processType值约定
            List<ProcessInstanceChange> processInstanceChangeList = processInstanceChangeService.list(new QueryWrapper<ProcessInstanceChange>().eq("process_instance_data_id", processInstanceData.getId()).eq("name", "责任部门"));
            if (CollUtil.isNotEmpty(processInstanceChangeList)) {
                ProcessInstanceChange processInstanceChange =processInstanceChangeList.get(0);
                String currentDeptName = processInstanceChange.getOldValue().replace("*", "");//*号机制：为了方便外设流程的相关变更记录：将责任人/部门的old值都加了*
                String changeDeptName = processInstanceChange.getNewValue().replace("*", "");//new值应该本身就没*,这里的替换可能没用
                int deptId = sysDeptService.getOne(new QueryWrapper<SysDept>().eq("name", changeDeptName)).getId();
                List<SysUser> userListForChangeDept = sysUserService.list(new QueryWrapper<SysUser>().eq("dept_id", deptId));
                if (CollUtil.isEmpty(userListForChangeDept))
                    throw new RuntimeException("新部门没有用户！");
                List<Integer> userIdListForChangeDept = userListForChangeDept.stream().map(item -> item.getId()).collect(Collectors.toList());
                List<Integer> userIdListForBaomiUser = sysRoleUserService.list(new QueryWrapper<SysRoleUser>().eq("role_id", 7)).stream().map(item -> item.getUserId()).collect(Collectors.toList());
                List<Integer> baomiUserIdListForChangeDept = userIdListForChangeDept.stream().filter(item -> userIdListForBaomiUser.contains(item)).collect(Collectors.toList());// 也可以用取交集的方法：A.retainAll(B)
                if (CollUtil.isEmpty(baomiUserIdListForChangeDept))
                    throw new RuntimeException("新部门没有保密员，无法审核本流程！");
                int baomiUserIdForChangeDept = baomiUserIdListForChangeDept.get(0);//只取一个保密员
                ProcessDefinitionTask taskForChangeDeptForBaomiUser = processDefinitionTaskService.getOne(new QueryWrapper<ProcessDefinitionTask>().like("task_name", "新部门保密员").eq("process_definition_id", processDefinition.getId()));
                //更改这个Task记录里的处理人配置信息
                taskForChangeDeptForBaomiUser.setOperatorType("用户");
                taskForChangeDeptForBaomiUser.setOperatorTypeValue(baomiUserIdForChangeDept + "");
                taskForChangeDeptForBaomiUser.setOperatorTypeLabel("新部门保密员");
                processDefinitionTaskService.updateById(taskForChangeDeptForBaomiUser);
                List<Task> activeTaskList = workFlowBean.getActiveTask(processInstanceData.getActProcessInstanceId());
                Task activeTask = activeTaskList.get(0);
                Map<String, String> map = new HashMap<>();
                map.put("currentDeptName", currentDeptName);
                map.put("changeDeptName", changeDeptName);
                workFlowBean.setProVarList(activeTask, map);
            }
        }


    }

    private void setProVarListForExGateway(ProcessInstanceData processInstanceData, Task actTask) {

        List<String> proVarList = workFlowBean.getProVarListForExGateway(processInstanceData.getProcessDefinitionId());
        Map<String, Object> mapForProVar = new HashMap<>();
        if (CollUtil.isEmpty(proVarList)) return;
        ProcessFormValue1 processFormValue1 = processFormValue1Service.getOne(new QueryWrapper<ProcessFormValue1>().eq("process_definition_id", processInstanceData.getProcessDefinitionId()).eq("act_process_instance_id", processInstanceData.getActProcessInstanceId()));
        List<ProcessFormValue2> formValue2List = processFormValue2Service.list(new QueryWrapper<ProcessFormValue2>().eq("form_value1_id", processFormValue1.getId()));
        if (CollUtil.isEmpty(formValue2List)) return;
        List<AsConfig> asConfigList = asConfigService.list(new QueryWrapper<AsConfig>().select("distinct en_table_name,zh_table_name"));
        JSONObject jsonObject = JSONObject.parseObject(processFormValue1.getValue());
        //20220722处理非变更字段中的proVar
        List<ProcessFormTemplate> formTemplateListForBasicColumn = processFormTemplateService.list(new QueryWrapper<ProcessFormTemplate>().eq("process_definition_id", processInstanceData.getProcessDefinitionId()).eq("flag", "基本类型").orderByAsc("name"));
        for (ProcessFormTemplate processFormTemplate : formTemplateListForBasicColumn) {// formTemplateListForBasicColumn顶多会是空数组
            String pageValue;
            Integer id = processFormTemplate.getId();
            if (processFormTemplate.getType().equals("日期")) {
                pageValue = jsonObject.getString(id + "Date");
            } else if (processFormTemplate.getType().equals("日期时间")) {
                pageValue = jsonObject.getString(id + "Datetime");
            } else {
                pageValue = jsonObject.getString(id + "");
            }
            if (proVarList.contains(processFormTemplate.getLabel())) {
                mapForProVar.put(processFormTemplate.getLabel(), pageValue);
            }
        }
        //取出所有的变更字段
        List<ProcessFormTemplate> formTemplateList = processFormTemplateService.list(new QueryWrapper<ProcessFormTemplate>().eq("process_definition_id", processInstanceData.getProcessDefinitionId()).eq("flag", "字段变更类型").orderByAsc("name"));
        //组装map <自定义表ID，List<template>>
        Map<Integer, List<ProcessFormTemplate>> map = new HashMap<>();
        for (ProcessFormValue2 processFormValue2 : formValue2List) {
            List<ProcessFormTemplate> list = formTemplateList.stream().filter(item -> (item.getName().split("\\.")[0]).equals(processFormValue2.getCustomTableId() + "")).collect(Collectors.toList());
            map.put(processFormValue2.getCustomTableId(), list);
        }
        //遍历map
        for (Map.Entry<Integer, List<ProcessFormTemplate>> entry : map.entrySet()) {
            //组装map2  <基本表ID，List<template>>
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
                for (ProcessFormTemplate processFormTemplate : entry2.getValue()) {
                    //name,baomi_no
                    String columnName = (processFormTemplate.getName().split(",")[0]).split("\\.")[3];
                    // String columnName = StrUtil.toCamelCase(columnNameTmp);
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
                    if (ObjectUtil.isNotEmpty(pageValue)) {//这是变更字段存在值的分支
//                        //20220708 todo断点，这个位置变更字段的名称与值都有了，现在要（增加一个函数调用他）寻找流程定义中所有的排他网关节点及节点的引线/edge，将这些Edge上的parmName读出来
                        String tableName = entry2.getKey();
                        //    String tableName = StrUtil.toCamelCase(tableNameTmp);
                        AsConfig asConfig = asConfigService.getOne(new QueryWrapper<AsConfig>().eq("en_table_name", tableName).eq("en_column_name", columnName));
                        String columnZhName = asConfig.getZhColumnName();
                        if (proVarList.contains(columnZhName)) {
                            mapForProVar.put(columnZhName, pageValue);
                        }

//
//                        }

                    }
                }

            }

        }
        if (MapUtil.isNotEmpty(mapForProVar))
            workFlowBean.setProVarList(actTask, mapForProVar);
    }

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
    public boolean endAndStart(EndAndStartProcessVO endAndStartProcessVO) {
        //结束老流程
        ProcessInstanceData preProcessInstanceData = processInstanceDataService.getById(endAndStartProcessVO.getPreProcessInstDataId());
        String preActProcessInstanceId = preProcessInstanceData.getActProcessInstanceId();
        String processStatus = preProcessInstanceData.getProcessStatus();
        //取出我的一个任务
        List<Task> myTaskListForPre = workFlowBean.getMyTask(preActProcessInstanceId);
        Task myTaskForPre = myTaskListForPre.get(0);
        String postProcessName = (String) workFlowBean.getProcessVariable(myTaskForPre);
        //完成任务
        workFlowBean.completeTask(preProcessInstanceData.getProcessDefinitionId(), myTaskForPre);
        preProcessInstanceData.setEndDatetime(LocalDateTime.now());
        preProcessInstanceData.setProcessStatus("完成");
        preProcessInstanceData.setDisplayCurrentStep("");
        preProcessInstanceData.setLoginCurrentStep("");
        processInstanceDataService.updateById(preProcessInstanceData);
        //插入流程节点数据
        SysUser userForPre = (SysUser) httpSession.getAttribute("user");
        SysDept deptForPre = sysDeptService.getById(userForPre.getDeptId());
        ProcessInstanceNode preProcessInstanceNode = new ProcessInstanceNode();
        //BeanUtils.copyProperties(checkProcessVO, processInstanceNode);//20220617增：关于processInstanceNode的属性自动赋值:实际上是可以用的：因为前端已经从values里把那些“非全局表单元素”/node相关的值删除了（当然这样的结果是，换一个流程节点相关form组件里没有初始值了：当然没有也是合理的）：
        preProcessInstanceNode.setProcessInstanceDataId(preProcessInstanceData.getId());
        preProcessInstanceNode.setTaskDefKey(myTaskForPre.getTaskDefinitionKey());
        preProcessInstanceNode.setTaskName(myTaskForPre.getName());
        preProcessInstanceNode.setDisplayName(userForPre.getDisplayName());
        preProcessInstanceNode.setLoginName(userForPre.getLoginName());
        preProcessInstanceNode.setDeptName(deptForPre.getName());
        //
        HistoricTaskInstance historicTaskInstance = workFlowBean.getHistoricTaskInstance(preProcessInstanceData.getActProcessInstanceId(), myTaskForPre.getTaskDefinitionKey());
        Date startDateTime = historicTaskInstance.getStartTime();
        Date endDateTime = historicTaskInstance.getEndTime();
        //
        preProcessInstanceNode.setStartDatetime(LocalDateTime.ofInstant(startDateTime.toInstant(), ZoneId.systemDefault()));
        preProcessInstanceNode.setEndDatetime(LocalDateTime.ofInstant(endDateTime.toInstant(), ZoneId.systemDefault()));
        preProcessInstanceNode.setButtonName("提交");
        preProcessInstanceNode.setOperate(postProcessName);
        processInstanceNodeService.save(preProcessInstanceNode);
        changeColumnForHandle(preProcessInstanceData, true);
        //20220621 保存硬盘信息
        //先删除DiskForHisForProcess表原记录
        diskForHisForProcessService.remove(new QueryWrapper<DiskForHisForProcess>().eq("process_instance_data_id", preProcessInstanceData.getId()));
        List<DiskForHisForProcess> diskListForHisForPre = diskForHisForProcessService.list(new QueryWrapper<DiskForHisForProcess>().eq("process_instance_data_id", preProcessInstanceData.getId()));
        List<AsDeviceCommon> asDeviceCommonList = null;
        if (CollUtil.isNotEmpty(diskListForHisForPre)) {
            AsDeviceCommon asDeviceCommon = asDeviceCommonService.getById(diskListForHisForPre.get(0).getHostAsId());
            diskListForHisForPre.stream().forEach(item -> {
                if (workFlowBean.isFinish(preProcessInstanceData.getActProcessInstanceId())) {
                    AsDeviceCommon tmp = new AsDeviceCommon();
                    BeanUtils.copyProperties(item, tmp);
                    // BeanUtils.copyProperties(item, itemNew);
                    if (item.getFlag().equals("新增")) {
                        tmp.setUserName(asDeviceCommon.getUserName());
                        tmp.setUserDept(asDeviceCommon.getUserDept());
                        tmp.setUserMiji(asDeviceCommon.getUserMiji());
                        tmp.setNetType(asDeviceCommon.getNetType());
                        tmp.setName("硬盘");
                        tmp.setTypeId(30);
                        asDeviceCommonService.save(tmp);
                        item.setAsId(tmp.getId());
                    } else if (item.getFlag().equals("修改")) {
                        tmp.setId(item.getAsId());
                        asDeviceCommonService.updateById(tmp);
                    }
                }
            });
            diskForHisForProcessService.saveBatch(diskListForHisForPre);
        }
        List<ProcessFormValue2> preProcessFormValue2List = processFormValue2Service.list(new QueryWrapper<ProcessFormValue2>().eq("act_process_instance_id", preActProcessInstanceId));
        //processFormValue1.value
        ProcessFormValue1 preProcessFormValue1 = processFormValue1Service.getOne(new QueryWrapper<ProcessFormValue1>().eq("act_process_instance_id", preActProcessInstanceId));
        this.saveProcessFormCustomInst(preProcessFormValue1.getValue(), preProcessInstanceData.getId(), preProcessFormValue2List);
        //
        httpSession.removeAttribute("nextUserVO");

        //开启新流程
        //保存processFormValue1
        ProcessFormValue1 processFormValue1 = new ProcessFormValue1();
        BeanUtils.copyProperties(endAndStartProcessVO.getValue1(), processFormValue1);
        processFormValue1Service.save(processFormValue1);
        //endAndStartProcessVO.getValue1().setId(processFormValue1.getId());
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
        List<ProcessFormValue2> processFormValue2List = endAndStartProcessVO.getValue2List();
        //20220528加判空
        if (CollUtil.isNotEmpty(processFormValue2List)) {
            processFormValue2List.forEach(item -> {

                item.setProcessDefinitionId(processFormValue1.getProcessDefinitionId());
                item.setActProcessInstanceId(actProcessInstance.getId());
                item.setFormValue1Id(processFormValue1.getId());
            });
            processFormValue2Service.saveBatch(processFormValue2List);
        }
        //nextUserVO放入session
        if (endAndStartProcessVO.getHaveNextUser().equals("是")) {
            NextUserVO nextUserVO = new NextUserVO(endAndStartProcessVO.getOperatorType(), endAndStartProcessVO.getOperatorTypeValue(), endAndStartProcessVO.getHaveNextUser());
            httpSession.setAttribute("nextUserVO", nextUserVO);
        }
        //第二个ActTask创建；20220608 startEvent执行完后activeTask，一般只会有一activeTask(即“发起者”那个节点)&&流程的发起者一般也同样是“发起者”那个处理候选人：直接查activeTask即可：不过暂不改
        List<Task> myTaskList = workFlowBean.getMyTask(actProcessInstance.getId());
        Task myTask = myTaskList.get(0);
        //20220710以下前置，为了setProVarListForExGateway
        //插入流程实例数据
        ProcessInstanceData processInstanceData = new ProcessInstanceData();
        processInstanceData.setProcessDefinitionId(processDefinition.getId());
        //20211203根据流程实例命名规则及表单提交者确定名称
        processInstanceData.setProcessName(getProcessName(endAndStartProcessVO, processDefinition));
        processInstanceData.setBusinessId(processFormValue1.getId());
        processInstanceData.setActProcessInstanceId(actProcessInstance.getId());
        processInstanceData.setProcessStatus("审批中");
        processInstanceData.setPreProcessInstanceId(preProcessInstanceData.getId());
        processInstanceDataService.save(processInstanceData);//保存成功后mybatis会把主键/ID传回参数processInstanceData中
        setProVarListForExGateway(processInstanceData, myTask);//20220710加

        if (ObjectUtil.isNotEmpty(endAndStartProcessVO.getButtonName())) {
            workFlowBean.completeTaskByParam(processDefinition.getId(), myTask, endAndStartProcessVO.getButtonName(), null);//20220628占个位
        } else {
            workFlowBean.completeTask(processDefinition.getId(), myTask);
        }
        //跳转到ActEventListener,设置下一个节点的处理人（如果下一个节点是当前节点<发起人>指定的，那会用到session/"nextUserVO"）
        //20211216注意以上代码总共会引发创建两个activiTask:发起人task及下一步的task:两次进入监听器

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
        processInstanceDataService.updateById(processInstanceData);//20220710 save前置，这里改update
        //20220510
        this.saveProcessFormCustomInst(processFormValue1.getValue(), processInstanceData.getId(), processFormValue2List);
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
        if (ObjectUtil.isNotEmpty(endAndStartProcessVO.getButtonName())) {
            processInstanceNode.setButtonName(endAndStartProcessVO.getButtonName());
        } else {
            processInstanceNode.setButtonName("提交");
        }
        //20220703注释掉
//        if (ObjectUtil.isNotEmpty(endAndStartProcessVO.getHaveNextUser()) && endAndStartProcessVO.getHaveNextUser().equals("是")) {
//            processInstanceNode.setOperatorType(endAndStartProcessVO.getOperatorType());
//            processInstanceNode.setOperatorTypeValue(endAndStartProcessVO.getOperatorTypeValue());
//            processInstanceNode.setOperatorTypeLabel(endAndStartProcessVO.getOperatorTypeLabel());
//        }
        processInstanceNodeService.save(processInstanceNode);
        //把ID记录入preProInstData
        preProcessInstanceData.setPostProcessInstanceId(processInstanceData.getId());
        processInstanceDataService.updateById(preProcessInstanceData);
        //20220620 保存硬盘信息
        List<DiskForHisForProcess> diskListForHisForProcess = endAndStartProcessVO.getDiskListForHisForProcess();
        //目前没有“删除”,第一次提交只有（对DiskForHisForProcess表来说）新增，对AsDeviceCommon表来说有“新增/编辑”
        if (CollUtil.isNotEmpty(diskListForHisForProcess)) {//20220701 diskListForHisForProcess初始的数据来源为后台service.list():查不到他也会返回空LIST对象（而不是null）
            diskListForHisForProcess.forEach(item -> {
                item.setProcessInstanceDataId(processInstanceData.getId());
            });
            diskForHisForProcessService.saveBatch(diskListForHisForProcess);//20220614这个flag设置有点小问题：暂不改
        }
        //变更字段
        changeColumnForStart(processInstanceData, processFormValue1, processFormValue2List);
        //
        httpSession.removeAttribute("nextUserVO");
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public synchronized boolean start(StartProcessVO startProcessVO) {
        //保存processFormValue1
        ProcessFormValue1 processFormValue1 = new ProcessFormValue1();
        BeanUtils.copyProperties(startProcessVO.getValue1(), processFormValue1);
        processFormValue1Service.save(processFormValue1);
        // startProcessVO.getValue1().setId(processFormValue1.getId());
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
                item.setProcessDefinitionId(processFormValue1.getProcessDefinitionId());
                item.setActProcessInstanceId(actProcessInstance.getId());
                item.setFormValue1Id(processFormValue1.getId());
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
        //20220710  processInstanceData的写入DB前置到这里，为了
        //插入流程实例数据
        ProcessInstanceData processInstanceData = new ProcessInstanceData();
        processInstanceData.setProcessDefinitionId(processDefinition.getId());
        //20211203根据流程实例命名规则及表单提交者确定名称
        processInstanceData.setProcessName(getProcessName(startProcessVO, processDefinition));
        processInstanceData.setBusinessId(processFormValue1.getId());
        processInstanceData.setActProcessInstanceId(actProcessInstance.getId());
        processInstanceData.setProcessStatus("审批中");//20220710 流程用户节点只有“发起”一个时，这个状态可能会出错：暂不考虑这种情况
        processInstanceDataService.save(processInstanceData);//保存成功后mybatis会把主键/ID传回参数processInstanceData中
        setProVarListForExGateway(processInstanceData, myTask);//20220710加

        if (ObjectUtil.isNotEmpty(startProcessVO.getButtonName())) {
            workFlowBean.completeTaskByParam(processDefinition.getId(), myTask, startProcessVO.getButtonName(), null);//20220628占个位
        } else {
            workFlowBean.completeTask(processDefinition.getId(), myTask);
        }
        //跳转到ActEventListener,设置下一个节点的处理人（如果下一个节点是当前节点<发起人>指定的，那会用到session/"nextUserVO"）
        //20211216注意以上代码总共会引发创建两个activiTask:发起人task及下一步的task:两次进入监听器
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
        //20220710 save前置，这里加个update
        processInstanceDataService.updateById(processInstanceData);
        //20220510
        this.saveProcessFormCustomInst(processFormValue1.getValue(), processInstanceData.getId(), processFormValue2List);
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
        //20220703注释掉
//        if (ObjectUtil.isNotEmpty(startProcessVO.getHaveNextUser()) && startProcessVO.getHaveNextUser().equals("是")) {
//            processInstanceNode.setOperatorType(startProcessVO.getOperatorType());
//            processInstanceNode.setOperatorTypeValue(startProcessVO.getOperatorTypeValue());
//            processInstanceNode.setOperatorTypeLabel(startProcessVO.getOperatorTypeLabel());
//        }
        processInstanceNodeService.save(processInstanceNode);
        //20220620 保存硬盘信息
        List<DiskForHisForProcess> diskListForHisForProcess = startProcessVO.getDiskListForHisForProcess();
        //目前没有“删除”,第一次提交只有（对DiskForHisForProcess表来说）新增，对AsDeviceCommon表来说有“新增/编辑”
        if (CollUtil.isNotEmpty(diskListForHisForProcess)) {//20220701 diskListForHisForProcess初始的数据来源为后台service.list():查不到他也会返回空LIST对象（而不是null）
            diskListForHisForProcess.forEach(item -> {
                item.setProcessInstanceDataId(processInstanceData.getId());
            });
            diskForHisForProcessService.saveBatch(diskListForHisForProcess);//20220614这个flag设置有点小问题：暂不改
        }
        //变更字段
        changeColumnForStart(processInstanceData, processFormValue1, processFormValue2List);
        //20220725 部门变更相关流程变量的设置
        setProVarListForChangeDept(processDefinition, processInstanceData);
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
        Task myTask = myTaskList.get(0);//20220711感觉当前用户如果有两个待办结点，那本程序执行逻辑就有问题了：todo后续有时间再细想
        //20220711 从task里找出流程定义信息-->找出(自定义的)task定义相关信息：取代checkVo从前端读的部分
        //20220709设置流程变量
        setProVarListForExGateway(processInstanceData, myTask);
        //完成任务
        if (ObjectUtil.isNotEmpty(checkProcessVO.getButtonName()) || checkProcessVO.getHaveSelectProcess().equals("是")) {
            String selectedProcess = checkProcessVO.getSelectedProcess();
            if (checkProcessVO.getHaveSelectProcess().equals("是") && ObjectUtil.isEmpty(checkProcessVO.getSelectedProcess()))
                selectedProcess = "";//20220629 如果bpmnxml里流程变量值的判断条件可以是“！=null”,那这两个判断条件可以合并：todor后续试验验证
            else if (checkProcessVO.getHaveSelectProcess().equals("是") && ObjectUtil.isNotEmpty(checkProcessVO.getSelectedProcess())) {
                selectedProcess = checkProcessVO.getSelectedProcess();
                // processInstanceData.setPreProcessInstanceId();
            }
            //20220709注意这个completeTaskByParam仅包含buttonNmae/selectedProcess两种机制
            workFlowBean.completeTaskByParam(processInstanceData.getProcessDefinitionId(), myTask, checkProcessVO.getButtonName(), selectedProcess);
        } else
            workFlowBean.completeTask(processInstanceData.getProcessDefinitionId(), myTask);
        //跳转到ActEventListener,设置下一个节点的处理人
        //更新流程实例
        if (workFlowBean.isFinish(processInstanceData.getActProcessInstanceId())) {//2020621finish改成isFinish
            processInstanceData.setEndDatetime(LocalDateTime.now());
            processInstanceData.setProcessStatus("完成");
            processInstanceData.setDisplayCurrentStep("完成");//20220726由“”改成“完成”
            processInstanceData.setLoginCurrentStep("完成");
        } else {
            Map<String, String> stepMap = workFlowBean.getCurrentStep(processInstanceData.getProcessDefinitionId(), processInstanceData.getId(), processInstanceData.getActProcessInstanceId(), myTask.getTaskDefinitionKey());
            processInstanceData.setDisplayCurrentStep(stepMap.get("displayName"));
            processInstanceData.setLoginCurrentStep(stepMap.get("loginName"));
            //注意：下面这个task可能只是当前并发任务中的一个：但也无妨，因为他是为了判断退回，退回连线约定不能放在并发任务（如并行网关）结点中；如果碰到这种情况，Aciviti运行可能报错，但暂时不考虑这种情况了
            Task activieTask = workFlowBean.getActiveTask(processInstanceData.getActProcessInstanceId()).get(0);
            //
            /*20220725 todo在这里添加部门变更的逻辑：在Start方法中，先判断流程名字是不是“变更”，然后判断是不是有部门名称，有的话设置两个流程变量#《currentDeptName，XXXX》,#《部门名称，changeDeptNmae
            这两个名称不相等的话，在此处读user表找出新部门的保密员ID,然后再找到“新部门保密员”（这个节点名要约定）task定义结点，把这个用户信息给他加进去；新部门的部门领导由前一步的保密员选择，这里就不动了

            ; 排他网关上判断这两个是否相等

             */

            String currentTaskDefKey = activieTask.getTaskDefinitionKey();
            if (workFlowBean.getReturnedTaskEdge(processInstanceData.getProcessDefinitionId(), myTask.getTaskDefinitionKey(), currentTaskDefKey) != null) {
                processInstanceData.setProcessStatus("退回");
            } else {
                Object nextProcessObj = workFlowBean.getProcessVariable(activieTask);
                ProcessDefinitionTask feedBackUserTask = workFlowBean.getFeedBackUserTask(processInstanceData.getProcessDefinitionId());
                if (ObjectUtil.isNotEmpty(nextProcessObj) && ObjectUtil.isNotEmpty(feedBackUserTask)) {
                    if (currentTaskDefKey.equals(feedBackUserTask.getTaskDefKey())) {
                        System.out.println((String) workFlowBean.getProcessVariable(activieTask));
                        processInstanceData.setProcessStatus("新任务");
                    }
                } else if (stepMap.get("displayName").contains("处理")) {
                    processInstanceData.setProcessStatus("处理中"); //2021117对于处理节点，要改成“处理中”，这个值有利于前端动态渲染“审批/处理按钮”名字时判断
                } else {
                    processInstanceData.setProcessStatus("审批中");
                }
            }
        }
        processInstanceDataService.updateById(processInstanceData);
        //插入流程节点数据
        SysUser user = (SysUser) httpSession.getAttribute("user");
        SysDept dept = sysDeptService.getById(user.getDeptId());
        ProcessInstanceNode processInstanceNode = new ProcessInstanceNode();
        //BeanUtils.copyProperties(checkProcessVO, processInstanceNode);//20220617增：关于processInstanceNode的属性自动赋值:实际上是可以用的：因为前端已经从values里把那些“非全局表单元素”/node相关的值删除了（当然这样的结果是，换一个流程节点相关form组件里没有初始值了：当然没有也是合理的）：
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
        //20220703注释掉
//        if (ObjectUtil.isNotEmpty(checkProcessVO.getHaveNextUser()) && checkProcessVO.getHaveNextUser().equals("是")) {
//            processInstanceNode.setOperatorType(checkProcessVO.getOperatorType());
//            processInstanceNode.setOperatorTypeValue(checkProcessVO.getOperatorTypeValue());
//            processInstanceNode.setOperatorTypeLabel(checkProcessVO.getOperatorTypeLabel());
//        }
        if (ObjectUtil.isNotEmpty(checkProcessVO.getComment()) && checkProcessVO.getHaveComment().equals("是")) {
            processInstanceNode.setComment(checkProcessVO.getComment());
        } else {
            if (processInstanceNode.getButtonName().contains("同意")) {//20211117修改退回时，意见也是同意的问题
                processInstanceNode.setComment("同意");
            } else if (processInstanceNode.getButtonName().contains("提交")) {//20220726：“处理”改“提交”
                processInstanceNode.setComment("已处理");
            }
        }
        if (ObjectUtil.isNotEmpty(checkProcessVO.getHaveOperate()) && checkProcessVO.getHaveOperate().equals("是")) {
            processInstanceNode.setOperate(checkProcessVO.getOperate());
        }
        if (ObjectUtil.isNotEmpty(checkProcessVO.getSelectedProcess()) && checkProcessVO.getHaveSelectProcess().equals("是")) {
            processInstanceNode.setSelectedProcess(checkProcessVO.getSelectedProcess());
        }
        processInstanceNodeService.save(processInstanceNode);
        //processFormValue1.value
        ProcessFormValue1 processFormValue1 = processFormValue1Service.getOne(new QueryWrapper<ProcessFormValue1>().eq("act_process_instance_id", actProcessInstanceId));
        processFormValue1.setValue(checkProcessVO.getValue1().getValue());
        processFormValue1Service.updateById(processFormValue1);
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
        //变更字段 20220701下移到这里，因为这时 processFormValue1已经写到DB中了，下面的方法是根据processInstanceDataID找的value1表查的：正好也验证下在事务机制下：此时能不能从DB中读出刚改入的值：Y
        changeColumnForHandle(processInstanceData, workFlowBean.isFinish(processInstanceData.getActProcessInstanceId()));
        //20220621 保存硬盘信息
        //先删除DiskForHisForProcess表原记录
        diskForHisForProcessService.remove(new QueryWrapper<DiskForHisForProcess>().eq("process_instance_data_id", processInstanceData.getId()));
        List<DiskForHisForProcess> diskListForHis = checkProcessVO.getDiskListForHisForProcess();
        List<AsDeviceCommon> asDeviceCommonList = null;
        if (CollUtil.isNotEmpty(diskListForHis)) {
            AsDeviceCommon asDeviceCommon = asDeviceCommonService.getById(diskListForHis.get(0).getHostAsId());
            diskListForHis.stream().forEach(item -> {
                if (workFlowBean.isFinish(processInstanceData.getActProcessInstanceId())) {
                    AsDeviceCommon tmp = new AsDeviceCommon();
                    BeanUtils.copyProperties(item, tmp);
                    // BeanUtils.copyProperties(item, itemNew);
                    if (item.getFlag().equals("新增")) {
                        tmp.setUserName(asDeviceCommon.getUserName());
                        tmp.setUserDept(asDeviceCommon.getUserDept());
                        tmp.setUserMiji(asDeviceCommon.getUserMiji());
                        tmp.setNetType(asDeviceCommon.getNetType());
                        tmp.setName("硬盘");
                        tmp.setTypeId(30);
                        asDeviceCommonService.save(tmp);
                        item.setAsId(tmp.getId());
                    } else if (item.getFlag().equals("修改")) {
                        tmp.setId(item.getAsId());
                        asDeviceCommonService.updateById(tmp);
                    }
                }
            });
            diskForHisForProcessService.saveBatch(diskListForHis);
        }
        //20220510
        this.saveProcessFormCustomInst(checkProcessVO.getValue1().getValue(), processInstanceData.getId(), processFormValue2List);
        //20220725 部门变更相关流程变量的设置
        setProVarListForChangeDept(processDefinitionService.getById(processInstanceData.getProcessDefinitionId()), processInstanceData);
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
        //20220716
        ProcessDefinition processDefinition = processDefinitionService.getById(processInstanceData.getProcessDefinitionId());

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
                            //20220722增加“DeviceCommon表中责任人、责任部门”强制记录的干预:在原值前强制加一个“*”
                            if (processFormTemplate.getLabel().split("\\.")[1].equals("责任人") || processFormTemplate.getLabel().split("\\.")[1].equals("责任部门"))
                                dbValue = dbValue + "*";
                        }
                        if (!pageValue.equals(dbValue)) {
                            //变更字段
                            ProcessInstanceChange change = new ProcessInstanceChange();
                            //20220716 设置生命周期状态;
                            AsDeviceCommon asDeviceCommon = asDeviceCommonService.getById(asId);
                            List<Integer> typeIdList = asTypeService.getTypeIdList(processDefinition.getAsTypeId());//获取流程定义里的“主设备”类型(及其子类)List
                            if (typeIdList.contains(asDeviceCommon.getTypeId()))//如果当前资产是“主类型”，生命周期类型为流程定义中的processType,否则为processType2
                                change.setLifteCycle(processDefinition.getProcessType());
                            else
                                change.setLifteCycle(processDefinition.getProcessType2());


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

    private void changeColumnForHandle(ProcessInstanceData processInstanceData, boolean isFinish) {
        //先删除变更记录
        processInstanceChangeService.remove(new QueryWrapper<ProcessInstanceChange>().eq("process_instance_data_id", processInstanceData.getId()));
        //20220716
        ProcessDefinition processDefinition = processDefinitionService.getById(processInstanceData.getProcessDefinitionId());

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
        //组装map <自定义表ID，List<template>>
        Map<Integer, List<ProcessFormTemplate>> map = new HashMap<>();
        for (ProcessFormValue2 processFormValue2 : formValue2List) {
            List<ProcessFormTemplate> list = formTemplateList.stream().filter(item -> (item.getName().split("\\.")[0]).equals(processFormValue2.getCustomTableId() + "")).collect(Collectors.toList());
            map.put(processFormValue2.getCustomTableId(), list);
        }
        //遍历map
        for (Map.Entry<Integer, List<ProcessFormTemplate>> entry : map.entrySet()) {
            Integer asId = asIdMap.get(entry.getKey());
            //组装map2  <基本表ID，List<template>>
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
                    if (ObjectUtil.isNotEmpty(pageValue)) {//这是变更字段存在值的分支

                        Object dbValueObj = ReflectUtil.getFieldValue(dbObject, columnName);
                        String dbValue = null;
                        if (ObjectUtil.isEmpty(dbValueObj)) {
                            dbValue = "";
                        } else {
                            dbValue = dbValueObj.toString();
                            //20220722增加“DeviceCommon表中责任人、责任部门”强制记录的干预:在原值前强制加一个“*”
                            if (processFormTemplate.getLabel().split("\\.")[1].equals("责任人") || processFormTemplate.getLabel().split("\\.")[1].equals("责任部门"))
                                dbValue = dbValue + "*";
                        }
                        if (!pageValue.equals(dbValue)) {
                            //变更字段
                            ProcessInstanceChange change = new ProcessInstanceChange();
                            //20220716 设置生命周期状态
                            AsDeviceCommon asDeviceCommon = asDeviceCommonService.getById(asId);
                            List<Integer> typeIdList = asTypeService.getTypeIdList(processDefinition.getAsTypeId());//获取流程定义里的“主设备”类型(及其子类)List
                            if (typeIdList.contains(asDeviceCommon.getTypeId()))//如果当前资产是“主类型”，生命周期类型为流程定义中的processType,否则为processType2
                                change.setLifteCycle(processDefinition.getProcessType());
                            else
                                change.setLifteCycle(processDefinition.getProcessType2());

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
                            //数据对象
                            if (isFinish) {//20220713 有问题todo断点 就没有“变更硬盘”这个字段
                                ReflectUtil.setFieldValue(dbObject, columnName, pageValue);
                                if (columnName.contains("变更硬盘") || columnName.contains("硬盘变更"))//20220625加
                                    ReflectUtil.setFieldValue(dbObject, columnName, "");
                            }
                        }
                    }
                }
                //数据对象
                if (isFinish) {
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



