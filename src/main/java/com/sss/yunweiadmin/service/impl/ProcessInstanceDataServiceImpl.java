package com.sss.yunweiadmin.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ObjUtil;
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
import com.sss.yunweiadmin.model.vo.*;
import com.sss.yunweiadmin.service.*;
import lombok.SneakyThrows;
import org.activiti.engine.history.HistoricTaskInstance;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpSession;
import java.io.File;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    @Autowired
    InfoNoService infoNoService;
    @Autowired
    RecordForReportService recordForReportService;
    @Autowired
    AsComputerSpecialService asComputerSpecialService;
    @Autowired
    Environment environment;
    @Autowired
    AttachmentService attachmentService;

    // 寻找最后一个英文句点后的内容
    public  String subStringByLastPeriod(String input,String direction) {
        // 寻找最后一个英文句点的位置
        int lastPeriodIndex = input.lastIndexOf('.');
        // 如果找到了句点且句点不在字符串的最后
        if (lastPeriodIndex != -1 && lastPeriodIndex < input.length() - 1) {
            if("right".equals(direction)){
                // 获取句点右面的内容
                String lastSentence = input.substring(lastPeriodIndex + 1).trim();
                return "."+lastSentence;
            } else {//左边：不含句点
                String lastSentence = input.substring(0,lastPeriodIndex ).trim();
                return lastSentence;
            }

        } else {
            // 没有找到句点，或者句点在字符串的最后，返回空字符串或原始字符串，视情况而定
            return "";
        }
    }
    //维护硬盘列表相关的变更记录及vlaue2表的值;startAndEnd暂没凋用：有空做吧
    public void setDiskListChangeAndValue2(List<DiskForHisForProcess> diskListForHisForProcess, ProcessInstanceData processInstanceData, ProcessDefinition processDefinition, ProcessFormValue1 processFormValue1, SysUser user, SysDept dept){
        //20220620 保存硬盘信息
       // List<DiskForHisForProcess> diskListForHisForProcess = startProcessVO.getDiskListForHisForProcess();
        diskForHisForProcessService.remove(new QueryWrapper<DiskForHisForProcess>().eq("process_instance_data_id", processInstanceData.getId()));//对于start无意义

        //目前没有“删除”,第一次提交只有（对DiskForHisForProcess表来说）新增，对AsDeviceCommon表来说有“新增/编辑”
        if (CollUtil.isNotEmpty(diskListForHisForProcess)) {//20220701 diskListForHisForProcess初始的数据来源为后台service.list():查不到他也会返回空LIST对象（而不是null）
            List<ProcessFormValue2> processFormValue2ListForDisk = new ArrayList<>();
            List<ProcessInstanceChange> processInstanceChangeListForDisk = new ArrayList<>();//start时无意义
            AsDeviceCommon asDeviceCommon = asDeviceCommonService.getById(diskListForHisForProcess.get(0).getHostAsId());
            diskListForHisForProcess.forEach(item -> {
                item.setProcessInstanceDataId(processInstanceData.getId());//仅start时有意义
                //20230729 硬盘列表写入value2;starAndEnd我也不准确写
                ProcessInstanceChange processInstanceChangeForDisk = new ProcessInstanceChange();
                ProcessFormValue2 processFormValue2 = new ProcessFormValue2();
                processFormValue2.setCustomTableId(39);//20230729硬盘列表专用（ID为39
                processFormValue2.setProcessDefinitionId(processFormValue1.getProcessDefinitionId());
                processFormValue2.setActProcessInstanceId(processInstanceData.getActProcessInstanceId());
                processFormValue2.setFormValue1Id(processFormValue1.getId());
                //processFormValue2.setAsId(item.getAsId());//start时因为新增的硬盘没有写入AsDeviceCommon，故这时asID没值（默认是0）; 在流程finish时才（真正写入db+
                // ）有值
                if (workFlowBean.isFinish(processInstanceData.getActProcessInstanceId()) && item.getFlag() != null) { //20231005 start节点(且下一节点就是结束)时：item.getFlag() == null：不深研
                    AsDeviceCommon tmp = new AsDeviceCommon();
                    BeanUtils.copyProperties(item, tmp);
                    processInstanceChangeForDisk.setProcessInstanceDataId(processInstanceData.getId());
                    processInstanceChangeForDisk.setActProcessInstanceId(processInstanceData.getActProcessInstanceId());
                    processInstanceChangeForDisk.setModifyDatetime(LocalDateTime.now());
                    processInstanceChangeForDisk.setLifteCycle(processDefinition.getProcessType());//暂用“主自定义表”的生命周期类型：目前取值范围定密、维修
                    processInstanceChangeForDisk.setName("状态（硬盘）");//硬盘列表的专用“状态”字段名：暂仅变更这一个
                    processInstanceChangeForDisk.setNewValue(item.getState());
                    processInstanceChangeForDisk.setIsFinish("是");//20230730只在finish时才把这个processInstanceChangeForDisk添加入新增列表
                    processInstanceChangeForDisk.setDeptName(dept.getName());
                    processInstanceChangeForDisk.setDisplayName(user.getDisplayName());
                    processInstanceChangeForDisk.setLoginName(user.getLoginName());
                    if (item.getFlag().equals("新增")) {
                        //20230108 todo增加序列号判重；modify方法也要照着添加；“填错”状态的SN不在比对范围
                        tmp.setUserName(asDeviceCommon.getUserName());
                        tmp.setUserDept(asDeviceCommon.getUserDept());
                        tmp.setUserMiji(asDeviceCommon.getUserMiji());
                        tmp.setNetType(asDeviceCommon.getNetType());
                        tmp.setName("硬盘");
                        tmp.setTypeId(30);
                        asDeviceCommonService.save(tmp);
                        item.setAsId(tmp.getId());
                        processFormValue2.setAsId(tmp.getId());//20230730
                        processInstanceChangeForDisk.setAsId(tmp.getId());
                        processInstanceChangeForDisk.setNewValue(item.getState()+"（新增）");
                        processInstanceChangeListForDisk.add(processInstanceChangeForDisk);
                    } else if (item.getFlag().equals("修改")) {
                        tmp.setId(item.getAsId());
                        processInstanceChangeForDisk.setAsId(item.getAsId());
                        processFormValue2.setAsId(item.getAsId());
                        //20230730 硬盘状态的原值记录
                        AsDeviceCommon asDeviceCommonForDisk = asDeviceCommonService.getById(item.getAsId());
                        processInstanceChangeForDisk.setOldValue(asDeviceCommonForDisk.getState());

                        if(!item.getState().equals(asDeviceCommonForDisk.getState()))
                            processInstanceChangeListForDisk.add(processInstanceChangeForDisk);
                        asDeviceCommonService.updateById(tmp);
                    }
                }


                processFormValue2ListForDisk.add(processFormValue2);

            });
            //20230729先删除旧的value2/硬盘ID;约定“硬盘列表信息表”专用自定义表ID 39; 这个逻辑只在处理节点有用
            processFormValue2Service.remove(new QueryWrapper<ProcessFormValue2>().eq("act_process_instance_id",processInstanceData.getActProcessInstanceId()).eq("custom_table_id",39));//start时无意义
            processFormValue2Service.saveBatch(processFormValue2ListForDisk);
            diskForHisForProcessService.saveBatch(diskListForHisForProcess);
            processInstanceChangeService.remove(new QueryWrapper<ProcessInstanceChange>().eq("act_process_instance_id",processInstanceData.getActProcessInstanceId()).eq("name","状态（硬盘）"));//start时无意义
            processInstanceChangeService.saveBatch(processInstanceChangeListForDisk);//硬盘变更记录 //start时无意义

        }
    }

    //20221213 写入”报表记录表“：
    public void saveForReport(String value1, Integer processDefinitionId, Integer processInstanceDataId) {
        recordForReportService.remove(new QueryWrapper<RecordForReport>().eq("process_instance_data_id", processInstanceDataId));
        JSONObject jsonObject = null;
        List<RecordForReport> recordForReportList = new ArrayList<>();
        if (ObjectUtil.isNotEmpty(value1)) {
            jsonObject = JSONObject.parseObject(value1);
            List<ProcessFormTemplate> formTemplateList = processFormTemplateService.list(new QueryWrapper<ProcessFormTemplate>().eq("process_definition_id", processDefinitionId).eq("is_for_report", "是").and(qw -> qw.eq("flag", "基本类型").or().eq("flag", "字段变更类型")));
            for (ProcessFormTemplate template : formTemplateList) {
                String value = jsonObject.getString(template.getId() + "");
                if (ObjectUtil.isNotEmpty(value)) {
                    RecordForReport recordForReport = new RecordForReport();
                    recordForReport.setProcessInstanceDataId(processInstanceDataId);
                    recordForReport.setTemplateId(template.getId());
                    String name = null;
                    if (template.getFlag().equals("字段变更类型")) {
                        name = template.getLabel().split("\\.")[1];
                        recordForReport.setName(name);
                    } else
                        recordForReport.setName(template.getLabel());
                    recordForReport.setValue(value);
                    recordForReportList.add(recordForReport);
                }
            }
            if (CollUtil.isNotEmpty(recordForReportList))
                recordForReportService.saveBatch(recordForReportList);
        }
    }

    //20221113 生成“时间戳+随机数”结构的流程实例ID
    public String getTimeAndRandomNum() {
        LocalDateTime date = LocalDateTime.now();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
        String dateStr = date.format(fmt);
        Random rnd = new Random();
        // int code = rnd.nextInt(10000) + 1000;
        // String randomNum =Integer.toString(code);
        return dateStr;
    }

    public CheckTaskVO getCheckTaskVO(Integer processDefinitionId, String actProcessInstanceId) {
        SysUser user = (SysUser) httpSession.getAttribute("user");
        if (user == null) {
            throw new RuntimeException("用户未登录或登陆超时，请关闭本页面，重新从登陆入口进入");
        }
        CheckTaskVO checkTaskVO = new CheckTaskVO();
        //取出我的一个任务
        List<Task> taskList = workFlowBean.getMyTask(actProcessInstanceId);
        if (CollUtil.isEmpty(taskList)) {
            throw new RuntimeException("任务不存在或已被其他人处理，请尝试刷新待办列表");
        }
        Task actTask = taskList.get(0);
        //获取多条连线
        List<String> buttonNameList = workFlowBean.getButtonNameList(processDefinitionId, actTask.getTaskDefinitionKey());
        if (ObjectUtil.isNotEmpty(buttonNameList)) {
            checkTaskVO.setButtonNameList(buttonNameList);
        }
        //是否允许 意见，修改表单，下一步处理人
        ProcessDefinitionTask checkTask = processDefinitionTaskService.getOne(new QueryWrapper<ProcessDefinitionTask>().eq("process_definition_id", processDefinitionId).eq("task_def_key", actTask.getTaskDefinitionKey()));
        BeanUtils.copyProperties(checkTask, checkTaskVO);
        if (checkTask.getHaveComment().equals("是")) {
            if (checkTask.getTaskType().equals("bpmn:approvalTask")) {
                //审批任务
                checkTaskVO.setCommentTitle("意见/备注");
            } else {
                //处理任务
                checkTaskVO.setCommentTitle("操作记录");
            }
        }
        return checkTaskVO;
    }

    private void updateInfoNo(String dbValue, String pageValue, Integer asId) {
        if (StrUtil.isNotEmpty(dbValue)) {
            dbValue.replace("无,", "");//把默认的这个字符去掉，效果暂未验：不急
            dbValue.replace("无", "");
            String[] infoNoOldArr = dbValue.split(",");
            List<String> infoNoOldList = Stream.of(infoNoOldArr).collect(Collectors.toList());
            //父类是计算机的as_type
            List<Map<String, Object>> asTypesListMaps = asTypeService.listMaps(new QueryWrapper<AsType>().eq("pid", 4).select("id"));
            List<Integer> asTypeList = asTypesListMaps.stream().map(item -> Integer.valueOf(item.get("id").toString())).collect(Collectors.toList());
            List<Map<String, Object>> listMaps = asDeviceCommonService.listMaps(new QueryWrapper<AsDeviceCommon>().eq("state", "在用").in("type_id", asTypeList).ne("id", asId).select("port_no"));
            List<String> infoNoListForAsdeviceCommon = listMaps.stream().map(item -> item.get("port_no").toString()).collect(Collectors.toList());
            //在计算机变更记录中的“旧信息点号”（串）中查找出需要修改为“空闲”的信息点号
            List<String> infoNoOldListFileterd = infoNoOldList.stream().filter(item -> {
                boolean notUsedByOtherDevice = true;
                for (String infoNoForAsdeviceCommon : infoNoListForAsdeviceCommon) {
                    if (StrUtil.isNotEmpty(infoNoForAsdeviceCommon)) {
                        String[] infoNoArrForAsdeviceCommon = infoNoForAsdeviceCommon.split(",");
                        List<String> infoNoListForAsdeviceCommonForSplit = Stream.of(infoNoArrForAsdeviceCommon).collect(Collectors.toList());
                        for (String infoNoForAsdeviceCommonForSplit : infoNoListForAsdeviceCommonForSplit) {
                            if (infoNoForAsdeviceCommonForSplit.equals(item)) {//排除掉（虽然自己不用了）在别的计算机上还在使用的信息点号
                                // notUsedByOtherDevice = false;
                                return false;//20230525 未测，有时间待测
                            }
                        }
                    }
                }
                return true;
            }).collect(Collectors.toList());
            if (CollUtil.isNotEmpty(infoNoOldListFileterd)) {//对不再使用的信息点号的状态更改
                //  List<InfoNo> infoNoListUpdate = new ArrayList<InfoNo>();
                List<InfoNo> infoNoListUpdate = infoNoService.list(new QueryWrapper<InfoNo>().in("value", infoNoOldListFileterd));
                for (InfoNo infoNoUpdate : infoNoListUpdate) {
                    infoNoUpdate.setStatus("空闲");
                }
                if (CollUtil.isNotEmpty(infoNoListUpdate)) {
                    infoNoService.updateBatchById(infoNoListUpdate);
                }
            }
        }
        //对新申请使用的信息点号的（对应信息点表的）状态处理; 20230525 dbValue(旧信息点号)也可能有些也是需要用的&&同时存在于新的号中(pageValue)
        //但是有重复也没问：上面误把它标为空闲后，下面这里也能重置为“占用”
        //pageValue中有“无”和其他“杂乱的值”也不怕，这里是基于“信息点表”的记录值来过滤出要变更的信息点
        if (StrUtil.isNotEmpty(pageValue)) {
            String[] infoNoNewArr = pageValue.split(",");
            List<String> infoNoNewList = Stream.of(infoNoNewArr).collect(Collectors.toList());
            List<InfoNo> infoNoListUpdate = infoNoService.list(new QueryWrapper<InfoNo>().in("value", infoNoNewList));
            for (InfoNo infoNoUpdate : infoNoListUpdate) {
                infoNoUpdate.setStatus("占用");
            }
            if (CollUtil.isNotEmpty(infoNoListUpdate)) {
                infoNoService.updateBatchById(infoNoListUpdate);
            }


        }

    }


    private void setProVarListForChangeDept(ProcessDefinition processDefinition, ProcessInstanceData processInstanceData) {

          /*20220725 todo在这里添加部门变更的逻辑：在Start方法中，先判断流程名字是不是“变更”，然后判断是不是有部门名称，有的话设置两个流程变量#《currentDeptName，XXXX》,#《部门名称，changeDeptNmae
        这两个名称不相等的话，在此处读user表找出新部门的保密员ID,然后再找到“新部门保密员”（这个节点名要约定:新部门保密员）task定义结点，把这个用户信息给他加进去；新部门的部门领导由前一步的保密员选择，这里就不动了
        ; 排他网关上判断这两个是否相等
         */
        List<Task> activeTaskList = workFlowBean.getActiveTask(processInstanceData.getActProcessInstanceId());
        if(CollUtil.isEmpty(activeTaskList))//20231005 start节点后就结束了的场景：这里直接返回
            return ;
        Task activeTask = activeTaskList.get(0);
        Map<String, String> map = new HashMap<>();
        if (processDefinition.getProcessType().contains("变更") && processDefinition.getProcessName().contains("变更") && !processDefinition.getProcessType().contains("用户")) {//20220725对信息设备及用户变更流程的processType值约定；20240123 排除了“密钥上交流程”
            List<ProcessInstanceChange> processInstanceChangeList = processInstanceChangeService.list(new QueryWrapper<ProcessInstanceChange>().eq("process_instance_data_id", processInstanceData.getId()).eq("name", "责任部门").eq("is_report_title", "否"));
            if (CollUtil.isNotEmpty(processInstanceChangeList)) {
                ProcessInstanceChange processInstanceChange = processInstanceChangeList.get(0);
                String currentDeptName = processInstanceChange.getOldValue();
                String changeDeptName = processInstanceChange.getNewValue();
                int deptId = sysDeptService.getOne(new QueryWrapper<SysDept>().eq("name", changeDeptName)).getId();
                List<SysUser> userListForChangeDept = sysUserService.list(new QueryWrapper<SysUser>().eq("dept_id", deptId).eq("status","正常"));
                if (CollUtil.isEmpty(userListForChangeDept))
                    throw new RuntimeException("新部门没有用户！");
                List<Integer> userIdListForChangeDept = userListForChangeDept.stream().map(item -> item.getId()).collect(Collectors.toList());
                List<Integer> userIdListForBaomiUser = sysRoleUserService.list(new QueryWrapper<SysRoleUser>().eq("role_id", 7)).stream().map(item -> item.getUserId()).collect(Collectors.toList());
                List<Integer> baomiUserIdListForChangeDept = userIdListForChangeDept.stream().filter(item -> userIdListForBaomiUser.contains(item)).collect(Collectors.toList());// 也可以用取交集的方法：A.retainAll(B)
                if (CollUtil.isEmpty(baomiUserIdListForChangeDept))
                    throw new RuntimeException("新部门没有保密员，无法审核本流程！");
                //填充新部门保密员节点处理人
                int baomiUserIdForChangeDept = baomiUserIdListForChangeDept.get(0);//只取一个保密员
                ProcessDefinitionTask taskForChangeDeptForBaomiUser = processDefinitionTaskService.getOne(new QueryWrapper<ProcessDefinitionTask>().like("task_name", "新部门保密员").eq("process_definition_id", processDefinition.getId()));
                //更改这个Task记录里的处理人配置信息
                taskForChangeDeptForBaomiUser.setOperatorType("用户");
                taskForChangeDeptForBaomiUser.setOperatorTypeIds(baomiUserIdForChangeDept + "");
                taskForChangeDeptForBaomiUser.setOperatorTypeStr("新部门保密员");
                processDefinitionTaskService.updateById(taskForChangeDeptForBaomiUser);
                //填充新部门领导节点处理人20230227
                String leaderIdStrForChangeDept = sysRoleUserService.list(new QueryWrapper<SysRoleUser>().eq("role_id", 12).select("DISTINCT user_id")).stream().map(item -> item.getUserId().toString()).filter(item -> userIdListForChangeDept.contains(Integer.valueOf(item))).collect(Collectors.joining(","));
                // userIdListForChangeDept.stream().filter(item->item.getroleIdList().contains(12)).collect(Collectors.toList());
                if (ObjectUtil.isEmpty(leaderIdStrForChangeDept))
                    throw new RuntimeException("新部门没有部门领导，无法审核本流程！");
                ProcessDefinitionTask taskForChangeDeptForLeader = processDefinitionTaskService.getOne(new QueryWrapper<ProcessDefinitionTask>().like("task_name", "新部门领导").eq("process_definition_id", processDefinition.getId()));
                taskForChangeDeptForLeader.setOperatorType("用户");
                taskForChangeDeptForLeader.setOperatorTypeIds(leaderIdStrForChangeDept);
                taskForChangeDeptForLeader.setOperatorTypeStr("新部门领导");
                processDefinitionTaskService.updateById(taskForChangeDeptForLeader);
                map.put("currentDeptName", currentDeptName);
                map.put("changeDeptName", changeDeptName);
                workFlowBean.setProVarList(activeTask, map);
            } else {//20220829没变部门，就随便赋个值吧（反正也是在流程变量里比较相等与否）
                map.put("currentDeptName", "");
                map.put("changeDeptName", "");
                workFlowBean.setProVarList(activeTask, map);
            }
        }


    }

    /*
    处理变更字段及非变更字段中的
            20221211 参数多加了value1,区别通过DB查到的value1:前者是还没入DB的，所以是最新的
            ：下面的逻辑里还有用到DB的value2的，有时间也要换成从VO里取：但好像也没有必要：毕竟流程变量用的只是pageValue:暂不研
     */
    private void setProVarListForExGateway(ProcessInstanceData processInstanceData, String value1, Task actTask) {
        List<String> proVarList = workFlowBean.getProVarListForExGateway(processInstanceData.getProcessDefinitionId());
        Map<String, Object> mapForProVar = new HashMap<>();
        if (CollUtil.isEmpty(proVarList)) return;
        ProcessFormValue1 processFormValue1 = processFormValue1Service.getOne(new QueryWrapper<ProcessFormValue1>().eq("process_definition_id", processInstanceData.getProcessDefinitionId()).eq("act_process_instance_id", processInstanceData.getActProcessInstanceId()));
        List<ProcessFormValue2> formValue2List = processFormValue2Service.list(new QueryWrapper<ProcessFormValue2>().eq("form_value1_id", processFormValue1.getId()));
        // if (CollUtil.isEmpty(formValue2List)) return;//20220920 注释掉，新用户入网流程就没有关联资产
        List<AsConfig> asConfigList = asConfigService.list(new QueryWrapper<AsConfig>().select("distinct en_table_name,zh_table_name"));
        JSONObject jsonObject = JSONObject.parseObject(value1);
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
                    if (ObjectUtil.isEmpty(pageValue)) //这是变更字段存在值的分支
                        pageValue = "";//20231001 空值也写入流程变量值 | 减少页面报错可能
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

                    //}
                }

            }

        }
        if (MapUtil.isNotEmpty(mapForProVar))
            workFlowBean.setProVarList(actTask, mapForProVar);
    }

    private void saveProcessFormCustomInst(String jsonStr, Integer processInstanceId, List<ProcessFormValue2> processFormValue2List) {
        JSONObject jsonObject = JSONObject.parseObject(jsonStr);
        //20230721
        ProcessDefinition processDefinition = processDefinitionService.getById(this.getById(processInstanceId).getProcessDefinitionId());
        List<Map<String, Object>> listMaps = processFormTemplateService.listMaps(new QueryWrapper<ProcessFormTemplate>().eq("flag", "设备列表").eq("process_definition_id", processDefinition.getId()).select("flag"));
        List<String> listFlags = listMaps.stream().map(item -> (String) item.get("flag")).collect(Collectors.toList());
        List<ProcessFormCustomInst> processFormCustomInstList = new ArrayList<>();
        if (listFlags.contains("设备列表")) {
            JSONObject jsonObject1 = jsonObject.getJSONObject("repeaterForAssetList");
            if(ObjUtil.isEmpty(jsonObject1))
                return;
            JSONArray jsonAssetArray = jsonObject1.getJSONArray("dataSource");
            if (CollUtil.isEmpty(jsonAssetArray))
                return;
            else {
                List<AsDeviceCommon> asDeviceCommonList = jsonAssetArray.toJavaList(AsDeviceCommon.class);
                for (AsDeviceCommon a : asDeviceCommonList) {
                    List<ProcessFormCustomInst> tList = new ArrayList<>();
                    //每一个设备暂加6行记录/属性
                    for (Integer i = 0; i < 5; i++) {
                        ProcessFormCustomInst t = new ProcessFormCustomInst();
                        t.setAsId(a.getId());
                        t.setAssetTypeId(a.getTypeId());
                        tList.add(t);
                    }
                    tList.get(0).setColumnName("no");
                    tList.get(0).setColumnValue(a.getNo());
                    tList.get(1).setColumnName("miji");
                    tList.get(1).setColumnValue(a.getMiji());
                    tList.get(2).setColumnName("net_type");
                    tList.get(2).setColumnValue(a.getNetType());
                    tList.get(3).setColumnName("user_name");
                    tList.get(3).setColumnValue(a.getUserName());
                    tList.get(4).setColumnName("user_dept");
                    tList.get(4).setColumnValue(a.getUserDept());
                    processFormCustomInstList.addAll(tList);
                }
                for (ProcessFormCustomInst p : processFormCustomInstList) {
                    p.setTableName("as_device_common");
                    p.setColumnType("设备列表字段");
                    p.setProcessInstanceDataId(processInstanceId);
                }
//                jsonAssetArray.stream().forEach(item -> {
//                    JSONObject itemJson = (JSONObject) item;//注意：关于强转“直接用后半句(JSONObject)item.getString”是不行的，可能因为那个括号最后执行吧，也不是：todo记录
//                    System.out.println(item);
//                    //map.put(itemJson.getString("customTableId"), itemJson.getString("asId"));
//                });

            }

        } else {
            //List<ProcessFormTemplate> templateList
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
                    if (StrUtil.isEmpty(assetId))//20220908排除有自定义表没有选资产的情况：如外设变更流程中，上传机未变更&未选资产
                        continue;
                    processFormCustomInst.setAsId(Integer.parseInt(assetId));
                    Integer type_id = asDeviceCommonService.getById(assetId).getTypeId();
                    Integer Level2AsTypeId = asTypeService.getLevel2AsTypeById(type_id).getId();//20220510换成asid todo断点
                    processFormCustomInst.setAssetTypeId(Level2AsTypeId);
                    processFormCustomInstList.add(processFormCustomInst);
                }
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
        } else if (type.equals("设备名称的流程定义名称")) {
            //设备名称
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

        LocalDateTime localDateTime = LocalDateTime.now();

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
        httpSession.removeAttribute("assiginTaskAndUserVO");

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
        //assiginTaskAndUserVO放入session
        if (ObjUtil.isNotEmpty(endAndStartProcessVO.getAssiginTask())) {
            AssiginTaskAndUserVO assiginTaskAndUserVO = new AssiginTaskAndUserVO(endAndStartProcessVO.getOperatorType(), endAndStartProcessVO.getOperatorTypeIds(), endAndStartProcessVO.getHaveNextUser(),null,null);
            httpSession.setAttribute("assiginTaskAndUserVO", assiginTaskAndUserVO);
        }
        //20230516  processInstanceData的写入DB前置到这里，
        //20220710以下前置，为了setProVarListForExGateway
        //插入流程实例数据
        ProcessInstanceData processInstanceData = new ProcessInstanceData();
        processInstanceData.setProcessDefinitionId(processDefinition.getId());
        //20211203根据流程实例命名规则及表单提交者确定名称
        processInstanceData.setProcessName(getProcessName(endAndStartProcessVO, processDefinition));
        processInstanceData.setBusinessId(processFormValue1.getId());
        processInstanceData.setActProcessInstanceId(actProcessInstance.getId());
        //默认授权发起流程后
//        Map<String, String> stepMap = workFlowBean.getCurrentStep(processDefinition.getId(), 0, actProcessInstance.getId(), myTask.getTaskDefinitionKey());
//        //20221115

        processInstanceData.setPreProcessInstanceId(preProcessInstanceData.getId());
        //20221113 增加工单编号及赋值机制
        processInstanceData.setOrderNum(getTimeAndRandomNum());
        SysUser user = (SysUser) httpSession.getAttribute("user");
        SysDept dept = sysDeptService.getById(user.getDeptId());
        processInstanceData.setDisplayName(user.getDisplayName());
        processInstanceData.setLoginName(user.getLoginName());
        processInstanceData.setDeptName(dept.getName());
        processInstanceData.setStartDatetime(LocalDateTime.now());
        processInstanceData.setLastCommitDatetime(localDateTime);//20241015
        processInstanceDataService.save(processInstanceData);//保存成功后mybatis会把主键/ID传回参数processInstanceData中

        //20240829
        httpSession.removeAttribute("currentStepHandler");
        Map<String, List> taskAndHandlerMap = new HashMap<>();//<taskName,userList>
        Map<Integer, Map> prcIDAndHandlerMap = new HashMap<>();//<1000884,<taskName,userList>>
        httpSession.setAttribute("currentStepHandler", prcIDAndHandlerMap);//20240823 current在本项目中等于“下一步”
        prcIDAndHandlerMap.put(processInstanceData.getId(),taskAndHandlerMap);



        //第二个节点（也第一个ActTask，“发起”节点）创建；20220608 startEvent执行完后activeTask，一般只会有一activeTask(即“发起者”那个节点)&&流程的发起者一般也同样是“发起者”那个处理候选人：直接查activeTask即可：不过暂不改
        List<Task> myTaskList = workFlowBean.getMyTask(actProcessInstance.getId());
        Task myTask = myTaskList.get(0);
        setProVarListForExGateway(processInstanceData, endAndStartProcessVO.getValue1().getValue(), myTask);//20220710加
        if (ObjectUtil.isNotEmpty(endAndStartProcessVO.getButtonName())) {
            workFlowBean.completeTaskByParam(processDefinition.getId(), myTask, endAndStartProcessVO.getButtonName(), null);//20220628占个位
        } else {
            workFlowBean.completeTask(processDefinition.getId(), myTask);
        }
        //跳转到ActEventListener,设置下一个节点的处理人（如果下一个节点是当前节点<发起人>指定的，那会用到session/"assiginTaskAndUserVO"）
        //20211216注意以上代码总共会引发创建两个activiTask:发起人task及下一步的task:两次进入监听器

        //20230531加是否结束判断
        if (workFlowBean.isFinish(processInstanceData.getActProcessInstanceId())) {//2020621finish改成isFinish
            processInstanceData.setEndDatetime(LocalDateTime.now());
            processInstanceData.setProcessStatus("完成");
            processInstanceData.setDisplayCurrentStep("完成");//20220726由“”改成“完成”
            processInstanceData.setLoginCurrentStep("完成");
        } else {
            //断点20211211 instanceDataid为0 到底有何意义，20230517现已将真实实例id放入
            Map<String, String> stepMap = workFlowBean.getCurrentStep(processDefinition.getId(), processInstanceData.getId(), actProcessInstance.getId(), myTask.getTaskDefinitionKey());
            if (stepMap.get("taskType").contains("handle")) {
                processInstanceData.setProcessStatus("处理中");
            } else
                processInstanceData.setProcessStatus("审批中");
            processInstanceData.setDisplayCurrentStep(stepMap.get("displayName"));
            processInstanceData.setLoginCurrentStep(stepMap.get("loginName"));
        }
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
        //20240808 把assginTask相关信息写入task
        //逻辑：判断session|assiginTaskAndUserVO中的assiginTask有值后，再读取VO里的责任人信息，写入node相应字段
        AssiginTaskAndUserVO assiginTaskAndUserVO = (AssiginTaskAndUserVO) httpSession.getAttribute("assiginTaskAndUserVO");
        if(ObjectUtil.isNotEmpty(assiginTaskAndUserVO) && ObjectUtil.isNotEmpty(assiginTaskAndUserVO.getAssiginTask())){
            String assiginTask = assiginTaskAndUserVO.getAssiginTask();
            if(!"下一节点".equals(assiginTask)){
                processInstanceNode.setAssigin(assiginTask + "|" +  assiginTaskAndUserVO.getOperatorTypeIds());//主管所领导审批|1701
            }
        }


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
//            processInstanceNode.setOperatorTypeIds(endAndStartProcessVO.getOperatorTypeIds());
//            processInstanceNode.setOperatorTypeStr(endAndStartProcessVO.getOperatorTypeStr());
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
        httpSession.removeAttribute("assiginTaskAndUserVO");
        return true;
    }
    /*20231106 后台发起|组装流程实例 todo
    * 逻辑：组装“标准的start()”中的startProcessVO(主要是value1 && value2)
    * 1>读template表组装value1(里的value成员|json字符串)：没必要调用已有的相关方法（它们不能用于现在的情形）
    * 自己新写实现方法：直接遍历templdate表组装成jsonObject格式todo：可以先组装成map，再由map转换成jsonObject:JSONObject.fromObject(user)
     *
    * */
    public synchronized boolean start1(Integer processDefinitionId) {
        List<ProcessFormTemplate> list = processFormTemplateService.list(new QueryWrapper<ProcessFormTemplate>().eq("process_definition_id", processDefinitionId));

        return true;
    }


    @Override
    @SneakyThrows
    @Transactional(rollbackFor = Exception.class)
    public synchronized boolean start( MultipartFile[] files,StartProcessVO startProcessVO,String fujianMiji) {

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
        //assiginTaskAndUserVO放入session
        if (ObjUtil.isNotEmpty(startProcessVO.getAssiginTask())) {
            AssiginTaskAndUserVO assiginTaskAndUserVO = new AssiginTaskAndUserVO(startProcessVO.getOperatorType(), startProcessVO.getOperatorTypeIds(), startProcessVO.getHaveNextUser(),startProcessVO.getAssiginTask(),startProcessVO.getAssiginUser());
            httpSession.setAttribute("assiginTaskAndUserVO", assiginTaskAndUserVO);
        }
        //20230516  processInstanceData的写入DB前置到这里，为了workFlowBean.completeTask要引用processInstanceData的部门信息（当下一部是“提交人部门领导时”）
        //插入流程实例数据
        ProcessInstanceData processInstanceData = new ProcessInstanceData();
        processInstanceData.setProcessDefinitionId(processDefinition.getId());
        //20211203根据流程实例命名规则及表单提交者确定名称
        processInstanceData.setProcessName(getProcessName(startProcessVO, processDefinition));
        processInstanceData.setBusinessId(processFormValue1.getId());
        processInstanceData.setActProcessInstanceId(actProcessInstance.getId());
        //20221113 增加工单编号及赋值机制
        processInstanceData.setOrderNum(getTimeAndRandomNum());
        SysUser user = (SysUser) httpSession.getAttribute("user");
        SysDept dept = sysDeptService.getById(user.getDeptId());
//        if(processFormValue1.getCommitterType().equals("代其他人申请")){
//            String[] comitterStrArr = processFormValue1.getCommitterStr().split("\\.");
//            processInstanceData.setDisplayName(comitterStrArr[1]);
//            processInstanceData.setLoginName(comitterStrArr[2]);
//            processInstanceData.setDeptName(comitterStrArr[3]);
//        } else {
        //20221209改：不管本人提交还是代其他人提交，表单的提交人都应是“实际提交人”
        processInstanceData.setDisplayName(user.getDisplayName());
        processInstanceData.setLoginName(user.getLoginName());
        processInstanceData.setDeptName(dept.getName());
        // }
        processInstanceData.setStartDatetime(LocalDateTime.now());
        processInstanceDataService.save(processInstanceData);//保存成功后mybatis会把主键/ID传回参数processInstanceData中
        //第二个ActTask创建；20220608 startEvent执行完后activeTask，一般只会有一activeTask(即“发起者”那个节点)&&流程的发起者一般也同样是“发起者”那个处理候选人：直接查activeTask即可：不过暂不改

        //20240829
        httpSession.removeAttribute("currentStepHandler");
        Map<String, List> taskAndHandlerMap = new HashMap<>();//<taskName,userList>
        Map<Integer, Map> prcIDAndHandlerMap = new HashMap<>();//<1000884,<taskName,userList>>
        httpSession.setAttribute("currentStepHandler", prcIDAndHandlerMap);//20240823 current在本项目中等于“下一步”
        prcIDAndHandlerMap.put(processInstanceData.getId(),taskAndHandlerMap);

        LocalDateTime localDateTime = LocalDateTime.now();

        List<Task> myTaskList = workFlowBean.getMyTask(actProcessInstance.getId());
        Task myTask = myTaskList.get(0);
        //20230517下面这条语句中的myTask必须是“当前task”,所以不能放在"complete"之后
        setProVarListForExGateway(processInstanceData, startProcessVO.getValue1().getValue(), myTask);//20220710加
        if (ObjectUtil.isNotEmpty(startProcessVO.getButtonName())) {
            workFlowBean.completeTaskByParam(processDefinition.getId(), myTask, startProcessVO.getButtonName(), null);//20220628占个位
        } else {
            workFlowBean.completeTask(processDefinition.getId(), myTask);
        }
        if (workFlowBean.isFinish(processInstanceData.getActProcessInstanceId())) {//2020621finish改成isFinish
            processInstanceData.setEndDatetime(LocalDateTime.now());
            processInstanceData.setProcessStatus("完成");
            processInstanceData.setDisplayCurrentStep("完成");//20220726由“”改成“完成”
            processInstanceData.setLoginCurrentStep("完成");
        } else {
            //跳转到ActEventListener,设置下一个节点的处理人（如果下一个节点是当前节点<发起人>指定的，那会用到session/"assiginTaskAndUserVO"）
            //20211216注意以上代码总共会引发创建两个activiTask:发起人task及下一步的task:两次进入监听器
            //20211211 instanceDataid为0 到底有何意义，！？？？20230516已改成真实instanceDataid（相应的实例写入DB代码前移）
            Map<String, String> stepMap = workFlowBean.getCurrentStep(processDefinition.getId(), processInstanceData.getId(), actProcessInstance.getId(), myTask.getTaskDefinitionKey());
            //20221212
            if (stepMap.get("taskType").contains("handle")) {
                processInstanceData.setProcessStatus("处理中");
            } else
                processInstanceData.setProcessStatus("审批中");
            processInstanceData.setDisplayCurrentStep(stepMap.get("displayName"));
            processInstanceData.setLoginCurrentStep(stepMap.get("loginName"));
            processInstanceData.setLastCommitDatetime(localDateTime);//20241015

        }

        //20240124 处理附件
        String attachmentIds = "";
        if(ObjectUtil.isNotEmpty(files)){
            for(MultipartFile file : files){
                //MultipartFile file = files[0];
                Attachment attachment = new Attachment();
                attachment.setCreateDatetime(LocalDateTime.now()) ;
                attachment.setMiji(fujianMiji);

               // String savedName = getTimeAndRandomNum();
                attachment.setRoute(processInstanceData.getOrderNum());

                String suffix = subStringByLastPeriod(file.getOriginalFilename(),"right");
                attachment.setType(suffix);
                String fileNameSaved = subStringByLastPeriod(file.getOriginalFilename(),"left")+ "(" + fujianMiji+ ")" + suffix;
                attachment.setName(fileNameSaved);
                attachment.setSourceId(processInstanceData.getId());
                String localPath = environment.getProperty("downloadRoot")+ "/" + processInstanceData.getOrderNum() + "/" ;

                if (!FileUtil.exist(localPath)) {
                    FileUtil.mkdir(localPath);
                    System.out.println("Directory created: " + localPath);
                } else {
                    System.out.println("Directory already exists.");
                }
                attachmentService.save(attachment);
                attachmentIds += (attachment.getId() + ",");
                file.transferTo(new File(environment.getProperty("downloadRoot")+ "/" + processInstanceData.getOrderNum() + "/" + fileNameSaved));
            }


        }
        if(ObjectUtil.isNotEmpty(attachmentIds)){//删除字符串最后一个','
            char lastC = attachmentIds.substring(attachmentIds.length() - 1).charAt(0);
            if(lastC ==',')
                attachmentIds = attachmentIds.substring(0, attachmentIds.length() - 1);
        }

        //20220710 save前置，这里加个update
        processInstanceData.setAttachmentIds(attachmentIds);//20240124

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
        //20240808 把assginTask相关信息写入task
        //逻辑：判断session|assiginTaskAndUserVO中的assiginTask有值后，再读取VO里的责任人信息，写入node相应字段
        AssiginTaskAndUserVO assiginTaskAndUserVO = (AssiginTaskAndUserVO) httpSession.getAttribute("assiginTaskAndUserVO");
        if(ObjectUtil.isNotEmpty(assiginTaskAndUserVO) && ObjectUtil.isNotEmpty(assiginTaskAndUserVO.getAssiginTask())){
            String assiginTask = assiginTaskAndUserVO.getAssiginTask();
            if(!"下一节点".equals(assiginTask)){
                processInstanceNode.setAssigin(assiginTask + "|" +  assiginTaskAndUserVO.getOperatorTypeIds());//主管所领导审批|1701
            }
        }

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
//            processInstanceNode.setOperatorTypeIds(startProcessVO.getOperatorTypeIds());
//            processInstanceNode.setOperatorTypeStr(startProcessVO.getOperatorTypeStr());
//        }
        processInstanceNodeService.save(processInstanceNode);
        //20220620 保存硬盘信息
        List<DiskForHisForProcess> diskListForHisForProcess = startProcessVO.getDiskListForHisForProcess();
        setDiskListChangeAndValue2( diskListForHisForProcess, processInstanceData, processDefinition, processFormValue1, user, dept);
//        List<ProcessFormValue2> processFormValue2ListForDisk = new ArrayList<>();
//        //目前没有“删除”,第一次提交只有（对DiskForHisForProcess表来说）新增，对AsDeviceCommon表来说有“新增/编辑”
//        if (CollUtil.isNotEmpty(diskListForHisForProcess)) {//20220701 diskListForHisForProcess初始的数据来源为后台service.list():查不到他也会返回空LIST对象（而不是null）
//            diskListForHisForProcess.forEach(item -> {
//                item.setProcessInstanceDataId(processInstanceData.getId());
//                //20230729 硬盘列表写入value2;实际上只在handle里写就可以了&&这里写没有必要&&starAndEnd我也不准确写
//                ProcessFormValue2 processFormValue2 = new ProcessFormValue2();
//                processFormValue2.setCustomTableId(39);//20230729硬盘列表专用（ID为39
//                processFormValue2.setProcessDefinitionId(processFormValue1.getProcessDefinitionId());
//                processFormValue2.setActProcessInstanceId(actProcessInstance.getId());
//                processFormValue2.setFormValue1Id(processFormValue1.getId());
//                processFormValue2.setAsId(item.getAsId());//start时因为新增的硬盘没有写入AsDeviceCommon，故这时asID没值（默认是0）; 在流程finish时才（真正写入db+
//                // ）有值
//                processFormValue2ListForDisk.add(processFormValue2);
//
//            });
//            //20230729先删除旧的value2/硬盘ID;约定“硬盘列表信息表”专用自定义表ID 39; 这个逻辑只在处理节点有用
//            //processFormValue2Service.remove(new QueryWrapper<ProcessFormValue2>().eq("act_process_instance_id",actProcessInstance.getId()).eq("custom_table_id",39));
//            processFormValue2Service.saveBatch(processFormValue2ListForDisk);
//            diskForHisForProcessService.saveBatch(diskListForHisForProcess);//20220614这个flag设置有点小问题：暂不改
//
//
//        }
        //20230223
        saveForReport(processFormValue1.getValue(), processInstanceData.getProcessDefinitionId(), processInstanceData.getId());
        //变更字段
        changeColumnForStart(processInstanceData, processFormValue1, processFormValue2List);
        //20220725 部门变更相关流程变量的设置
        setProVarListForChangeDept(processDefinition, processInstanceData);

        httpSession.removeAttribute("assiginTaskAndUserVO");
        return true;
    }

    @Override
    @Transactional
    public synchronized boolean handle(CheckProcessVO checkProcessVO) {
        ProcessInstanceData processInstanceData = processInstanceDataService.getById(checkProcessVO.getProcessInstanceDataId());
        String actProcessInstanceId = processInstanceData.getActProcessInstanceId();
        String processStatus = processInstanceData.getProcessStatus();
        //checkProcessVO放入session

        if (ObjUtil.isNotEmpty(checkProcessVO.getAssiginTask())) {
            AssiginTaskAndUserVO assiginTaskAndUserVO = new AssiginTaskAndUserVO(checkProcessVO.getOperatorType(), checkProcessVO.getOperatorTypeIds(), checkProcessVO.getHaveNextUser(),checkProcessVO.getAssiginTask(),checkProcessVO.getAssiginUser());
            httpSession.setAttribute("assiginTaskAndUserVO", assiginTaskAndUserVO);
        }


        //20240829
        httpSession.removeAttribute("currentStepHandler");
        Map<String, List> taskAndHandlerMap = new HashMap<>();//<taskName,userList>
        Map<Integer, Map> prcIDAndHandlerMap = new HashMap<>();//<1000884,<taskName,userList>>
        httpSession.setAttribute("currentStepHandler", prcIDAndHandlerMap);//20240823 current在本项目中等于“下一步”
        prcIDAndHandlerMap.put(processInstanceData.getId(),taskAndHandlerMap);

        LocalDateTime localDateTime = LocalDateTime.now();


        //取出我的一个任务
        List<Task> myTaskList = workFlowBean.getMyTask(actProcessInstanceId);
        //20230707测试获取历史任务：测他含不含当前activie task：含 todo断点
        //List<HistoricTaskInstance> list = workFlowBean.getHistoricTaskInstance1(actProcessInstanceId);

        if (CollUtil.isEmpty(myTaskList)) {
            throw new RuntimeException("任务不存在或已被其他人处理，请尝试刷新待办列表");
        }
        Task myTask = myTaskList.get(0);//20220711感觉当前用户如果有两个待办结点，那本程序执行逻辑就有问题了：todo后续有时间再细想

        //20220711 从task里找出流程定义信息-->找出(自定义的)task定义相关信息：取代checkVo从前端读的部分
        //20220709设置流程变量
        setProVarListForExGateway(processInstanceData, checkProcessVO.getValue1().getValue(), myTask);
        //20220725 部门变更相关流程变量的设置
        setProVarListForChangeDept(processDefinitionService.getById(processInstanceData.getProcessDefinitionId()), processInstanceData);
        //完成任务
        if (ObjectUtil.isNotEmpty(checkProcessVO.getButtonName()) || checkProcessVO.getHaveSelectProcess().equals("是")) {
            String selectedProcess = checkProcessVO.getSelectedProcess();
            if (checkProcessVO.getHaveSelectProcess().equals("是") && ObjectUtil.isEmpty(checkProcessVO.getSelectedProcess()))
                selectedProcess = "";//20220629 如果bpmnxml里流程变量值的判断条件可以是“！=null”,那这两个判断条件可以合并：todor后续试验验证
            else if (checkProcessVO.getHaveSelectProcess().equals("是") && ObjectUtil.isNotEmpty(checkProcessVO.getSelectedProcess())) {
                selectedProcess = checkProcessVO.getSelectedProcess();
                // processInstanceData.setPreProcessInstanceId();
            }
            //20220709注意这个completeTaskByParam仅包含buttonName/selectedProcess两种机制
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
                } else if (stepMap.get("taskType").contains("handle")) {
                    processInstanceData.setProcessStatus("处理中"); //2021117对于处理节点，要改成“处理中”，这个值有利于前端动态渲染“审批/处理按钮”名字时判断
                } else {
                    processInstanceData.setProcessStatus("审批中");
                }
            }
            processInstanceData.setLastCommitDatetime(localDateTime);//20241015
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

        //20240808 把assginTask相关信息写入task
        //逻辑：判断session|assiginTaskAndUserVO中的assiginTask有值后，再读取VO里的责任人信息，写入node相应字段
        AssiginTaskAndUserVO assiginTaskAndUserVO = (AssiginTaskAndUserVO) httpSession.getAttribute("assiginTaskAndUserVO");
        if(ObjectUtil.isNotEmpty(assiginTaskAndUserVO) && ObjectUtil.isNotEmpty(assiginTaskAndUserVO.getAssiginTask())){
            String assiginTask = assiginTaskAndUserVO.getAssiginTask();
            if(!"下一节点".equals(assiginTask)){
                processInstanceNode.setAssigin(assiginTask + "|" +  assiginTaskAndUserVO.getOperatorTypeIds());//主管所领导审批|1701
            }
        }


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
//            processInstanceNode.setOperatorTypeIds(checkProcessVO.getOperatorTypeIds());
//            processInstanceNode.setOperatorTypeStr(checkProcessVO.getOperatorTypeStr());
//        }
        if (ObjectUtil.isNotEmpty(checkProcessVO.getComment()) && checkProcessVO.getHaveComment().equals("是")) {
            processInstanceNode.setComment(checkProcessVO.getComment());
        } else {
            if (processInstanceNode.getButtonName().contains("同意")) {//20211117修改退回时，意见也是同意的问题
                processInstanceNode.setComment(processInstanceNode.getButtonName());//20221104 把“同意”改为processInstanceNode.getButtonName()：为了兼容“不同意”
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
        //20221213
        saveForReport(checkProcessVO.getValue1().getValue(), processInstanceData.getProcessDefinitionId(), processInstanceData.getId());
        //变更字段 20220701下移到这里，因为这时 processFormValue1已经写到DB中了，下面的方法是根据processInstanceDataID找的value1表查的：20221213其实应该直接使用"环境中现有的"value1:暂不改
        changeColumnForHandle(processInstanceData, workFlowBean.isFinish(processInstanceData.getActProcessInstanceId()));
        //20220621 保存硬盘信息
        //先删除DiskForHisForProcess表原记录
        diskForHisForProcessService.remove(new QueryWrapper<DiskForHisForProcess>().eq("process_instance_data_id", processInstanceData.getId()));
        List<DiskForHisForProcess> diskListForHis = checkProcessVO.getDiskListForHisForProcess();
        ProcessDefinition processDefinition = processDefinitionService.getById(processFormValue1.getProcessDefinitionId());
        setDiskListChangeAndValue2(diskListForHis, processInstanceData,processDefinition, processFormValue1, user, dept);

        //20220510
        this.saveProcessFormCustomInst(checkProcessVO.getValue1().getValue(), processInstanceData.getId(), processFormValue2List);
//        //20220725 部门变更相关流程变量的设置 20220829 挪在complete前
//        setProVarListForChangeDept(processDefinitionService.getById(processInstanceData.getProcessDefinitionId()), processInstanceData);
        httpSession.removeAttribute("assiginTaskAndUserVO");
        return true;
    }

    @Override
    public boolean modifyProcessForm(ModifyProcessFormVO modifyProcessFormVO) {//20230820添加了对value2/变更记录的维护；自定义表实例快照机制没验证（但问题不大）
        Integer processFormValue1Id = modifyProcessFormVO.getProcessFormValue1Id();
        //
        System.out.println(modifyProcessFormVO);
        ProcessFormValue1 processFormValue1 = processFormValue1Service.getById(processFormValue1Id);
        processFormValue1.setValue(modifyProcessFormVO.getValue());
        String actProcessInstanceId = processFormValue1.getActProcessInstanceId();
        List<ProcessInstanceData> list = processInstanceDataService.list(new QueryWrapper<ProcessInstanceData>().eq("act_process_instance_id",actProcessInstanceId));
        ProcessInstanceData processInstanceData = list.get(0);
        SysUser user = (SysUser) httpSession.getAttribute("user");
        SysDept dept = sysDeptService.getById(user.getDeptId());
        List<ProcessFormValue2> processFormValue2List = modifyProcessFormVO.getValue2List();
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
        //20221213
        saveForReport(modifyProcessFormVO.getValue(), processInstanceData.getProcessDefinitionId(), processInstanceData.getId());
        //变更字段 20220701下移到这里，因为这时 processFormValue1已经写到DB中了，下面的方法是根据processInstanceDataID找的value1表查的：20221213其实应该直接使用"环境中现有的"value1:暂不改
        changeColumnForHandle(processInstanceData, workFlowBean.isFinish(processInstanceData.getActProcessInstanceId()));
        //20220621 保存硬盘信息
        //先删除DiskForHisForProcess表原记录
        diskForHisForProcessService.remove(new QueryWrapper<DiskForHisForProcess>().eq("process_instance_data_id", processInstanceData.getId()));
        List<DiskForHisForProcess> diskListForHis = modifyProcessFormVO.getDiskListForHisForProcess();
        ProcessDefinition processDefinition = processDefinitionService.getById(processFormValue1.getProcessDefinitionId());
        setDiskListChangeAndValue2(diskListForHis, processInstanceData,processDefinition, processFormValue1, user, dept);
        this.saveProcessFormCustomInst(modifyProcessFormVO.getValue(), processInstanceData.getId(), processFormValue2List);
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
        //删除自定义表快照20220920
        processFormCustomInstService.remove(new QueryWrapper<ProcessFormCustomInst>().eq("process_instance_data_id", processInstanceData.getId()));
        //删除报表字段记录表20221213
        recordForReportService.remove(new QueryWrapper<RecordForReport>().eq("process_instance_data_id", processInstanceData.getId()));
        //删除流程实例
        ProcessDefinition proDef = processDefinitionService.getById(processInstanceData.getProcessDefinitionId());
        if (ObjectUtil.isNotEmpty(proDef.getDeployId()))//20221105加此判断：如果是流程定义删除后的流程实例：activi里没有相应实例信息了 && 对应的流程定义的deployID为空串
            workFlowBean.deleteProcessInstance(processInstanceData.getActProcessInstanceId());
        //删除diskForHisForProcess
        diskForHisForProcessService.remove(new QueryWrapper<DiskForHisForProcess>().eq("process_instance_data_id", processInstanceData.getId()));
        //20220817todo增加逻辑：判断要删除的流程实例对尖的流程定义是不是已经过时的（have_display ===否）：如果过时，就将该流程定义对应的定义表definition/task/edge以及activiti相关的流程实例及定义删除
        if (ObjectUtil.isNotEmpty(proDef)) {
            if (proDef.getHaveDisplay().equals("否")) {
                if (CollUtil.isEmpty(processInstanceDataService.list(new QueryWrapper<ProcessInstanceData>().eq("process_definition_id", proDef.getId()))))//当前如果还有实例，但不能删除定义
                 /* 20221105 增加这个判断：而deployid为空时的删除实例是由删除定义发起：需要被排除;
                    20230708注：这种情况应该是删除流程定义时的操作逻辑导致（对于走过流程实例的定义在删除时会清空deployID字段）*/
                    if (ObjectUtil.isNotEmpty(proDef.getDeployId()))
                        processDefinitionService.delete(proDef.getId());
            }
        }
        //20240124 删除附件表
        if(ObjectUtil.isNotEmpty(processInstanceData.getAttachmentIds())){
            String[] ids_str = processInstanceData.getAttachmentIds().split("\\,");
            List<String> ids = Arrays.asList(ids_str);
            ids.forEach(i->{
                Attachment attachment = attachmentService.getById(Integer.valueOf(i));
                String localPath = environment.getProperty("downloadRoot")+ "/" + attachment.getRoute() + "/" + attachment.getName();
                if (FileUtil.exist(localPath)) {
                    // 删除目录及其所有内容
                    FileUtil.del(localPath);
                    System.out.println("Directory and all its contents deleted successfully.");
                }
            });

            attachmentService.removeByIds(Arrays.asList(ids_str));
        }


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
        //20230720 如果想支持“一个基本类型对应多个设备”，那么asIdMap/value类型就要换成List<Integer>,然后在下面“ Integer asId = asIdMap.get(entry.getKey());”这句改成从上述那个list中遍历，并且把下面的代码放在这个遍历体中即可：已
        // Map<Integer, Integer> asIdMap = formValue2List.stream().collect(Collectors.toMap(ProcessFormValue2::getCustomTableId, ProcessFormValue2::getAsId));
        Map<Integer, List<Integer>> customIdAsIdListMap = new HashMap<>();
        formValue2List.stream().forEach(item -> {
            if (CollUtil.isNotEmpty(customIdAsIdListMap.get(item.getCustomTableId()))) {
                customIdAsIdListMap.get(item.getCustomTableId()).add(item.getAsId());
            } else {
                List<Integer> list = new ArrayList<>();
                list.add(item.getAsId());
                customIdAsIdListMap.put(item.getCustomTableId(), list);
            }
        });
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
            List<ProcessInstanceChange> changeList = Lists.newArrayList();
            //Integer asId = asIdMap.get(entry.getKey());
            //20230721将上面这句改成从List中遍历
            List<Integer> listAsId = customIdAsIdListMap.get(entry.getKey());
            listAsId.forEach(asId -> {

                //组装map2（<基本表名，变更字段List>）
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
                    for (ProcessFormTemplate processFormTemplate : entry2.getValue()) {
                        String columnNameTmp = (processFormTemplate.getName().split(",")[0]).split("\\.")[3];
                        String columnName = StrUtil.toCamelCase(columnNameTmp);
                        //根据变更字段ID取出processFormValue1中对应的值
                        Integer id = processFormTemplate.getId();
                        String pageValue;
                        if (processFormTemplate.getType().equals("日期")) {
                            pageValue = jsonObject.getString(id + "Date");
                        } else if (processFormTemplate.getType().equals("日期时间")) {
                            pageValue = jsonObject.getString(id + "Datetime");
                        } else {
                            pageValue = jsonObject.getString(id + "");
                        }
                        //20220820为了记录“审批报表专用字段”的变更字段
                        ProcessInstanceChange changeSpecial = new ProcessInstanceChange();
                        //if (processFormTemplate.getLabel().split("\\.")[1].equals("责任人") || processFormTemplate.getLabel().split("\\.")[1].equals("责任部门")|| processFormTemplate.getLabel().split("\\.")[1].equals("责任人密级"))
                        String changeColumnName = processFormTemplate.getLabel().split("\\.")[1];
                        if (changeColumnName.equals("重装操作系统") || changeColumnName.equals("更换硬盘") || changeColumnName.equals("更换内存") || changeColumnName.equals("更换网卡")) {
                            changeSpecial.setAsId(asId);
                            changeSpecial.setProcessInstanceDataId(processInstanceData.getId());
                            changeSpecial.setActProcessInstanceId(processInstanceData.getActProcessInstanceId());
                            changeSpecial.setName(changeColumnName);
                            changeSpecial.setNewValue(pageValue);
                            changeSpecial.setIsFinish("否");
                            changeSpecial.setIsReportTitle("是");
                            changeSpecial.setZhTableName(asConfigMap.get(tableNameTmp));
                            changeList.add(changeSpecial);
                            //以下两个只是个“开关量”：不记入“普通”变更记录；20221208 下面这个判断条件有点多余，todo考虑去掉
                            if (changeColumnName.equals("重装操作系统") || changeColumnName.equals("更换硬盘") || changeColumnName.equals("更换内存") || changeColumnName.equals("更换网卡"))
                                continue;
                        }
                        if (ObjectUtil.isNotEmpty(pageValue)) {
                           //20231114 更新“安全产品状态”|写入基本表DB
                            if(changeColumnName.equals("是否安装安全产品") && "否".equals(pageValue)){
                                AsComputerSpecial asComputerSpecial = asComputerSpecialService.getOne(new QueryWrapper<AsComputerSpecial>().eq("as_id",asId));
                                asComputerSpecial.setSafeInstall("否");
                                asComputerSpecialService.updateById(asComputerSpecial);
                            }

                            if ("置空".equals(pageValue)) {//20230524新增“置空”机制：在硬盘报废流程中，利用“信息点号”字段(用于前端临时借用放宿主机资产号的)传递（但此时DB里本身并没有值）
                                pageValue = "";
                                /*此时为“硬盘报废”场景，清空
                                 * 这个清空会引发变更记录相应记录；当然也会让dbObject更新这个值（因为DB中“信息点号”字段本身就为空串，无意义但也不用改）
                                 */
                                if (processDefinition.getProcessName().contains("硬盘报废")) {
                                    //这句其实应放在isFinsh==true判断之后，但为了代码简洁，就不动了
                                    if (changeColumnName.equals("信息点号")) {
                                        changeColumnName = "宿主机";//原来是“信息点号”
                                        ReflectUtil.setFieldValue(dbObject, "hostAsId", 0);//这个字段不是自定表中的字段/也不会在变更记录字段
                                    }
                                }
                            }
                            Object dbValueObj = ReflectUtil.getFieldValue(dbObject, columnName);
                            String dbValue = null;
                            if (ObjectUtil.isEmpty(dbValueObj)) {
                                dbValue = "";
                            } else {
                                dbValue = dbValueObj.toString();
                            }
                            if (!pageValue.equals(dbValue)) {
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
                                change.setName(changeColumnName);
                                change.setOldValue(dbValue);
                                change.setNewValue(pageValue);
                                //
                                SysUser user = (SysUser) httpSession.getAttribute("user");
                                SysDept dept = sysDeptService.getById(user.getDeptId());
                                change.setDeptName(dept.getName());
                                change.setDisplayName(user.getDisplayName());
                                change.setLoginName(user.getLoginName());
                                change.setModifyDatetime(LocalDateTime.now());
                                change.setIsFinish("否");
                                change.setZhTableName(asConfigMap.get(tableNameTmp));
                                changeList.add(change);
                            }
                        }
                    }
                }
            });

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
        //Map<Integer, Integer> asIdMap = formValue2List.stream().collect(Collectors.toMap(ProcessFormValue2::getCustomTableId, ProcessFormValue2::getAsId));
        Map<Integer, List<Integer>> customIdAsIdListMap = new HashMap<>();
        formValue2List.stream().forEach(item -> {
            if (CollUtil.isNotEmpty(customIdAsIdListMap.get(item.getCustomTableId()))) {
                customIdAsIdListMap.get(item.getCustomTableId()).add(item.getAsId());
            } else {
                List<Integer> list = new ArrayList<>();
                list.add(item.getAsId());
                customIdAsIdListMap.put(item.getCustomTableId(), list);
            }
        });
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
            List<ProcessInstanceChange> changeList = Lists.newArrayList();
            //Integer asId = asIdMap.get(entry.getKey());
            //20230721将上面这句改成从List中遍历
            List<Integer> listAsId = customIdAsIdListMap.get(entry.getKey());
            listAsId.forEach(asId -> {
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
                //List<ProcessInstanceChange> changeList = Lists.newArrayList();
                for (Map.Entry<String, List<ProcessFormTemplate>> entry2 : map2.entrySet()) {
                    //as_device_common
                    String tableNameTmp = entry2.getKey();
                    String tableName = StrUtil.toCamelCase(tableNameTmp);
                    //取出数据对象
                    IService service = (IService) SpringUtil.getBean(tableName + "ServiceImpl");
                    Object dbObject = null;
                    Integer asTypeId = 0;//20230609加：用于计算机（asDeviceCommon表）变更时的相关硬盘信息同步变更
                    if (tableNameTmp.equals("as_device_common")) {
                        dbObject = service.getById(asId);
                        AsDeviceCommon asDeviceCommon = (AsDeviceCommon) dbObject;
                        asTypeId = asDeviceCommon.getTypeId();

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
                            Object thing = jsonObject.get(id + "");
                            if (thing instanceof JSONArray) {//20230222 对json数组做了处理：转成字符串
                                pageValue = StringUtils.join(jsonObject.getJSONArray(id + ""), ",");
                            } else
                                pageValue = jsonObject.getString(id + "");
                        }
                        //20220820为了记录“审批报表专用字段”的变更字段
                        ProcessInstanceChange changeSpecial = new ProcessInstanceChange();
                        String changeColumnName = processFormTemplate.getLabel().split("\\.")[1];
                        if (changeColumnName.equals("重装操作系统") || changeColumnName.equals("更换硬盘") || changeColumnName.equals("更换内存") || changeColumnName.equals("更换网卡")) {
                            if (ObjectUtil.isNotEmpty(pageValue)) {//20220829 去空
                                changeSpecial.setAsId(asId);
                                changeSpecial.setProcessInstanceDataId(processInstanceData.getId());
                                changeSpecial.setActProcessInstanceId(processInstanceData.getActProcessInstanceId());
                                changeSpecial.setName(changeColumnName);
                                changeSpecial.setNewValue(pageValue);
                                changeSpecial.setIsFinish("否");
                                changeSpecial.setIsReportTitle("是");//20320609 有点记不清本字段具体用法：暂不研
                                changeSpecial.setZhTableName(asConfigMap.get(tableNameTmp));
                                changeList.add(changeSpecial);
                                //以下只是个“开关量”：
                                if (changeColumnName.equals("重装操作系统") || changeColumnName.equals("更换硬盘") || changeColumnName.equals("更换内存") || changeColumnName.equals("更换网卡"))
                                    continue;
                            }
                        }
                        if (ObjectUtil.isNotEmpty(pageValue)) {//这是变更字段存在值的分支
                            //20230527 asDeviceCommon创建前置至此
                            AsDeviceCommon asDeviceCommon = asDeviceCommonService.getById(asId);//20230215 实际上这个asDeviceCommon没必要设置：直接用 ReflectUtil.getFieldValue(dbObject, columnName)取相应值即可：暂不改
                            if ("置空".equals(pageValue)) {//20230524新增“置空”机制，
                                pageValue = "";
                                /*此时为“硬盘报废”场景，清空
                                 * 这个清空会引发变更记录相应记录；当然也会让dbObject更新这个值（因为DB本身就为空串，无意义但也不用改）
                                 * 前端是借用“信息点号/protNO”这个字段传值（但此时DB里本身并没有值）*/
                                if (processDefinition.getProcessName().contains("硬盘报废")) {
                                    //这句其实应放在isFinsh==true判断之后，但为了代码简洁，就不动了
                                    if (changeColumnName.equals("信息点号")) {
                                        changeColumnName = "宿主机";//改名
                                        ReflectUtil.setFieldValue(dbObject, "portNo", "");//借用的这个字段DB值清空
                                        if (asDeviceCommon.getHostAsId() != 0) {
                                            AsDeviceCommon asDeviceCommonForHost = asDeviceCommonService.getById(asDeviceCommon.getHostAsId());
                                            if (ObjectUtil.isNotEmpty(asDeviceCommonForHost))
                                                //备注里标注上摘除前宿主机
                                                ReflectUtil.setFieldValue(dbObject, "remark", "摘除前的宿主机：" + asDeviceCommonForHost.getNo());
                                        }
                                        ReflectUtil.setFieldValue(dbObject, "hostAsId", 0);//这个字段不是自定表中的字段/也不会在变更记录字段

                                    }
                                }
                            }
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
                                //20220716 设置生命周期状态;

                                List<Integer> typeIdList = asTypeService.getTypeIdList(processDefinition.getAsTypeId());//获取流程定义里的“主设备”类型(及其子类)List
                                if (typeIdList.contains(asDeviceCommon.getTypeId()))//如果当前资产是“主类型”，生命周期类型为流程定义中的processType,否则为processType2
                                    change.setLifteCycle(processDefinition.getProcessType());
                                else
                                    change.setLifteCycle(processDefinition.getProcessType2());

                                change.setAsId(asId);
                                change.setProcessInstanceDataId(processInstanceData.getId());
                                change.setActProcessInstanceId(processInstanceData.getActProcessInstanceId());
                                change.setName(changeColumnName);
                                change.setOldValue(dbValue);
                                change.setNewValue(pageValue);
                                //
                                SysUser user = (SysUser) httpSession.getAttribute("user");
                                SysDept dept = sysDeptService.getById(user.getDeptId());
                                change.setDeptName(dept.getName());//20230608 变更人应该是填写变更值的那步的处理人：有时间再研&改
                                change.setDisplayName(user.getDisplayName());
                                change.setLoginName(user.getLoginName());
                                change.setModifyDatetime(LocalDateTime.now());
                                if (isFinish)
                                    change.setIsFinish("是");
                                else
                                    change.setIsFinish("否");
                                change.setZhTableName(asConfigMap.get(tableNameTmp));
                                changeList.add(change);
                                //数据对象
                                if (isFinish) {
                                    ReflectUtil.setFieldValue(dbObject, columnName, pageValue);
                                    //20220625注意留意：这些清空字段不能与正常字段重名
                                    if (changeColumnName.contains("计算机策略") || changeColumnName.contains("故障描述") || changeColumnName.contains("新接入外设") || changeColumnName.contains("硬盘变更"))
                                        ReflectUtil.setFieldValue(dbObject, columnName, "");
                                    if (changeColumnName.contains("操作系统安装时间")) {//20220915
                                        asDeviceCommonService.addStatistics();
                                    }
                                    //20221005信息点号单独处理
                                    //20230525排除硬盘变更（“宿主机”字段借用"信息点号"）
                                    if (asTypeId != 30) {
                                        if (changeColumnName.contains("信息点号")) {
                                            updateInfoNo(dbValue, pageValue, asId);
                                        }
                                    }
                                    if (changeColumnName.equals("涉密级别")) {//202302115 监听密级字段变更：重新生成保密编号
                                        String newBaomiLab = asDeviceCommonService.makeBaomiNo(asTypeService.getById(asDeviceCommon.getTypeId()), pageValue, asDeviceCommon.getUseDate());
                                        ReflectUtil.setFieldValue(dbObject, "baomiNo", newBaomiLab);
                                    }
                                    //20230608对硬盘相关字段的更新：待验证正确性
                                    if (asTypeId != 0) {//（机制见上方相应逻辑）为0代表本变更字段并不是asDeviceCommon主表的
                                        AsType asTypeLevel2 = asTypeService.getLevel2AsTypeById(asTypeId);
                                        if (asTypeLevel2.getId() == 29 || asTypeLevel2.getId() == 4) {
                                            //20240131 归库流程排除硬盘与主表（计算机）的关联字段变更
                                            if (!processInstanceData.getProcessName().contains("归库")  && (changeColumnName.equals("责任人") || changeColumnName.equals("责任人密级") || changeColumnName.equals("责任部门") || changeColumnName.equals("联网类别") || changeColumnName.equals("涉密级别") || changeColumnName.equals("状态"))) {//20230609 责任人
                                                //添加硬盘相关变更记录
                                                List<AsDeviceCommon> diskList = asDeviceCommonService.list(new QueryWrapper<AsDeviceCommon>().eq("host_as_id", asDeviceCommon.getId()).ne("state", "报废").ne("state", "摘除"));
                                                if (CollUtil.isNotEmpty(diskList)) {
                                                    for (AsDeviceCommon item : diskList) {
                                                        ProcessInstanceChange processInstanceChange = new ProcessInstanceChange();
                                                        BeanUtils.copyProperties(change, processInstanceChange);
                                                        //item.setTemp("");
                                                        if (changeColumnName.equals("涉密级别"))
                                                            item.setMiji(pageValue);
                                                        if (changeColumnName.equals("责任人"))
                                                            item.setUserName(pageValue);
                                                        if (changeColumnName.equals("责任人密级"))
                                                            item.setUserMiji(pageValue);
                                                        if (changeColumnName.equals("责任部门"))
                                                            item.setUserDept(pageValue);
                                                        //只有这两种状态同步
                                                        if (changeColumnName.equals("状态") && ((pageValue.equals("在用") || pageValue.equals("停用"))))
                                                            item.setState(pageValue);
                                                        if (changeColumnName.equals("联网类别"))
                                                            item.setNetType(pageValue);
                                                        processInstanceChange.setAsId(item.getId());
                                                        changeList.add(processInstanceChange);
                                                    }
                                                    //20230809 todo添加对硬盘编辑时的信息维护：如硬盘的新增与摘除，还有硬盘其他信息维护：可能需要对比那个“硬盘状态表diskForHIS”:但好像已经在别处实现了，只是有点问题：如diskForHIS表记录里：如果两块硬盘同时摘除，只把第二个记录成了摘除：todo研
                                                    //20230610 同步硬盘与计算机同值的相关DB字段
                                                    asDeviceCommonService.updateBatchById(diskList);
                                                }


                                            }

                                        }
                                    }

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


            });
            if (ObjectUtil.isNotEmpty(changeList)) {
                //20230527 添加对老变更记录的删除（即对“isFinsh状态为‘否’”的记录的删除）：暂不验证是不是有问题
                processInstanceChangeService.removeById(processInstanceData.getActProcessInstanceId());
                processInstanceChangeService.saveBatch(changeList);

            }
        }
    }
}



