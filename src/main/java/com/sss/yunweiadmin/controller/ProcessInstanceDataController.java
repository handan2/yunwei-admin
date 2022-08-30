package com.sss.yunweiadmin.controller;


import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sss.yunweiadmin.bean.WorkFlowBean;
import com.sss.yunweiadmin.common.operate.OperateLog;
import com.sss.yunweiadmin.common.result.ResponseResult;
import com.sss.yunweiadmin.common.result.ResponseResultWrapper;
import com.sss.yunweiadmin.common.utils.SpringUtil;
import com.sss.yunweiadmin.model.entity.*;
import com.sss.yunweiadmin.model.vo.*;
import com.sss.yunweiadmin.service.*;
import org.activiti.engine.task.Task;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.method.P;
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
    public IPage<ProcessInstanceData> list(int currentPage, int pageSize, String processName, String processStatus, String processType, String displayName, String deptName, String handleName, String no, String startDate, String endDate, Integer id) {
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
        Score score = scoreService.getOne(new QueryWrapper<Score>().eq("business_id", processInstanceData.getId()).eq("node_type", 3), false);
        if (score != null) {
            processInstanceData.setScore(score.getScore());
        } else {
            processInstanceData.setScore(0);
        }

    }

    //已办任务
    //20220719回头改:这个loginNmae/displayName矛盾
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
    //20220806已提工单
    @GetMapping("listForCommitted")
    public IPage<ProcessInstanceData> listForCommitted(int currentPage, int pageSize) {
        SysUser user = (SysUser) httpSession.getAttribute("user");
        if (user == null) {
            throw new RuntimeException("用户未登录");
        }
        //遍历page并加入score信息
        IPage<ProcessInstanceData> page =  processInstanceDataService.page(new Page<>(currentPage, pageSize), new QueryWrapper<ProcessInstanceData>().eq("login_name", user.getLoginName()).orderByDesc("id"));
        List<ProcessInstanceData> list = page.getRecords();
        for (ProcessInstanceData processInstanceData : list) {
            this.setProcessInstanceDataScore(processInstanceData);
        }
        return page;

    }

    //201211111流程实例发起时的校验：如果存在互斥流程实例，则返回互斥的流程实例LIST，当然如果不存在就返回null
    //20220824 加限制只寻找“资产类型”一致的互斥流程定义的实例
    private List<ProcessInstanceData> validate(ProcessFormValue1 value1, List<ProcessFormValue2> value2List, Integer processInstanceDataId) {//StartProcessVO/CheckProcessVO
        SysUser user = (SysUser) httpSession.getAttribute("user");
        if (user == null) {
            throw new RuntimeException("用户未登录");
        }
        if (ObjectUtil.isEmpty(value1)) {
            return null;
        }
        int processDefId = value1.getProcessDefinitionId();
        ProcessDefinition proDef = processDefinitionService.getById(processDefId);
        if(proDef.getProcessName().contains("故障报修")){//202208034 故障报修流程不判断validate
            return  null;
        }
        String processType = proDef.getProcessType();
        //20220528加判空：不能用OBjectUtil(不能判断size=0这种List）
        if (CollUtil.isEmpty(value2List)) {
            return null;
        }
        //20220721 ：读所有资产，并只判断流程定义中“主类型”对应的资产
        int mainTypeIdForDef = proDef.getAsTypeId();
        List<Integer> mainTypeIdListForDef = asTypeService.getTypeIdList(mainTypeIdForDef);
        List<ProcessFormValue2> value2ListForFilter = null;
        if (CollUtil.isNotEmpty(mainTypeIdListForDef)) {
            value2ListForFilter = value2List.stream().filter(item -> mainTypeIdListForDef.contains(asDeviceCommonService.getById(item.getAsId()).getTypeId())).collect(Collectors.toList());
        }
        if (CollUtil.isEmpty(value2ListForFilter)) {
            return null;
        }
        ProcessFormValue2 value2Filterd = value2ListForFilter.get(0);//约定：一个流程只有一个主类型对应资产
        int assetId = value2Filterd.getAsId();
//        //判空，如果是空，证明是没有关联资产的，直接放行  //20220528暂把这个注释了
//        if (ObjectUtil.isEmpty(assetId)) return null;
        //加对资产TYPE的判断，只有信息设备需要做互斥判断
        int typeId = asDeviceCommonService.getById(assetId).getTypeId();
        //查询资产类型表中typeId对应的level=1/上级分类的名称是不是“信息设备”，不是的话，直接放行
        int p_typeId = asTypeService.getOne(new QueryWrapper<AsType>().eq("id", typeId)).getPid();
        int p_p_typeId = asTypeService.getOne(new QueryWrapper<AsType>().eq("id", p_typeId)).getPid();
        if (!asTypeService.getOne(new QueryWrapper<AsType>().eq("id", p_p_typeId)).getName().contains("信息设备"))
            return null;

        //获取资产ID对应的所有ACTINST IDList
        List<Map<String, Object>> actProcessInsIdListMap = null;
        if (ObjectUtil.isEmpty(processInstanceDataId))
            actProcessInsIdListMap = processFormValue2Service.listMaps(new QueryWrapper<ProcessFormValue2>().eq("as_id", assetId).select("act_process_instance_id"));
        else {
            actProcessInsIdListMap = processFormValue2Service.listMaps(new QueryWrapper<ProcessFormValue2>().eq("as_id", assetId).ne("act_process_instance_id", processInstanceDataService.getById(processInstanceDataId).getActProcessInstanceId()).select("act_process_instance_id"));
        }
        //判断  actProcessInsIdListMap是不是空。如果是空的，那么不用再校验了--放行
        if (ObjectUtil.isEmpty(actProcessInsIdListMap)) return null;
        List<String> actProcessInsIdList = actProcessInsIdListMap.stream().map(item -> item.get("act_process_instance_id").toString()).collect(Collectors.toList());
        //查出互斥定义的定义IDlist
        List<Map<String, Object>> mutexDefIdListMap;//不可能为空，肯定有值
        QueryWrapper<ProcessDefinition> queryWrapper= new QueryWrapper<ProcessDefinition>().eq("as_type_id",mainTypeIdForDef);;
        if (processType.contains("申领") || processType.contains("停用")) {
            mutexDefIdListMap = processDefinitionService.listMaps(queryWrapper.like("process_type", "申领").or().like("process_type", "停用").select("id"));
        } else {//查出同类型流程
            mutexDefIdListMap = processDefinitionService.listMaps(queryWrapper.like("process_type", processType).notLike("process_name","故障报修").select("id"));//20220801 同类型流程要排除“故障报修”：因为故障报修流程在“发起新流程提交时&&老流程并没有关闭”
        }
        List<Integer> mutexDefIdList = mutexDefIdListMap.stream().map(item -> Integer.valueOf(item.get("id").toString())).collect(Collectors.toList());
        //取出互斥的业务实例表
        List<ProcessInstanceData> processInstanceDataList = processInstanceDataService.list(new QueryWrapper<ProcessInstanceData>().in("process_definition_id", mutexDefIdList).ne("process_status", "完成").in("act_process_instance_id", actProcessInsIdList));
        return processInstanceDataList;
    }

    @GetMapping("getOneDeviceByProcessInstId")
//20220702加,注意逻辑是根据流程类型中的“资产类型”ID，来查找对应的设备：约定同一个资产类型只有只能选择一个资产：这条需要单独记录在一个地方
    public AsDeviceCommon getOneDeviceByProcessInstId(Integer processInstanceDataId) {
        ProcessInstanceData processInstanceData = processInstanceDataService.getById(processInstanceDataId);
        List<Integer> assetIdList = processFormValue2Service.list(new QueryWrapper<ProcessFormValue2>().eq("act_process_instance_id", processInstanceData.getActProcessInstanceId())).stream().map(item -> item.getAsId()).collect(Collectors.toList());
        if (CollUtil.isNotEmpty(assetIdList))
            return asDeviceCommonService.getById(assetIdList.get(0));//审批型流程的前置流程：约定：只能选择一个资产
        return null;

    }

    @GetMapping("getNewProcessDef")//20220702加
    public ProcessDefinition getNewProcessDef(Integer processInstanceDataId) {
        ProcessInstanceData processInstanceData = processInstanceDataService.getById(processInstanceDataId);
        String actProcessInstanceId = processInstanceData.getActProcessInstanceId();
        List<Task> myTaskList = workFlowBean.getMyTask(actProcessInstanceId);
        Task myTask = myTaskList.get(0);
        Object nextProcessNameObj = workFlowBean.getProcessVariable(myTask);
        // ProcessDefinition nextProcess  = processDefinitionService.getOne(new QueryWrapper<ProcessDefinition>().eq("status","启用").eq("have_display","是").eq("process_name",(String)nextProcessNameObj));
        return processDefinitionService.getOne(new QueryWrapper<ProcessDefinition>().eq("status", "启用").eq("have_display", "是").eq("process_name", (String) nextProcessNameObj));
        //return new StartOrHandleProcessResultVO();

    }

    @GetMapping("getOperateRecordForRepair")//20220829加:专门用于故障报修流程：该 流程只有一个处理节点
    public  ResponseResult getOperateRecordForRepair(Integer processInstanceDataId) {
        ProcessInstanceNode node = processInstanceNodeService.list(new QueryWrapper<ProcessInstanceNode>().eq("process_instance_data_id",processInstanceDataId).like("task_name","处理")).get(0);
        return ResponseResult.success(node.getComment()+"   " +node.getDisplayName()+" "+ node.getEndDatetime());
    }

    @OperateLog(module = "流程模块", type = "发起流程")
    @PostMapping("endAndStart")//20211112重写
    public StartOrHandleProcessResultVO endAndStart(@RequestBody EndAndStartProcessVO endAndStartProcessVO) {
        StartOrHandleProcessResultVO startOrHandleProcessResultVO = new StartOrHandleProcessResultVO();
        List<ProcessInstanceData> processInstanceDataList = this.validate(endAndStartProcessVO.getValue1(), endAndStartProcessVO.getValue2List(), null);
        if (ObjectUtil.isNotEmpty(processInstanceDataList)) {//有值即存在互斥实例
            startOrHandleProcessResultVO.setProcessInstanceDataList(processInstanceDataList);
            startOrHandleProcessResultVO.setIsSuccess(false);
        } else {
        startOrHandleProcessResultVO.setIsSuccess(processInstanceDataService.endAndStart(endAndStartProcessVO));
        }
        return startOrHandleProcessResultVO;
    }

    @OperateLog(module = "流程模块", type = "发起流程")
    @PostMapping("start")//20211112重写
    public StartOrHandleProcessResultVO start(@RequestBody StartProcessVO startProcessVO) {
        StartOrHandleProcessResultVO startOrHandleProcessResultVO = new StartOrHandleProcessResultVO();
        List<ProcessInstanceData> processInstanceDataList = this.validate(startProcessVO.getValue1(), startProcessVO.getValue2List(), null);
        if (ObjectUtil.isNotEmpty(processInstanceDataList)) {//有值即存在互斥实例
            startOrHandleProcessResultVO.setProcessInstanceDataList(processInstanceDataList);
            startOrHandleProcessResultVO.setIsSuccess(false);
        } else {
            startOrHandleProcessResultVO.setIsSuccess(processInstanceDataService.start(startProcessVO));
        }
        return startOrHandleProcessResultVO;
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
    //返回值改造成自定义VO
    public StartOrHandleProcessResultVO handle(@RequestBody CheckProcessVO checkProcessVO) {
        SysUser user = (SysUser) httpSession.getAttribute("user");
        if (user == null) {
            throw new RuntimeException("用户未登录");
        }
        StartOrHandleProcessResultVO startOrHandleProcessResultVO = new StartOrHandleProcessResultVO();
        List<ProcessInstanceData> processInstanceDataList = this.validate(checkProcessVO.getValue1(), checkProcessVO.getValue2List(), checkProcessVO.getProcessInstanceDataId());
        if (ObjectUtil.isNotEmpty(processInstanceDataList)) {//有值即存在互斥实例
            startOrHandleProcessResultVO.setProcessInstanceDataList(processInstanceDataList);
            startOrHandleProcessResultVO.setIsSuccess(false);
        } else {
            startOrHandleProcessResultVO.setIsSuccess(processInstanceDataService.handle(checkProcessVO));
        }

        return startOrHandleProcessResultVO;
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

    //20220525todo在StartProcessConditionVO /CheckProcessConditionVO增加 hide_group_ids/hide_group_label字段
    @GetMapping("getStartProcessConditionVO")
    public StartProcessConditionVO getStartProcessConditionVO(Integer processDefinitionId) {
        StartProcessConditionVO startProcessConditionVO = new StartProcessConditionVO();
        //取出流程定义中的第一个发起任务节点
        //注意startEvent是比"startTask"之前那个节点，后者才是实际的发起人节点
        ProcessDefinitionTask startEvent = processDefinitionTaskService.getOne(new QueryWrapper<ProcessDefinitionTask>().eq("process_definition_id", processDefinitionId).eq("task_type", "bpmn:startEvent"));
        List<ProcessDefinitionEdge> edgeList;
        //20220517 加判空
        if (ObjectUtil.isNotEmpty(startEvent)) {
            //20220608感觉可以用getOne:毕竟startEvent到StartTask只有一条线）
            edgeList = processDefinitionEdgeService.list(new QueryWrapper<ProcessDefinitionEdge>().eq("process_definition_id", processDefinitionId).eq("source_id", startEvent.getTaskDefKey()));
        } else {
            throw new RuntimeException("该流程可能不存在，找不到起始结点信息");
        }
        String startTaskDefKey;
        if (ObjectUtil.isNotEmpty(edgeList)) {
            startTaskDefKey = edgeList.get(0).getTargetId();
        } else {
            throw new RuntimeException("流程图错误,缺少开始节点");
        }
        if (startTaskDefKey != null) {
            //获取多条连线
            List<String> buttonNameList = workFlowBean.getButtonNameList(processDefinitionId, startTaskDefKey);
            if (ObjectUtil.isNotEmpty(buttonNameList)) {
                startProcessConditionVO.setButtonNameList(buttonNameList);
            }
            ProcessDefinitionTask startTask = processDefinitionTaskService.getOne(new QueryWrapper<ProcessDefinitionTask>().eq("process_definition_id", processDefinitionId).eq("task_def_key", startTaskDefKey));
            BeanUtils.copyProperties(startTask, startProcessConditionVO);//20220828
//            startProcessConditionVO.setHaveNextUser(startTask.getHaveNextUser());
//            startProcessConditionVO.setHideGroupIds(startTask.getHideGroupIds());
//            startProcessConditionVO.setHideGroupLabel(startTask.getHideGroupLabel());
//            startProcessConditionVO.setHaveSelectAsset(startTask.getHaveSelectAsset());//20220724加
        }

        return startProcessConditionVO;
    }

    @GetMapping("getCheckTaskVO")
    public CheckTaskVO getCheckTaskVO(Integer processDefinitionId, String actProcessInstanceId) {
        SysUser user = (SysUser) httpSession.getAttribute("user");
        if (user == null) {
            throw new RuntimeException("用户未登录");
        }
        CheckTaskVO checkTaskVO = new CheckTaskVO();
        //取出我的一个任务
        List<Task> taskList = workFlowBean.getMyTask(actProcessInstanceId);
        if (ObjectUtil.isEmpty(taskList)) {
            throw new RuntimeException("没有用户任务，请尝试刷新列表");
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
//        checkTaskVO.setHaveComment(checkTask.getHaveComment());
//        checkTaskVO.setHaveEditForm(checkTask.getHaveEditForm());
//        checkTaskVO.setHaveNextUser(checkTask.getHaveNextUser());
//        checkTaskVO.setHaveOperate(checkTask.getHaveOperate());
//        checkTaskVO.setHideGroupIds(checkTask.getHideGroupIds());
//        checkTaskVO.setHideGroupLabel(checkTask.getHideGroupLabel());
//        checkTaskVO.setHaveSelectAsset(checkTask.getHaveSelectAsset());
        return checkTaskVO;
    }

    @GetMapping("getActiveTaskDefKeyList")
    public List<String> getActiveTaskDefKeyList(String actProcessInstanceId) {
        List<String> list = new ArrayList<>();
        List<Task> activeTaskList = workFlowBean.getActiveTask(actProcessInstanceId);
        if (ObjectUtil.isNotEmpty(activeTaskList)) {
            list = activeTaskList.stream().map(Task::getTaskDefinitionKey).collect(Collectors.toList());
        }
        return list;
    }
}
