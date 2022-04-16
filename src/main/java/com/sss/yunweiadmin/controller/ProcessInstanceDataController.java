package com.sss.yunweiadmin.controller;


import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sss.yunweiadmin.bean.WorkFlowBean;
import com.sss.yunweiadmin.common.operate.OperateLog;
import com.sss.yunweiadmin.common.result.ResponseResultWrapper;
import com.sss.yunweiadmin.common.utils.SpringUtil;
import com.sss.yunweiadmin.model.entity.*;
import com.sss.yunweiadmin.model.vo.*;
import com.sss.yunweiadmin.service.*;
import org.activiti.engine.task.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * 流程实例数据 前端控制器
 * </p>
 *
 * @author 任勇林
 * @since 2021-04-17
 */
@RestController
@RequestMapping("/processInstanceData")
@ResponseResultWrapper
public class ProcessInstanceDataController {
    @Autowired
    ProcessFormValue1Service processFormValue1Service;
    @Autowired
    ProcessFormValue2Service processFormValue2Service;
    @Autowired
    WorkFlowBean workFlowBean;
    @Autowired
    ProcessInstanceDataService processInstanceDataService;
    @Autowired
    ProcessInstanceNodeService processInstanceNodeService;
    @Autowired
    HttpSession httpSession;
    @Autowired
    SysDeptService sysDeptService;
    @Autowired
    ProcessDefinitionService processDefinitionService;
    @Autowired
    ProcessDefinitionTaskService processDefinitionTaskService;
    @Autowired
    ProcessDefinitionEdgeService processDefinitionEdgeService;
    @Autowired
    AsDeviceCommonService asDeviceCommonService;
    @Autowired
    ScoreService scoreService;
    @Autowired
    AsTypeService asTypeService;

    //流程实例 todo把查询条件换成VO
    @GetMapping("list")
    //接受参数是int时，不能没有值，会报null不能赋给int,但integer可以
    public IPage<ProcessInstanceData> list(int currentPage, int pageSize, String processName, String processStatus, String processType, String displayName, String deptName, String handleName, String no, String startDate, String endDate,Integer id) {
        QueryWrapper<ProcessInstanceData> queryWrapper = new QueryWrapper<ProcessInstanceData>().orderByDesc("id");
        if (ObjectUtil.isNotEmpty(id)) {
            System.out.println(id);
            queryWrapper.eq("id", id);
        }
        if (ObjectUtil.isNotEmpty(processName)) {
            queryWrapper.like("processName", processName);
        }
        if (ObjectUtil.isNotEmpty(processStatus)) {
            queryWrapper.eq("process_status", processStatus);
        }
        if (ObjectUtil.isNotEmpty(processType)) {
            List<ProcessDefinition> definitionList = processDefinitionService.list(new QueryWrapper<ProcessDefinition>().eq("process_type", processType));
            if (ObjectUtil.isNotEmpty(definitionList)) {
                queryWrapper.in("process_definition_id", definitionList.stream().map(ProcessDefinition::getId).collect(Collectors.toList()));
            } else {
                queryWrapper.in("process_definition_id", new ArrayList<>());
            }
        }
        if (ObjectUtil.isNotEmpty(displayName)) {
            queryWrapper.eq("display_name", displayName);
        }
        if (ObjectUtil.isNotEmpty(deptName)) {
            queryWrapper.like("dept_name", deptName);
        }
        if (ObjectUtil.isNotEmpty(handleName)) {
            List<ProcessInstanceNode> nodeList = processInstanceNodeService.list(new QueryWrapper<ProcessInstanceNode>().like("display_name", handleName));
            if (ObjectUtil.isNotEmpty(nodeList)) {
                List<Integer> processInstanceIdList = nodeList.stream().map(ProcessInstanceNode::getProcessInstanceDataId).collect(Collectors.toList());
                queryWrapper.in("id", processInstanceIdList);
            } else {
                queryWrapper.in("id", new ArrayList<>());
            }
        }
        if (ObjectUtil.isNotEmpty(no)) {
            List<AsDeviceCommon> asDeviceCommonList = asDeviceCommonService.list(new QueryWrapper<AsDeviceCommon>().like("no", no));
            if (ObjectUtil.isNotEmpty(asDeviceCommonList)) {
                List<Integer> asIdList = asDeviceCommonList.stream().map(AsDeviceCommon::getId).collect(Collectors.toList());
                List<ProcessFormValue2> value2List = processFormValue2Service.list(new QueryWrapper<ProcessFormValue2>().in("as_id", asIdList));
                if (ObjectUtil.isNotEmpty(value2List)) {
                    queryWrapper.in("act_process_instance_id", value2List.stream().map(ProcessFormValue2::getActProcessInstanceId).collect(Collectors.toList()));
                }
            } else {
                queryWrapper.in("act_process_instance_id", new ArrayList<>());
            }
        }
        if (ObjectUtil.isNotEmpty(startDate)) {
            String[] dateArr = startDate.split(",");
            queryWrapper.ge("start_datetime", dateArr[0] + " 00:00:00");
            queryWrapper.le("start_datetime", dateArr[1] + " 00:00:00");

        }
        if (ObjectUtil.isNotEmpty(endDate)) {
            String[] dateArr = endDate.split(",");
            queryWrapper.ge("end_datetime", dateArr[0] + " 00:00:00");
            queryWrapper.le("end_datetime", dateArr[1] + " 00:00:00");

        }
        return processInstanceDataService.page(new Page<>(currentPage, pageSize), queryWrapper);
    }

    //待办任务
    @GetMapping("myList")
    public IPage<ProcessInstanceData> myList(int currentPage, int pageSize, String processName, String processType, String startDate) {
        SysUser user = (SysUser) httpSession.getAttribute("user");
        if (user == null) {
            throw new RuntimeException("用户未登录");
        }
        QueryWrapper<ProcessInstanceData> queryWrapper = new QueryWrapper<ProcessInstanceData>().ne("process_status", "完成").like("display_current_step", user.getDisplayName()).like("login_current_step", user.getLoginName()).orderByDesc("id");
        if (ObjectUtil.isNotEmpty(processName)) {
            queryWrapper.like("processName", processName);
        }
        if (ObjectUtil.isNotEmpty(processType)) {
            List<ProcessDefinition> definitionList = processDefinitionService.list(new QueryWrapper<ProcessDefinition>().eq("process_type", processType));
            if (ObjectUtil.isNotEmpty(definitionList)) {
                queryWrapper.in("process_definition_id", definitionList.stream().map(ProcessDefinition::getId).collect(Collectors.toList()));
            } else {
                queryWrapper.in("process_definition_id", new ArrayList<>());
            }
        }
        if (ObjectUtil.isNotEmpty(startDate)) {
            String[] dateArr = startDate.split(",");
            queryWrapper.ge("start_datetime", dateArr[0] + " 00:00:00");
            queryWrapper.le("start_datetime", dateArr[1] + " 00:00:00");

        }
        return processInstanceDataService.page(new Page<>(currentPage, pageSize), queryWrapper);
    }

    //用于给传到前端的实例数据LIST中加入score信息
    private void setProcessInstanceDataScore(ProcessInstanceData processInstanceData) {
        //20211124 getone()里要加第二个参数false,这样在查找到多条记录时不会报错
        Score score = scoreService.getOne(new QueryWrapper<Score>().eq("business_id", processInstanceData.getId()).eq("node_type", 3),false);
        if (score != null) {
            processInstanceData.setScore(score.getScore());
        } else {
            processInstanceData.setScore(0);
        }

    }

    //已办任务
    @GetMapping("completeList")
    public IPage<ProcessInstanceData> completeList(int currentPage, int pageSize, String processName, String processType, String handleName, String no, String startDate, String endDate) {
        SysUser user = (SysUser) httpSession.getAttribute("user");
        if (user == null) {
            throw new RuntimeException("用户未登录");
        }
        QueryWrapper<ProcessInstanceData> queryWrapper = new QueryWrapper<ProcessInstanceData>().eq("process_status", "完成").eq("login_name", user.getLoginName()).orderByDesc("id");
        if (ObjectUtil.isNotEmpty(processName)) {
            queryWrapper.like("processName", processName);
        }
        if (ObjectUtil.isNotEmpty(processType)) {
            List<ProcessDefinition> definitionList = processDefinitionService.list(new QueryWrapper<ProcessDefinition>().eq("process_type", processType));
            if (ObjectUtil.isNotEmpty(definitionList)) {
                queryWrapper.in("process_definition_id", definitionList.stream().map(ProcessDefinition::getId).collect(Collectors.toList()));
            } else {
                queryWrapper.in("process_definition_id", new ArrayList<>());
            }
        }
        if (ObjectUtil.isNotEmpty(handleName)) {
            List<ProcessInstanceNode> nodeList = processInstanceNodeService.list(new QueryWrapper<ProcessInstanceNode>().like("display_name", handleName));
            if (ObjectUtil.isNotEmpty(nodeList)) {
                List<Integer> processInstanceIdList = nodeList.stream().map(ProcessInstanceNode::getProcessInstanceDataId).collect(Collectors.toList());
                queryWrapper.in("id", processInstanceIdList);
            } else {
                queryWrapper.in("id", new ArrayList<>());
            }
        }
        if (ObjectUtil.isNotEmpty(no)) {
            List<AsDeviceCommon> asDeviceCommonList = asDeviceCommonService.list(new QueryWrapper<AsDeviceCommon>().like("no", no));
            if (ObjectUtil.isNotEmpty(asDeviceCommonList)) {
                List<Integer> asIdList = asDeviceCommonList.stream().map(AsDeviceCommon::getId).collect(Collectors.toList());
                List<ProcessFormValue2> value2List = processFormValue2Service.list(new QueryWrapper<ProcessFormValue2>().in("as_id", asIdList));
                if (ObjectUtil.isNotEmpty(value2List)) {
                    queryWrapper.in("act_process_instance_id", value2List.stream().map(ProcessFormValue2::getActProcessInstanceId).collect(Collectors.toList()));
                }
            } else {
                queryWrapper.in("act_process_instance_id", new ArrayList<>());
            }
        }
        if (ObjectUtil.isNotEmpty(startDate)) {
            String[] dateArr = startDate.split(",");
            queryWrapper.ge("start_datetime", dateArr[0] + " 00:00:00");
            queryWrapper.le("start_datetime", dateArr[1] + " 00:00:00");

        }
        if (ObjectUtil.isNotEmpty(endDate)) {
            String[] dateArr = endDate.split(",");
            queryWrapper.ge("end_datetime", dateArr[0] + " 00:00:00");
            queryWrapper.le("end_datetime", dateArr[1] + " 00:00:00");

        }
        //遍历page并加入score信息
        IPage<ProcessInstanceData> page = processInstanceDataService.page(new Page<>(currentPage, pageSize), queryWrapper);
        List<ProcessInstanceData> list = page.getRecords();
        for (ProcessInstanceData processInstanceData : list) {
            this.setProcessInstanceDataScore(processInstanceData);
        }
        return page;
    }

    //当前工单
    @GetMapping("currentList")
    public IPage<ProcessInstanceData> currentList(int currentPage, int pageSize, int asId) {
        //
        List<ProcessFormValue2> value2List = processFormValue2Service.list(new QueryWrapper<ProcessFormValue2>().eq("as_id", asId));
        if (ObjectUtil.isNotEmpty(value2List)) {
            List<String> list = value2List.stream().map(ProcessFormValue2::getActProcessInstanceId).collect(Collectors.toList());
            return processInstanceDataService.page(new Page<>(currentPage, pageSize), new QueryWrapper<ProcessInstanceData>().ne("process_status", "完成").in("act_process_instance_id", list));
        }
        return null;
    }

    //历史工单
    @GetMapping("historyList")
    public IPage<ProcessInstanceData> historyList(int currentPage, int pageSize, int asId) {
        //
        List<ProcessFormValue2> value2List = processFormValue2Service.list(new QueryWrapper<ProcessFormValue2>().eq("as_id", asId));
        if (ObjectUtil.isNotEmpty(value2List)) {
            List<String> list = value2List.stream().map(ProcessFormValue2::getActProcessInstanceId).collect(Collectors.toList());
            return processInstanceDataService.page(new Page<>(currentPage, pageSize), new QueryWrapper<ProcessInstanceData>().eq("process_status", "完成").in("act_process_instance_id", list));
        }
        return null;
    }

    //201211111流程实例发起时的校验：如果存在互斥流程实例，则返回互斥的流程实例LIST，当然如果不存在就返回null
    private List<ProcessInstanceData> validate(StartProcessVO startProcessVO) {
        SysUser user = (SysUser) httpSession.getAttribute("user");
        System.out.println(startProcessVO.getValue2List().get(0).getAsId());
        if (user == null) {
            throw new RuntimeException("用户未登录");
        }

        int processDefId = startProcessVO.getProcessDefinitionId();
        String processType = processDefinitionService.getById(processDefId).getProcessType();
        int assetId = startProcessVO.getValue2List().get(0).getAsId();
        //判空，如果是空，证明是没有关联资产的，直接放行
        if (ObjectUtil.isEmpty(assetId)) return null;
        //需要加对资产TYPE的判断，只有“计算机类”的设备需要做互斥判断todo
        int typeId = asDeviceCommonService.getById(assetId).getTypeId();
        //查询资产类型表中typeId对应的level=2/上级分类的名称是不是“计算机”，不是的话，直接放行
        int p_typeId = asTypeService.getOne(new QueryWrapper<AsType>().eq("id", typeId)).getPid();
        if (!asTypeService.getOne(new QueryWrapper<AsType>().eq("id", p_typeId)).getName().contains("计算机"))
            return null;
        //获取资产ID对应的所有ACTINST IDList
        List<Map<String, Object>> actProcessInsIdListMap = processFormValue2Service.listMaps(new QueryWrapper<ProcessFormValue2>().eq("as_id", assetId).select("act_process_instance_id"));
        //判断  actProcessInsIdListMap是不是空。如果是空的，那么不用再校验了--放行
        if (ObjectUtil.isEmpty(actProcessInsIdListMap)) return null;
        List<String> actProcessInsIdList = actProcessInsIdListMap.stream().map(item -> item.get("act_process_instance_id").toString()).collect(Collectors.toList());
        //查出互斥定义的定义IDlist
        List<Map<String, Object>> mutexDefIdListMap;//不可能为空，肯定有值
        if (processType.contains("启用") || processType.contains("停用")) {
            mutexDefIdListMap = processDefinitionService.listMaps(new QueryWrapper<ProcessDefinition>().or().like("process_type", "启用").or().like("process_type", "停用").select("id"));
        } else {
            mutexDefIdListMap = processDefinitionService.listMaps(new QueryWrapper<ProcessDefinition>().like("process_type", processType).select("id"));
        }
        List<Integer> mutexDefIdList = mutexDefIdListMap.stream().map(item -> Integer.valueOf(item.get("id").toString())).collect(Collectors.toList());
        //取出互斥的业务实例表
        List<ProcessInstanceData> processInstanceDataList = processInstanceDataService.list(new QueryWrapper<ProcessInstanceData>().in("process_definition_id", mutexDefIdList).ne("process_status", "完成").in("act_process_instance_id", actProcessInsIdList));
        return processInstanceDataList;
    }
    @OperateLog(module = "流程模块", type = "发起流程")
    @PostMapping("start")//20211112重写
    public StartProcessResultVO start(@RequestBody StartProcessVO startProcessVO) {

        StartProcessResultVO startProcessResultVO = new StartProcessResultVO();
        List<ProcessInstanceData> processInstanceDataList = this.validate(startProcessVO);
        if(ObjectUtil.isNotEmpty(processInstanceDataList)){//有值即存在互斥实例
            startProcessResultVO.setProcessInstanceDataList(processInstanceDataList);
            startProcessResultVO.setIsSuccess(false);
        }else{
            startProcessResultVO.setIsSuccess( processInstanceDataService.start(startProcessVO));
        }

        return  startProcessResultVO;
    }
    @PostMapping("start2")//备份原版，改为start2
    public boolean start2(@RequestBody StartProcessVO startProcessVO) {
        SysUser user = (SysUser) httpSession.getAttribute("user");
        System.out.println(startProcessVO.getValue2List().get(0).getAsId());
        if (user == null) {
            throw new RuntimeException("用户未登录");
        }
        return processInstanceDataService.start(startProcessVO);
    }
    @OperateLog(module = "流程模块", type = "处理流程")
    @PostMapping("handle")
    public boolean handle(@RequestBody CheckProcessVO checkProcessVO) {
        SysUser user = (SysUser) httpSession.getAttribute("user");
        if (user == null) {
            throw new RuntimeException("用户未登录");
        }
        return processInstanceDataService.handle(checkProcessVO);
    }

    @PostMapping("modify")
    public boolean modify(@RequestBody ModifyProcessFormVO modifyProcessFormVO) {
        return processInstanceDataService.modifyProcessForm(modifyProcessFormVO);
    }

    @GetMapping("get")
    public ProcessInstanceData getById(String id) {
        return processInstanceDataService.getById(id);
    }
    @OperateLog(module = "流程模块", type = "删除流程")
    @PostMapping("delete")
    public boolean delete(@RequestBody ProcessInstanceData processInstanceData) {
        return processInstanceDataService.delete(processInstanceData);
    }

    @GetMapping("getStartProcessConditionVO")
    public StartProcessConditionVO getStartProcessConditionVO(Integer processDefinitionId) {
        StartProcessConditionVO startProcessConditionVO = new StartProcessConditionVO();
        //取出流程定义中的第一个发起任务节点
        List<ProcessDefinitionTask> startEventList = processDefinitionTaskService.list(new QueryWrapper<ProcessDefinitionTask>().eq("process_definition_id", processDefinitionId).eq("task_type", "bpmn:startEvent"));
        List<ProcessDefinitionEdge> edgeList = processDefinitionEdgeService.list(new QueryWrapper<ProcessDefinitionEdge>().eq("process_definition_id", processDefinitionId).in("source_id", startEventList.stream().map(ProcessDefinitionTask::getTaskId).collect(Collectors.toList())));
        String startTaskId;
        if (ObjectUtil.isNotEmpty(edgeList)) {
            startTaskId = edgeList.get(0).getTargetId();
        } else {
            throw new RuntimeException("流程图错误,缺少开始节点");
        }
        if (startTaskId != null) {
            //获取多条连线
            List<String> buttonNameList = workFlowBean.getButtonNameList(processDefinitionId, startTaskId);
            if (ObjectUtil.isNotEmpty(buttonNameList)) {
                startProcessConditionVO.setButtonNameList(buttonNameList);
            }
            ProcessDefinitionTask processDefinitionTask = processDefinitionTaskService.getOne(new QueryWrapper<ProcessDefinitionTask>().eq("process_definition_id", processDefinitionId).eq("task_id", startTaskId));
            //是否有下一步处理人
            startProcessConditionVO.setHaveNextUser(processDefinitionTask.getHaveNextUser());
        }

        return startProcessConditionVO;
    }

    @GetMapping("getCheckProcessConditionVO")
    public CheckProcessConditionVO getUserTaskConditionVO(Integer processDefinitionId, String actProcessInstanceId) {
        SysUser user = (SysUser) httpSession.getAttribute("user");
        if (user == null) {
            throw new RuntimeException("用户未登录");
        }
        CheckProcessConditionVO checkProcessConditionVO = new CheckProcessConditionVO();
        //取出我的一个任务
        List<Task> taskList = workFlowBean.getMyTask(actProcessInstanceId);
        if (ObjectUtil.isEmpty(taskList)) {
            throw new RuntimeException("没有用户任务");
        }
        Task actTask = taskList.get(0);
        //获取多条连线
        List<String> buttonNameList = workFlowBean.getButtonNameList(processDefinitionId, actTask.getTaskDefinitionKey());
        if (ObjectUtil.isNotEmpty(buttonNameList)) {
            checkProcessConditionVO.setButtonNameList(buttonNameList);
        }
        //是否允许 意见，修改表单，下一步处理人
        ProcessDefinitionTask processDefinitionTask = processDefinitionTaskService.getOne(new QueryWrapper<ProcessDefinitionTask>().eq("process_definition_id", processDefinitionId).eq("task_id", actTask.getTaskDefinitionKey()));
        if (processDefinitionTask.getHaveComment().equals("是")) {
            if (processDefinitionTask.getTaskType().equals("bpmn:approvalTask")) {
                //审批任务
                checkProcessConditionVO.setCommentTitle("意见备注");
            } else {
                //处理任务
                checkProcessConditionVO.setCommentTitle("处理备注");
            }
        }
        checkProcessConditionVO.setHaveComment(processDefinitionTask.getHaveComment());
        checkProcessConditionVO.setHaveEditForm(processDefinitionTask.getHaveEditForm());
        checkProcessConditionVO.setHaveNextUser(processDefinitionTask.getHaveNextUser());
        checkProcessConditionVO.setHaveOperate(processDefinitionTask.getHaveOperate());
        return checkProcessConditionVO;
    }

    @GetMapping("getActiveTaskIdList")
    public List<String> getActiveTaskIdList(String actProcessInstanceId) {
        List<String> list = new ArrayList<>();
        List<Task> activeTaskList = workFlowBean.getActiveTask(actProcessInstanceId);
        if (ObjectUtil.isNotEmpty(activeTaskList)) {
            list = activeTaskList.stream().map(Task::getTaskDefinitionKey).collect(Collectors.toList());
        }
        return list;
    }
}
