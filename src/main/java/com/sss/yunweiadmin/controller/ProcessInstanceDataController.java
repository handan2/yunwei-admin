package com.sss.yunweiadmin.controller;


import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.annotation.JsonAppend;
import com.google.common.collect.Maps;
import com.sss.yunweiadmin.bean.WorkFlowBean;
import com.sss.yunweiadmin.common.config.GlobalParam;
import com.sss.yunweiadmin.common.operate.OperateLog;
import com.sss.yunweiadmin.common.result.ResponseResult;
import com.sss.yunweiadmin.common.result.ResponseResultWrapper;
import com.sss.yunweiadmin.common.utils.CustomFunction;
import com.sss.yunweiadmin.common.utils.SpringUtil;
import com.sss.yunweiadmin.model.entity.*;
import com.sss.yunweiadmin.model.vo.*;
import com.sss.yunweiadmin.model.excel.ProcessInstanceExcel;
import com.sss.yunweiadmin.service.*;
import lombok.SneakyThrows;
import org.activiti.engine.HistoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.runtime.Execution;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.checkerframework.checker.units.qual.A;
import org.omg.CORBA.OBJ_ADAPTER;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.security.access.method.P;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
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
    @Autowired
    RecordForReportService recordForReportService;
    @Autowired
    OperateeLogService operateeLogService;
    @Autowired
    SysDicService sysDicService;
    @Autowired
    SysUserService sysUserService;
    @Autowired
    RuntimeService runtimeService;
    @Autowired
    HistoryService historyService;



    @GetMapping("getUserTodoList")
    public List<ColumnChartVO> getUserTodoList(Integer processDefinitionId, String hideGroupIds) {

        List<SysUser> sysUserList = sysUserService.list(new  QueryWrapper<SysUser>().eq("org_id",0).eq("status","正常").eq("dept_id",GlobalParam.deptIDForXXH).notLike("display_name","审计").notLike("display_name","管理").notLike("display_name","安全").notLike("display_name","系统"));
        Map<String, String> loginAndDisplayNameMap = new HashMap<>();
        sysUserList.forEach( user -> {
            // 可选：过滤空Key，避免Map出现null键
            if (ObjectUtil.isNotEmpty(user.getLoginName())) {
                loginAndDisplayNameMap.put(user.getLoginName(), user.getDisplayName());
            }
        });
        List<String> sysUserLoginNameList = sysUserList.stream().map(i->i.getLoginName()).collect(Collectors.toList());
        List<String> sysUserDisplayNameList = sysUserList.stream().map(i->i.getDisplayName()).collect(Collectors.toList());
        List<String> loginNameListForOperatorForCross = null;//跨系统待办人员
        QueryWrapper<ProcessInstanceData> queryWrapper = new  QueryWrapper<>();//目前唯二一处不需要加eq("org_id",GlobalParam.orgId)
        if(GlobalParam.orgId == 0) {//只让orgId为0时，即信息化中心来执行；约定了所与惯性公司的ID分别0、1
            SysDic dic_operatorForCross = sysDicService.getOne(new  QueryWrapper<SysDic>().eq("org_id",1).eq("flag", "跨系统待办人员").orderByAsc("sort"));
            if(ObjectUtil.isNotEmpty(dic_operatorForCross))
                loginNameListForOperatorForCross = Arrays.asList(dic_operatorForCross.getName().split(","));//Arrays.asList(operatorTypeIds.split(","))
           // if(CollUtil.isNotEmpty(loginNameListForOperatorForCross) && loginNameListForOperatorForCross.contains(user.getLoginName()))//同时要获取org_id为 0、1的待办 //
                queryWrapper.in("org_id",Arrays.asList(new Integer[]{0,1})).notIn("process_status", Arrays.asList(new String[]{"完成","中止","终止",""})).orderByDesc("last_commit_datetime");//20241015 倒排主键：id改为last_commit_datetime
           // else
              //  queryWrapper.eq("org_id",GlobalParam.orgId).notIn("process_status", Arrays.asList(new String[]{"完成","中止","终止"})).like("display_current_step", user.getDisplayName()).like("login_current_step", user.getLoginName()).orderByDesc("last_commit_datetime");//20241015 倒排主键：id改为last_commit_datetime
        } else
            queryWrapper.eq("org_id",GlobalParam.orgId).notIn("process_status", Arrays.asList(new String[]{"完成","中止","终止",""})).orderByDesc("last_commit_datetime");//20241015 倒排主键：id改为last_commit_datetime
        List<ProcessInstanceData> processInstanceDataList = processInstanceDataService.list(queryWrapper);
        Map<String, Integer> map = Maps.newHashMap();
        sysUserDisplayNameList.stream().forEach(item->map.put(item,0));
        List<ColumnChartVO> list = new ArrayList<>();
        List<String>stringList = new ArrayList<>();
        for(ProcessInstanceData p : processInstanceDataList){
            if(ObjectUtil.isNotEmpty(p.getLoginCurrentStep())){
                String[] strArr = p.getLoginCurrentStep().split(",");
                for(String s : strArr){
                    if(ObjectUtil.isNotEmpty(loginAndDisplayNameMap.get(s))){
                        map.put(loginAndDisplayNameMap.get(s), MapUtil.getInt(map, loginAndDisplayNameMap.get(s), 0) + 1);
                        if(s.equals("kanglei"))
                            stringList.add(p.getId().toString());
                    }

                }
            }
        }

         list = map.entrySet().stream()
                 .filter(entry -> StrUtil.isNotBlank(entry.getKey()) && entry.getValue() != null)
                 .map(entry -> new ColumnChartVO(entry.getKey(), entry.getValue()))
                 .collect(Collectors.toList());
     //   ColumnChartVO valueLabelVO = new ColumnChartVO("aaa",12);
       // list.add(valueLabelVO);
        System.out.println(map.get("康磊"));
        return list;
    }

    /**
     * 将流程当前活动节点跳转到指定节点
     * @param processInstanceId 流程实例 ID
     * @param targetActivityId 目标节点 ID
     */
    public void jumpToActivity(String processInstanceId, String targetActivityId) {
        // 获取当前流程实例
        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();

        if (processInstance == null) {
            throw new RuntimeException("未找到流程实例，ID: " + processInstanceId);
        }

        // 获取当前活动节点
        List<Execution> executions = runtimeService.createExecutionQuery()
                .processInstanceId(processInstanceId)
                .list();

//        for (Execution execution : executions) {
//            // 删除当前活动实例
//            runtimeService.signal(execution.getId());
//            runtimeService.deleteExecution
//            // 创建新的执行实例并跳转到指定节点
//            runtimeService.createProcessInstanceBuilder()
//                    .processInstanceId(processInstanceId)
//                    .startBeforeActivity(targetActivityId)
//                    .start();
//        }
    }

    //20231205 专用于手工记日志：流程相关

    void saveLog(String mode, String proInfo){
        SysUser user = (SysUser) httpSession.getAttribute("user");

        OperateeLog operateeLog = new OperateeLog();
        if(ObjectUtil.isNotEmpty(httpSession.getAttribute("crossOrgId")))//20241107如果是穿透流程
            operateeLog.setOrgId((Integer)httpSession.getAttribute("crossOrgId"));
        String paramStr = "";
        if("start".equals(mode)){
            paramStr =  "发起流程【"+ proInfo + "】";
            operateeLog.setOperateType("发起流程");
            operateeLog.setMethod("com.sss.yunweiadmin.controller.start()");
        } else if("handle".equals(mode)){
            String[] proInfoArr = proInfo.split("\\,");
            paramStr =  "处理流程【"+ proInfoArr[0] + "】---编号：" + proInfoArr[1] ;
            operateeLog.setOperateType("处理流程");
            operateeLog.setMethod("com.sss.yunweiadmin.controller.handle()");
        }

//        if(CollUtil.isNotEmpty(roleNameList)) {
//            paramStr = paramStr +  String.join(",", roleNameList);
//        }
        operateeLog.setParam(paramStr);
        operateeLog.setOperateModule("流程模块");

        operateeLog.setLoginName(user.getLoginName());
        operateeLog.setDisplayName(user.getDisplayName());
        //operateeLog.setIp(ServletUtil.getClientIP(request));
        String ip = (String) httpSession.getAttribute("IP");
        operateeLog.setIp(ip);

        operateeLog.setCreateDatetime(LocalDateTime.now());
        operateeLogService.save(operateeLog);

    }


    //20230314
    @GetMapping("addCandidate")
    public void addCandidateByUrl(String actProcessInstanceId, String loginName){
         workFlowBean.addCandidateByUrl(actProcessInstanceId,loginName);

    }
   //20230314
    @GetMapping("lock")
    public void lock(HttpServletRequest request, Integer processInstanceDataId, String taskName){
        SysUser user = (SysUser) request.getSession().getAttribute("user");
        if(ObjectUtil.isNotEmpty(user) && ObjectUtil.isNotEmpty(processInstanceDataId)){
            ProcessInstanceData processInstanceData = processInstanceDataService.getById(processInstanceDataId);
           // processInstanceData.setLockDatetime(LocalDateTime.now());
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String dateStr = LocalDateTime.now().format(fmt);
            List<String> lockUserListNew  = new ArrayList<>();
            if(ObjectUtil.isNotEmpty(processInstanceData.getLockUsername())){
                String [] lockUserStrArr = processInstanceData.getLockUsername().split(",");
                // boolean isExsit = false;

                if(ArrayUtil.isNotEmpty(lockUserStrArr)){
                    for(String item :lockUserStrArr){
                        String[] itemArr = item.split("\\|");
                        if(itemArr[0].equals(taskName)){
                            //跳过
                        } else
                            lockUserListNew.add(item);
                    }

                }
            }
            lockUserListNew.add(taskName + "|" + user.getDisplayName() + "|" + dateStr);
            processInstanceData.setLockUsername(lockUserListNew.stream().collect(Collectors.joining(",")));
            processInstanceDataService.updateById(processInstanceData);
        }


    }


    //流程实例 todo把查询条件换成VO
    @GetMapping("list")
    //接受参数是int时，不能没有值，会报null不能赋给int,但integer可以
    public IPage<ProcessInstanceData> list(int currentPage, int pageSize, String processName, String processStatus, String displayName, String deptName, String handleName,String name,  String no, String startDate, String endDate, Integer id, String processType, String orderNum, String miji) {
        QueryWrapper<ProcessInstanceData> queryWrapper = new  QueryWrapper<ProcessInstanceData>().eq("org_id",GlobalParam.orgId).orderByDesc("id");
        if (ObjectUtil.isNotEmpty(name)) {//20220920name表示设备名称
            List<Map<String, Object>> listMap = asDeviceCommonService.listMaps(new  QueryWrapper<AsDeviceCommon>().eq("org_id",GlobalParam.orgId).like("name",name).select("id"));
            List<Integer> asIdList = listMap.stream().map(item->Integer.parseInt(item.get("id").toString())).collect(Collectors.toList());
            if(CollUtil.isNotEmpty(asIdList)){
                List<Map<String, Object>> listMap2 = processFormValue2Service.listMaps(new  QueryWrapper<ProcessFormValue2>().eq("org_id",GlobalParam.orgId).in("as_id",asIdList).select("act_process_instance_id"));
                List<Integer> actInstIdList = listMap2.stream().map(item->Integer.parseInt(item.get("act_process_instance_id").toString())).collect(Collectors.toList());
                if(CollUtil.isNotEmpty(actInstIdList))
                      queryWrapper.in("act_process_instance_id",actInstIdList);
                else
                    return null;
            } else
                return null;

        }
        if (ObjectUtil.isNotEmpty(orderNum)) {
            queryWrapper.like("order_num", orderNum);
        }
        if (ObjectUtil.isNotEmpty(id)) {
            queryWrapper.eq("id", id);
        }
        if (ObjectUtil.isNotEmpty(processName)) {
            queryWrapper.like("process_Name", processName);
        }
        if (ObjectUtil.isNotEmpty(processStatus)) {
            queryWrapper.eq("process_status", processStatus);
        }
        if (ObjectUtil.isNotEmpty(processType)) {
            //20230627 为了查询界面的生命周期条件列表的精简：后台再细分
            List<String> processTypeList = new ArrayList<>();
            if(processType.equals("定密")){
                processTypeList.add("定密");
                processTypeList.add("定密及启用");
            } else if(processType.equals("借用归还")){
                processTypeList.add("借用");
                processTypeList.add("归还");
                processTypeList.add("借用归还");
            }
            List<ProcessDefinition> definitionList = null;
            if(CollUtil.isNotEmpty(processTypeList))
                definitionList = processDefinitionService.list(new  QueryWrapper<ProcessDefinition>().eq("org_id",GlobalParam.orgId).in("process_type", processTypeList));
            else
                definitionList = processDefinitionService.list(new  QueryWrapper<ProcessDefinition>().eq("org_id",GlobalParam.orgId).like("process_type", processType));
            //definitionList = processDefinitionService.list(new  QueryWrapper<ProcessDefinition>().eq("org_id",GlobalParam.orgId).in("process_type", processTypeList));
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
            queryWrapper.eq("dept_name", deptName);
        }
        if (ObjectUtil.isNotEmpty(handleName)) {
            List<ProcessInstanceNode> nodeList = processInstanceNodeService.list(new  QueryWrapper<ProcessInstanceNode>().eq("org_id",GlobalParam.orgId).like("display_name", handleName));
            if (ObjectUtil.isNotEmpty(nodeList)) {
                List<Integer> processInstanceIdList = nodeList.stream().map(ProcessInstanceNode::getProcessInstanceDataId).collect(Collectors.toList());
                queryWrapper.in("id", processInstanceIdList);
            } else {
                queryWrapper.in("id", new ArrayList<>());
            }
        }
        if (ObjectUtil.isNotEmpty(no) || ObjectUtil.isNotEmpty(miji)) {
            QueryWrapper<AsDeviceCommon> queryWrapper1 = new  QueryWrapper<AsDeviceCommon>().eq("org_id",GlobalParam.orgId);
            if (ObjectUtil.isNotEmpty(no))
                queryWrapper1.eq("no", no);//20241005 ,like换eq
            if (ObjectUtil.isNotEmpty(miji))
                queryWrapper1.eq("miji", miji);
            List<AsDeviceCommon> asDeviceCommonList = asDeviceCommonService.list(queryWrapper1);
            //组建map1<设备no,asID>
            if (ObjectUtil.isNotEmpty(asDeviceCommonList)) {
                List<Integer> asIdList = asDeviceCommonList.stream().map(AsDeviceCommon::getId).collect(Collectors.toList());
                List<ProcessFormValue2> value2List = processFormValue2Service.list(new  QueryWrapper<ProcessFormValue2>().eq("org_id",GlobalParam.orgId).in("as_id", asIdList));
                //组建map2<actid,asID>
                if (ObjectUtil.isNotEmpty(value2List)) {
                    queryWrapper.in("act_process_instance_id", value2List.stream().map(ProcessFormValue2::getActProcessInstanceId).collect(Collectors.toList()));
                } else
                    queryWrapper.in("act_process_instance_id", new ArrayList<>());
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
        IPage<ProcessInstanceData> page = processInstanceDataService.page(new Page<>(currentPage, pageSize), queryWrapper);
//        List<ProcessInstanceData> list= new ArrayList<>();
//        page.setRecords(list);
        return page;

    }

    //待办任务
    @GetMapping("myList")
    public IPage<ProcessInstanceData> myList(int currentPage, String param, int pageSize, String orderNum, String processName, String processType, String startDate, String no, String miji, String displayName, String deptName) {
        SysUser user = (SysUser) httpSession.getAttribute("user");
        if (user == null) {

            throw new RuntimeException("用户未登录或登陆超时，请关闭本页面，重新从登陆入口进入");
        }

        List<String> loginNameListForOperatorForCross = null;//跨系统待办人员
        QueryWrapper<ProcessInstanceData> queryWrapper = new  QueryWrapper<>();//目前唯一一处不需要加eq("org_id",GlobalParam.orgId)
        if(GlobalParam.orgId == 0) {//只让orgId为0时，即信息化中心来执行；约定了所与惯性公司的ID分别0、1
            SysDic dic_operatorForCross = sysDicService.getOne(new  QueryWrapper<SysDic>().eq("org_id",1).eq("flag", "跨系统待办人员").orderByAsc("sort"));
            if(ObjectUtil.isNotEmpty(dic_operatorForCross))
                loginNameListForOperatorForCross = Arrays.asList(dic_operatorForCross.getName().split(","));//Arrays.asList(operatorTypeIds.split(","))
            if( (ObjectUtil.isNotEmpty(param)  && "chart".contains(param)) ){
                queryWrapper.notLike("login_name", "yutao03");
                queryWrapper.in("org_id", Arrays.asList(new Integer[]{0, 1})).notIn("process_status", Arrays.asList(new String[]{"完成", "中止", "终止",""})).orderByDesc("last_commit_datetime");
                List<SysUser> sysUserList = sysUserService.list(new  QueryWrapper<SysUser>().eq("org_id",0).eq("status","正常").eq("dept_id",GlobalParam.deptIDForXXH).notLike("display_name","审计").notLike("display_name","管理").notLike("display_name","安全").notLike("display_name","系统"));
                if (CollUtil.isNotEmpty(sysUserList)) {
                    queryWrapper.and(wrapper -> {sysUserList.forEach(sysUser -> wrapper.or().like("display_current_step", sysUser.getDisplayName())); });
                }
            } else {
                if(CollUtil.isNotEmpty(loginNameListForOperatorForCross) && loginNameListForOperatorForCross.contains(user.getLoginName())) {//同时要获取org_id为 0、1的待办 //

                  queryWrapper.in("org_id", Arrays.asList(new Integer[]{0, 1})).notIn("process_status", Arrays.asList(new String[]{"完成", "中止", "终止"})).like("display_current_step", user.getDisplayName()).like("login_current_step", user.getLoginName()).orderByDesc("last_commit_datetime");//20241015 倒排主键：id改为last_commit_datetime
                } else
                    queryWrapper.eq("org_id",GlobalParam.orgId).notIn("process_status", Arrays.asList(new String[]{"完成","中止","终止"})).like("display_current_step", user.getDisplayName()).like("login_current_step", user.getLoginName()).orderByDesc("last_commit_datetime");//20241015 倒排主键：id改为last_commit_datetime
            }
            } else
            queryWrapper.eq("org_id",GlobalParam.orgId).notIn("process_status", Arrays.asList(new String[]{"完成","中止","终止"})).like("display_current_step", user.getDisplayName()).like("login_current_step", user.getLoginName()).orderByDesc("last_commit_datetime");//20241015 倒排主键：id改为last_commit_datetime

        if (ObjectUtil.isNotEmpty(processName)) {
            queryWrapper.like("process_name", processName);
        }
        if (ObjectUtil.isNotEmpty(orderNum)) {
            queryWrapper.like("order_num", orderNum);
        }
        if (ObjectUtil.isEmpty(param) || !"chart".contains(param)) {

            if (ObjectUtil.isNotEmpty(displayName)) {
                queryWrapper.eq("display_name", displayName);
            }
        } else{ //用于大屏
            queryWrapper.notLike("display_name", "管理");
            queryWrapper.notLike("display_name", "审计");
            queryWrapper.notLike("display_name", "于涛");
        }

        if (ObjectUtil.isNotEmpty(deptName)) {
            queryWrapper.eq("dept_name", deptName);
        }
        if (ObjectUtil.isNotEmpty(processType)) {
            List<ProcessDefinition> definitionList = processDefinitionService.list(new  QueryWrapper<ProcessDefinition>().eq("org_id",GlobalParam.orgId).eq("process_type", processType));
            if (ObjectUtil.isNotEmpty(definitionList)) {
                queryWrapper.in("process_definition_id", definitionList.stream().map(ProcessDefinition::getId).collect(Collectors.toList()));
            } else {
                queryWrapper.in("process_definition_id", new ArrayList<>());
            }
        }
        if (ObjectUtil.isNotEmpty(no) || ObjectUtil.isNotEmpty(miji)) {
            QueryWrapper<AsDeviceCommon> queryWrapper1 = new  QueryWrapper<AsDeviceCommon>().eq("org_id",GlobalParam.orgId);
            if (ObjectUtil.isNotEmpty(no))
                queryWrapper1.like("no", no);
            if (ObjectUtil.isNotEmpty(miji))
                queryWrapper1.eq("miji", miji);
            List<AsDeviceCommon> asDeviceCommonList = asDeviceCommonService.list(queryWrapper1);
            if (ObjectUtil.isNotEmpty(asDeviceCommonList)) {
                List<Integer> asIdList = asDeviceCommonList.stream().map(AsDeviceCommon::getId).collect(Collectors.toList());
                List<ProcessFormValue2> value2List = processFormValue2Service.list(new  QueryWrapper<ProcessFormValue2>().eq("org_id",GlobalParam.orgId).in("as_id", asIdList));
                if (ObjectUtil.isNotEmpty(value2List)) {
                    queryWrapper.in("act_process_instance_id", value2List.stream().map(ProcessFormValue2::getActProcessInstanceId).collect(Collectors.toList()));
                } else
                    queryWrapper.in("act_process_instance_id", new ArrayList<>());
            } else {
                queryWrapper.in("act_process_instance_id", new ArrayList<>());
            }
        }
        if (ObjectUtil.isNotEmpty(startDate)) {
            String[] dateArr = startDate.split(",");
            queryWrapper.ge("start_datetime", dateArr[0] + " 00:00:00");
            queryWrapper.le("start_datetime", dateArr[1] + " 00:00:00");

        }
        IPage<ProcessInstanceData> page = processInstanceDataService.page(new Page<>(currentPage, pageSize), queryWrapper);
        //再次对loginName进行了过滤：防止liuyang|liuyang02这种重复
        page.setRecords(page.getRecords().stream().filter(item->{
            List<String> list = Arrays.asList(item.getLoginCurrentStep().split(","));
            if (ObjectUtil.isEmpty(param) || !"chart".contains(param)){
                if(list.contains(user.getLoginName()))
                    return true;
                else
                    return  false;
            } else
                return true;
        }).collect(Collectors.toList()));

        return page;
    }

    //用于给传到前端的实例数据LIST中加入score信息
    private void setProcessInstanceDataScore(ProcessInstanceData processInstanceData) {
        //20211124 getone()里要加第二个参数false,这样在查找到多条记录时不会报错
        Score score = scoreService.getOne(new  QueryWrapper<Score>().eq("org_id",GlobalParam.orgId).eq("business_id", processInstanceData.getId()).eq("node_type", 3), false);
        if (score != null) {
            processInstanceData.setScore(score.getScore());
        } else {
            processInstanceData.setScore(0);
        }

    }

   //“新”已办任务：（登陆者）本人处理过的任务
    @GetMapping("handledList")
    public IPage<ProcessInstanceData> handledList(int currentPage, int pageSize, String orderNum, String processName, String processType, String startDate, String endDate, String no, String miji) {
        SysUser user = (SysUser) httpSession.getAttribute("user");
        if (user == null) {
            throw new RuntimeException("用户未登录或登陆超时，请关闭本页面，重新从登陆入口进入");
        }
        QueryWrapper<ProcessInstanceData> queryWrapper = new  QueryWrapper<ProcessInstanceData>().eq("org_id",GlobalParam.orgId).eq("org_id",GlobalParam.orgId).orderByDesc("id");
        //根据node表取出当前登陆者处理过的流程idList（包含提交的）
        QueryWrapper<ProcessInstanceNode> queryWrapper1 = new  QueryWrapper<ProcessInstanceNode>().eq("org_id",GlobalParam.orgId).eq("org_id",GlobalParam.orgId);
        if (ObjectUtil.isNotEmpty(startDate)) {
            String[] dateArr = startDate.split(",");
            queryWrapper1.ge("start_datetime", dateArr[0] + " 00:00:00");
            queryWrapper1.le("start_datetime", dateArr[1] + " 00:00:00");

        }
        if (ObjectUtil.isNotEmpty(endDate)) {
            String[] dateArr = endDate.split(",");
            queryWrapper1.ge("end_datetime", dateArr[0] + " 00:00:00");
            queryWrapper1.le("end_datetime", dateArr[1] + " 00:00:00");

        }
        List<Map<String,Object>> listMaps = processInstanceNodeService.listMaps(queryWrapper1.eq("login_name",user.getLoginName()).select("process_instance_data_id,start_datetime,end_datetime,task_name"));
        Map<Integer, LocalDateTime[]> mapForTime = new HashMap<>();
        Map<Integer, String> mapForNodeName = new HashMap<>();
        if(CollUtil.isNotEmpty(listMaps)){
            List<Integer> idListFiltered  = new ArrayList<>();
            List<Integer> idList = listMaps.stream().map(item->{
                LocalDateTime[] times = new LocalDateTime[2];
                //20230715 localDate类型不能直接用于强制转换：故先转成Timestamp
                times[0] = ((Timestamp)item.get("start_datetime")).toLocalDateTime();
                times[1] = ((Timestamp)item.get("end_datetime")).toLocalDateTime();
                if(!times[0].equals(times[1])) {//排除start节点
                    mapForTime.put(Integer.valueOf(item.get("process_instance_data_id").toString()), times);
                    mapForNodeName.put(Integer.valueOf(item.get("process_instance_data_id").toString()),item.get("task_name").toString());
                    idListFiltered.add(Integer.valueOf(item.get("process_instance_data_id").toString()));

                }
                return Integer.valueOf(item.get("process_instance_data_id").toString());
            }).collect(Collectors.toList());
            if(CollUtil.isNotEmpty(idListFiltered)){
                queryWrapper.in("id",idListFiltered);
            }
        }

        if (ObjectUtil.isNotEmpty(processName)) {
            queryWrapper.like("process_name", processName);
        }
        if (ObjectUtil.isNotEmpty(orderNum)) {
            queryWrapper.like("order_num",orderNum);
        }
        if (ObjectUtil.isNotEmpty(processType)) {
            List<ProcessDefinition> definitionList = processDefinitionService.list(new  QueryWrapper<ProcessDefinition>().eq("org_id",GlobalParam.orgId).eq("process_type", processType));
            if (ObjectUtil.isNotEmpty(definitionList)) {
                queryWrapper.in("process_definition_id", definitionList.stream().map(ProcessDefinition::getId).collect(Collectors.toList()));
            } else {
                queryWrapper.in("process_definition_id", new ArrayList<>());
            }
        }

        if (ObjectUtil.isNotEmpty(no) || ObjectUtil.isNotEmpty(miji)) {
            QueryWrapper<AsDeviceCommon> queryWrapper2 = new  QueryWrapper<AsDeviceCommon>().eq("org_id",GlobalParam.orgId);
            if (ObjectUtil.isNotEmpty(no))
                queryWrapper2.like("no", no);
            if (ObjectUtil.isNotEmpty(miji))
                queryWrapper2.eq("miji", miji);
            List<AsDeviceCommon> asDeviceCommonList = asDeviceCommonService.list(queryWrapper2);
            if (ObjectUtil.isNotEmpty(asDeviceCommonList)) {
                List<Integer> asIdList = asDeviceCommonList.stream().map(AsDeviceCommon::getId).collect(Collectors.toList());
                List<ProcessFormValue2> value2List = processFormValue2Service.list(new  QueryWrapper<ProcessFormValue2>().eq("org_id",GlobalParam.orgId).in("as_id", asIdList));
                if (ObjectUtil.isNotEmpty(value2List)) {
                    queryWrapper.in("act_process_instance_id", value2List.stream().map(ProcessFormValue2::getActProcessInstanceId).collect(Collectors.toList()));
                } else
                    queryWrapper.in("act_process_instance_id", new ArrayList<>());
            } else {
                queryWrapper.in("act_process_instance_id", new ArrayList<>());
            }
        }
        //遍历page并加入score信息
        IPage<ProcessInstanceData> page = processInstanceDataService.page(new Page<>(currentPage, pageSize), queryWrapper);
        List<ProcessInstanceData> list = page.getRecords();
        for (ProcessInstanceData processInstanceData : list) {
            processInstanceData.setStartDatetime(mapForTime.get(processInstanceData.getId())[0]);
            processInstanceData.setEndDatetime(mapForTime.get(processInstanceData.getId())[1]);
            processInstanceData.setProcessStatus(mapForNodeName.get(processInstanceData.getId()));
            this.setProcessInstanceDataScore(processInstanceData);
        }
        return page;
    }

    //已办任务
    //20220719回头改:这个loginNmae/displayName矛盾
    @GetMapping("completeList")
    public IPage<ProcessInstanceData> completeList(int currentPage, int pageSize, String processName, String processType, String handleName, String no, String startDate, String endDate) {
        SysUser user = (SysUser) httpSession.getAttribute("user");
        if (user == null) {
            throw new RuntimeException("用户未登录或登陆超时，请关闭本页面，重新从登陆入口进入");
        }
        QueryWrapper<ProcessInstanceData> queryWrapper = new  QueryWrapper<ProcessInstanceData>().eq("org_id",GlobalParam.orgId).eq("process_status", "完成").eq("login_name", user.getLoginName()).orderByDesc("id");

        if (ObjectUtil.isNotEmpty(processName)) {
            queryWrapper.like("process_name", processName);
        }
        if (ObjectUtil.isNotEmpty(processType)) {
            List<ProcessDefinition> definitionList = processDefinitionService.list(new  QueryWrapper<ProcessDefinition>().eq("org_id",GlobalParam.orgId).eq("process_type", processType));
            if (ObjectUtil.isNotEmpty(definitionList)) {
                queryWrapper.in("process_definition_id", definitionList.stream().map(ProcessDefinition::getId).collect(Collectors.toList()));
            } else {
                queryWrapper.in("process_definition_id", new ArrayList<>());
            }
        }
        if (ObjectUtil.isNotEmpty(handleName)) {
            List<ProcessInstanceNode> nodeList = processInstanceNodeService.list(new  QueryWrapper<ProcessInstanceNode>().eq("org_id",GlobalParam.orgId).like("display_name", handleName));
            if (ObjectUtil.isNotEmpty(nodeList)) {
                List<Integer> processInstanceIdList = nodeList.stream().map(ProcessInstanceNode::getProcessInstanceDataId).collect(Collectors.toList());
                queryWrapper.in("id", processInstanceIdList);
            } else {
                queryWrapper.in("id", new ArrayList<>());
            }
        }
        if (ObjectUtil.isNotEmpty(no)) {
            List<AsDeviceCommon> asDeviceCommonList = asDeviceCommonService.list(new  QueryWrapper<AsDeviceCommon>().eq("org_id",GlobalParam.orgId).like("no", no));
            if (ObjectUtil.isNotEmpty(asDeviceCommonList)) {
                List<Integer> asIdList = asDeviceCommonList.stream().map(AsDeviceCommon::getId).collect(Collectors.toList());
                List<ProcessFormValue2> value2List = processFormValue2Service.list(new  QueryWrapper<ProcessFormValue2>().eq("org_id",GlobalParam.orgId).in("as_id", asIdList));
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
        List<ProcessFormValue2> value2List = processFormValue2Service.list(new  QueryWrapper<ProcessFormValue2>().eq("org_id",GlobalParam.orgId).eq("as_id", asId));
        if (ObjectUtil.isNotEmpty(value2List)) {
            List<String> list = value2List.stream().map(ProcessFormValue2::getActProcessInstanceId).collect(Collectors.toList());
            return processInstanceDataService.page(new Page<>(currentPage, pageSize), new  QueryWrapper<ProcessInstanceData>().eq("org_id",GlobalParam.orgId).ne("process_status", "完成").in("act_process_instance_id", list).orderByDesc("start_datetime"));
        }
        return null;
    }

    //历史工单
    @GetMapping("historyList")
    public IPage<ProcessInstanceData> historyList(int currentPage, int pageSize, int asId) {
        //
        List<ProcessFormValue2> value2List = processFormValue2Service.list(new  QueryWrapper<ProcessFormValue2>().eq("org_id",GlobalParam.orgId).eq("as_id", asId));
        if (ObjectUtil.isNotEmpty(value2List)) {
            List<String> list = value2List.stream().map(ProcessFormValue2::getActProcessInstanceId).collect(Collectors.toList());
            return processInstanceDataService.page(new Page<>(currentPage, pageSize), new  QueryWrapper<ProcessInstanceData>().eq("org_id",GlobalParam.orgId).eq("process_status", "完成").in("act_process_instance_id", list).orderByDesc("start_datetime"));
        }
        return null;
    }
    //20220806已提工单
    @GetMapping("listForCommitted")
    public IPage<ProcessInstanceData> listForCommitted(int currentPage, int pageSize) {
        SysUser user = (SysUser) httpSession.getAttribute("user");
        if (user == null) {
            throw new RuntimeException("用户未登录或登陆超时，请关闭本页面，重新从登陆入口进入");
        }
        //遍历page并加入score信息
        IPage<ProcessInstanceData> page =  processInstanceDataService.page(new Page<>(currentPage, pageSize), new  QueryWrapper<ProcessInstanceData>().eq("org_id",GlobalParam.orgId).eq("login_name", user.getLoginName()).orderByDesc("id"));
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
            throw new RuntimeException("用户未登录或登陆超时，请关闭本页面，重新从登陆入口进入");
        }
        if (ObjectUtil.isEmpty(value1)) {
            return null;
        }
        int processDefId = value1.getProcessDefinitionId();
        ProcessDefinition proDef = processDefinitionService.getById(processDefId);
//        if (proDef.getProcessName().contains("故障报修")) {//202208034 故障报修流程不判断validate
//            return null;
//        }
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
        //查询资产类型表中typeId对应的level=1/上级分类的名称是不是“信息与存储设备”(id==1)，不是的话，直接放行
        int p_typeId = asTypeService.getOne(new  QueryWrapper<AsType>().eq("org_id",GlobalParam.orgId).eq("id", typeId)).getPid();
        int p_p_typeId = asTypeService.getOne(new  QueryWrapper<AsType>().eq("org_id",GlobalParam.orgId).eq("id", p_typeId)).getPid();
        //20220919 添加p_p_typeId == 0：用于满足“应用系统”的情况:设备类别的level=2,即p_p_typeId == 0;
        if (p_p_typeId != GlobalParam.devRootID)//20230719暂改为p_p_typeId != 1
            return null;

        //获取资产ID对应的所有ACTINST IDList
        List<Map<String, Object>> actProcessInsIdListMap = null;
        if (ObjectUtil.isEmpty(processInstanceDataId))
            actProcessInsIdListMap = processFormValue2Service.listMaps(new  QueryWrapper<ProcessFormValue2>().eq("org_id",GlobalParam.orgId).eq("as_id", assetId).select("act_process_instance_id"));
        else {
            actProcessInsIdListMap = processFormValue2Service.listMaps(new  QueryWrapper<ProcessFormValue2>().eq("org_id",GlobalParam.orgId).eq("as_id", assetId).ne("act_process_instance_id", processInstanceDataService.getById(processInstanceDataId).getActProcessInstanceId()).select("act_process_instance_id"));
        }
        //判断  actProcessInsIdListMap是不是空。如果是空的，那么不用再校验了--放行
        if (ObjectUtil.isEmpty(actProcessInsIdListMap)) return null;
        List<String> actProcessInsIdList = actProcessInsIdListMap.stream().map(item -> item.get("act_process_instance_id").toString()).collect(Collectors.toList());
        //查出互斥定义的定义IDlist
        List<Map<String, Object>> mutexDefIdListMap;//不可能为空，肯定有值
        QueryWrapper<ProcessDefinition> queryWrapper = new  QueryWrapper<ProcessDefinition>().eq("org_id",GlobalParam.orgId).eq("as_type_id", mainTypeIdForDef);
        ;
        if (processType.contains("申领") || processType.contains("归库")) {//20221225增加“报废”：“报废”是授权发起，没必要做判断：仅判断“别的流程发起时，能不能有他”
            mutexDefIdListMap = processDefinitionService.listMaps(queryWrapper.like("process_type", "申领").or().like("process_type", "归库").or().like("process_type", "报废").select("id"));
        } else {//查出同类型流程
            /*20220801 同类型流程要排除“故障报修”：因为故障报修流程与被授权的流程类型都是“维修”：在“发起新流程提交时&&老流程并没有关闭”
            * 20230829 把“三合一流程”与“策略变更”的互斥取消*/
            if(proDef.getProcessName().contains("策略变更"))
                mutexDefIdListMap = processDefinitionService.listMaps(queryWrapper.like("process_type", processType).notLike("process_name","三合一").select("id"));
            else if(proDef.getProcessName().contains("三合一"))
                mutexDefIdListMap = processDefinitionService.listMaps(queryWrapper.like("process_type", processType).notLike("process_name","策略变更").select("id"));
//            else if(proDef.getProcessName().contains("借用"))//20231228 暂用于便携机借用，不过没有实质意义：毕竟在“选择资产”时就提前屏敝了
//                mutexDefIdListMap = processDefinitionService.listMaps(queryWrapper.like("process_type", processType).or().like("process_type", "归还").select("id"));
            else if(proDef.getProcessName().contains("维修") || proDef.getProcessName().contains("定密"))//20250726 定密时，不能同时换硬盘；返过来也不行
                mutexDefIdListMap = processDefinitionService.listMaps(queryWrapper.like("process_type", "定密").or().like("process_type", "维修").notLike("process_name","故障报修").select("id"));
            else
                mutexDefIdListMap = processDefinitionService.listMaps(queryWrapper.like("process_type", processType).select("id"));
        }
        List<Integer> mutexDefIdList = mutexDefIdListMap.stream().map(item -> Integer.valueOf(item.get("id").toString())).collect(Collectors.toList());
        //取出互斥的业务实例表
        List<ProcessInstanceData> processInstanceDataList = processInstanceDataService.list(new  QueryWrapper<ProcessInstanceData>().eq("org_id",GlobalParam.orgId).in("process_definition_id", mutexDefIdList).notIn("process_status", Arrays.asList("完成","中止","终止")).in("act_process_instance_id", actProcessInsIdList));
        return processInstanceDataList;
    }

    @GetMapping("getOneDeviceByProcessInstId")
//20220702加,注意逻辑是根据流程类型中的“资产类型”ID，来查找对应的设备：约定同一个资产类型只有只能选择一个资产：这条需要单独记录在一个地方
    public AsDeviceCommon getOneDeviceByProcessInstId(Integer processInstanceDataId) {
        ProcessInstanceData processInstanceData = processInstanceDataService.getById(processInstanceDataId);
        List<Integer> assetIdList = processFormValue2Service.list(new  QueryWrapper<ProcessFormValue2>().eq("org_id",GlobalParam.orgId).eq("act_process_instance_id", processInstanceData.getActProcessInstanceId())).stream().map(item -> item.getAsId()).collect(Collectors.toList());
        if (CollUtil.isNotEmpty(assetIdList))
            return asDeviceCommonService.getById(assetIdList.get(0));//审批型流程的前置流程：约定：只能选择一个资产
        return null;

    }

    @GetMapping("getNewProcessDef")//20220702加
    public ProcessDefinition getNewProcessDef(Integer processInstanceDataId) {
        ProcessInstanceData processInstanceData = processInstanceDataService.getById(processInstanceDataId);
        String actProcessInstanceId = processInstanceData.getActProcessInstanceId();
        Object nextProcessNameObj = null;
        if(ObjectUtil.isNotEmpty(actProcessInstanceId)){
            List<Task> myTaskList = workFlowBean.getMyTask(actProcessInstanceId);
            Task myTask = myTaskList.get(0);//20250602 这里只适合之前把后续流程写入流程变量的作法
            nextProcessNameObj = workFlowBean.getProcessVariable(myTask);
        } else
        // ProcessDefinition nextProcess  = processDefinitionService.getOne(new  QueryWrapper<ProcessDefinition>().eq("org_id",GlobalParam.orgId).eq("status","启用").eq("have_display","是").eq("process_name",(String)nextProcessNameObj));
            nextProcessNameObj = "信息设备自查结果填报流程";//20250602有时间优化下机制
        return processDefinitionService.getOne(new  QueryWrapper<ProcessDefinition>().eq("org_id",GlobalParam.orgId).eq("status", "启用").eq("have_display", "是").eq("process_name", (String) nextProcessNameObj));
        //return new StartOrHandleProcessResultVO();

    }
 //这个应该不用了
    @GetMapping("getOperateRecordForRepair")//20220829加:专门用于故障报修流程：该 流程只有一个处理节点
    public  ResponseResult getOperateRecordForRepair(Integer processInstanceDataId) {
        RecordForReport recordForReport = recordForReportService.list(new  QueryWrapper<RecordForReport>().eq("org_id",GlobalParam.orgId).eq("process_instance_data_id",processInstanceDataId).eq("name","故障描述")).get(0);
       // ProcessInstanceNode node = processInstanceNodeService.list(new  QueryWrapper<ProcessInstanceNode>().eq("org_id",GlobalParam.orgId).eq("process_instance_data_id",processInstanceDataId).like("task_name","处理")).get(0);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
       // String dateStr = node.getEndDatetime().format(fmt);
       // return ResponseResult.success(node.getComment()+"   " +recordForReport.getValue()+" "+ dateStr);
        return ResponseResult.success(recordForReport.getValue());
    }
    @GetMapping("getOldProValueForRepair")//20230223加:专门用于故障报修流程：该 流程只有一个处理节点
    public  Map getOldProValueForRepair(Integer processInstanceDataId) {
        System.out.println(processInstanceDataId);
        ProcessInstanceNode node = processInstanceNodeService.list(new  QueryWrapper<ProcessInstanceNode>().eq("org_id",GlobalParam.orgId).eq("process_instance_data_id",processInstanceDataId).like("task_name","处理").or().like("task_name","判断")).get(0);//20250730 惯性报修流程中处理节点名里改为“判断”
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String dateStr = node.getEndDatetime().format(fmt);
        String problemDec = "";
        Map<String, String> map = new HashMap<>();
        List<RecordForReport> list = recordForReportService.list(new  QueryWrapper<RecordForReport>().eq("org_id",GlobalParam.orgId).eq("process_instance_data_id",processInstanceDataId));
        if(CollUtil.isNotEmpty(list)){
            RecordForReport recordForReport = list.get(0);
            problemDec = recordForReport.getValue();

        }
        map.put("problemDec",problemDec);
        map.put("comment",node.getComment()+"   " +node.getDisplayName()+" "+ dateStr);
        return map;
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

//    @OperateLog(module = "流程模块", type = "代发起流程")
    @PostMapping("autoStart")//20250602
    public StartOrHandleProcessResultVO autoStart(MultipartFile[] files,  String aaa, String fujianMiji) {
        EndAndStartProcessVO endAndStartProcessVO = JSON.parseObject(aaa, EndAndStartProcessVO.class);
        StartOrHandleProcessResultVO startOrHandleProcessResultVO = new StartOrHandleProcessResultVO();
        List<ProcessInstanceData> processInstanceDataList = this.validate(endAndStartProcessVO.getValue1(), endAndStartProcessVO.getValue2List(), null);
        if (ObjectUtil.isNotEmpty(processInstanceDataList)) {//有值即存在互斥实例
            startOrHandleProcessResultVO.setProcessInstanceDataList(processInstanceDataList);
            startOrHandleProcessResultVO.setIsSuccess(false);
        } else {
            Integer id = endAndStartProcessVO.getValue1().getProcessDefinitionId();
            ProcessDefinition processDefinition = processDefinitionService.getById(id);
            saveLog("start",processDefinition.getProcessName());
            startOrHandleProcessResultVO.setIsSuccess(processInstanceDataService.autoStart(files,endAndStartProcessVO, fujianMiji));
        }
        return startOrHandleProcessResultVO;
    }

//    @OperateLog(module = "流程模块", type = "发起流程")
    @PostMapping("start")//20211112重写
    @SneakyThrows//20240122; 注意总结, MultipartFile[] files && 前端用“formData”传参时，第二个参数也不能用@RequestBody修饰 了：todo研
    public StartOrHandleProcessResultVO start( MultipartFile[] files, String aaa,String fujianMiji) {

        StartProcessVO startProcessVO = JSON.parseObject(aaa, StartProcessVO.class);
        StartOrHandleProcessResultVO startOrHandleProcessResultVO = new StartOrHandleProcessResultVO();
        List<ProcessInstanceData> processInstanceDataList = this.validate(startProcessVO.getValue1(), startProcessVO.getValue2List(), null);
        if (ObjectUtil.isNotEmpty(processInstanceDataList)) {//有值即存在互斥实例
            startOrHandleProcessResultVO.setProcessInstanceDataList(processInstanceDataList);
            startOrHandleProcessResultVO.setIsSuccess(false);
        } else {
            Integer id = startProcessVO.getValue1().getProcessDefinitionId();
            ProcessDefinition processDefinition = processDefinitionService.getById(id);
            saveLog("start",processDefinition.getProcessName());
            startOrHandleProcessResultVO.setIsSuccess(processInstanceDataService.start(files,startProcessVO,fujianMiji));
        }
        return startOrHandleProcessResultVO;
    }

//    @PostMapping("start2")//备份原版，改为start2 188811111111111111
//    public boolean start2(@RequestBody StartProcessVO startProcessVO) {
//        SysUser user = (SysUser) httpSession.getAttribute("user");
//        System.out.println(startProcessVO.getValue2List().get(0).getAsId());
//        if (user == null) {
//            throw new RuntimeException("用户未登录或登陆超时，请关闭本页面，重新从登陆入口进入");
//        }
//        return processInstanceDataService.start(startProcessVO);
//    }

//    @OperateLog(module = "流程模块", type = "处理流程")
    @PostMapping("handle")
    //返回值改造成自定义VO  555
    public StartOrHandleProcessResultVO handle(@RequestBody CheckProcessVO checkProcessVO) {
        Integer orgId = GlobalParam.orgId;
        if(ObjectUtil.isNotEmpty(checkProcessVO.getOrgId())) {//20241106 流程的前端发来的机构D参数标记
             orgId = checkProcessVO.getOrgId();
             if(orgId != GlobalParam.orgId)//只有不相等时，才写入session
                httpSession.setAttribute("crossOrgId",orgId);
        }
        SysUser user = (SysUser) httpSession.getAttribute("user");
        if (user == null) {
            throw new RuntimeException("用户未登录或登陆超时，请关闭本页面，重新从登陆入口进入");
        }
    //    return null;
        StartOrHandleProcessResultVO startOrHandleProcessResultVO = new StartOrHandleProcessResultVO();
        List<ProcessInstanceData> processInstanceDataList = null;
        if(ObjectUtil.isEmpty(httpSession.getAttribute("crossOrgId")))//如果不是穿透流程才判断互斥
            processInstanceDataList = this.validate(checkProcessVO.getValue1(), checkProcessVO.getValue2List(), checkProcessVO.getProcessInstanceDataId());
        if (ObjectUtil.isNotEmpty(processInstanceDataList)) {//有值即存在互斥实例
            startOrHandleProcessResultVO.setProcessInstanceDataList(processInstanceDataList);
            startOrHandleProcessResultVO.setIsSuccess(false);
        } else {
            ProcessInstanceData processInstanceData = processInstanceDataService.getById(checkProcessVO.getProcessInstanceDataId());
            saveLog("handle", processInstanceData.getProcessName() + "," + processInstanceData.getId());
            startOrHandleProcessResultVO.setIsSuccess(processInstanceDataService.handle(checkProcessVO));
        }
        httpSession.removeAttribute("crossOrgId");//20241108 关闭跨流程标识
        return startOrHandleProcessResultVO;
    }


    @PostMapping("modify")
    public boolean modify(@RequestBody ModifyProcessFormVO modifyProcessFormVO) {
        return processInstanceDataService.modifyProcessForm(modifyProcessFormVO);
    }


    @GetMapping("get")
    public ProcessInstanceData getById(String id) {//2020313 添加isForHandle参数
     //   ProcessInstanceData processInstanceData = ProcessInstanceDataService.getOn
        return processInstanceDataService.getById(id);
    }

//    @GetMapping("getForSSO")//20230509
//    public ProcessInstanceData getByIdForSSO(String id) {
//        //   ProcessInstanceData processInstanceData = ProcessInstanceDataService.getOn
//        return processInstanceDataService.getById(id);
//    }

    @OperateLog(module = "流程模块", type = "删除流程")
    @PostMapping("terminate")
    public boolean terminate(@RequestBody ProcessInstanceData processInstanceData) {
        return processInstanceDataService.terminate(processInstanceData);
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
        ProcessDefinitionTask startEvent = processDefinitionTaskService.getOne(new  QueryWrapper<ProcessDefinitionTask>().eq("org_id",GlobalParam.orgId).eq("process_definition_id", processDefinitionId).eq("task_type", "bpmn:startEvent"));
        List<ProcessDefinitionEdge> edgeList;
        //20220517 加判空
        if (ObjectUtil.isNotEmpty(startEvent)) {
            //20220608感觉可以用getOne:毕竟startEvent到StartTask只有一条线）
            edgeList = processDefinitionEdgeService.list(new  QueryWrapper<ProcessDefinitionEdge>().eq("org_id", GlobalParam.orgId).eq("process_definition_id", processDefinitionId).eq("source_id", startEvent.getTaskDefKey()));
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
            ProcessDefinitionTask startTask = processDefinitionTaskService.getOne(new  QueryWrapper<ProcessDefinitionTask>().eq("org_id",GlobalParam.orgId).eq("process_definition_id", processDefinitionId).eq("task_def_key", startTaskDefKey));
            BeanUtils.copyProperties(startTask, startProcessConditionVO);//20220828
//            startProcessConditionVO.setHaveNextUser(startTask.getHaveNextUser());
//            startProcessConditionVO.setHideGroupIds(startTask.getHideGroupIds());
//            startProcessConditionVO.setHideGroupLabel(startTask.getHideGroupLabel());
//            startProcessConditionVO.setHaveSelectAsset(startTask.getHaveSelectAsset());//20220724加
        }

        return startProcessConditionVO;
    }

    @GetMapping("getCheckTaskVO")//20221103 把这个方法实体写在service同名方法中，方便template调用他
    public CheckTaskVO getCheckTaskVO(Integer processDefinitionId, String actProcessInstanceId) {
        return processInstanceDataService.getCheckTaskVO(processDefinitionId, actProcessInstanceId);
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

    /**小U
     * 导出流程实例Excel（包含ProcessFormValue1.value的JSON解析字段）
     */
    @GetMapping("exportExcel")
    public void exportExcel(
            String processName,
            String processStatus,
            String displayName,
            String deptName,
            String orderNum,
            String startDate,
            String endDate,
            HttpServletRequest request,
            HttpServletResponse response) {

        // 1. 构建查询条件，逻辑与list接口一致
        QueryWrapper<ProcessInstanceData> queryWrapper = new QueryWrapper<ProcessInstanceData>()
                .eq("org_id", GlobalParam.orgId)
                .orderByDesc("id");

        if (StrUtil.isNotBlank(processName)) {
            queryWrapper.like("process_name", processName);
        }
        if (StrUtil.isNotBlank(processStatus)) {
            queryWrapper.eq("process_status", processStatus);
        }
        if (StrUtil.isNotBlank(displayName)) {
            queryWrapper.eq("display_name", displayName);
        }
        if (StrUtil.isNotBlank(deptName)) {
            queryWrapper.eq("dept_name", deptName);
        }
        if (StrUtil.isNotBlank(orderNum)) {
            queryWrapper.like("order_num", orderNum);
        }
        if (StrUtil.isNotBlank(startDate)) {
            String[] dateArr = startDate.split(",");
            queryWrapper.ge("start_datetime", dateArr[0] + " 00:00:00");
            queryWrapper.le("start_datetime", dateArr[1] + " 00:00:00");
        }
        if (StrUtil.isNotBlank(endDate)) {
            String[] dateArr = endDate.split(",");
            queryWrapper.ge("end_datetime", dateArr[0] + " 00:00:00");
            queryWrapper.le("end_datetime", dateArr[1] + " 00:00:00");
        }

        // 2. 查询所有符合条件的流程实例（不分页）
        List<ProcessInstanceData> instanceList = processInstanceDataService.list(queryWrapper);

        // 3. 填充score和JSON字段
        List<ProcessInstanceExcel> excelList = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        for (ProcessInstanceData instance : instanceList) {
            ProcessInstanceExcel excel = new ProcessInstanceExcel();
            excel.setProcessName(instance.getProcessName());
            excel.setProcessStatus(instance.getProcessStatus());
            excel.setDisplayCurrentStep(instance.getDisplayCurrentStep());
            excel.setOrderNum(instance.getOrderNum());
            excel.setDisplayName(instance.getDisplayName());
            excel.setDeptName(instance.getDeptName());
            excel.setStartDatetime(instance.getStartDatetime() != null ? instance.getStartDatetime().format(fmt) : "");
            excel.setEndDatetime(instance.getEndDatetime() != null ? instance.getEndDatetime().format(fmt) : "");

            // 填充评分
            Score score = scoreService.getOne(
                    new QueryWrapper<Score>()
                            .eq("org_id", GlobalParam.orgId)
                            .eq("business_id", instance.getId())
                            .eq("node_type", 3), false);
            excel.setScore(score != null ? score.getScore() : 0);

            // 解析ProcessFormValue1中的JSON字段
            ProcessFormValue1 value1 = processFormValue1Service.getOne(
                    new QueryWrapper<ProcessFormValue1>()
                            .eq("org_id", GlobalParam.orgId)
                            .eq("act_process_instance_id", instance.getActProcessInstanceId()));
            if (value1 != null && StrUtil.isNotBlank(value1.getValue())) {
                excel.setJsonValue(value1.getValue());
                // 将JSON解析后放入extraFields，前端动态渲染列头
                try {
                    Map<String, Object> jsonMap = JSON.parseObject(value1.getValue(), Map.class);
                    if (jsonMap != null) {
                        Map<String, String> extraFields = new java.util.LinkedHashMap<>();
                        for (Map.Entry<String, Object> entry : jsonMap.entrySet()) {
                            extraFields.put(entry.getKey(), entry.getValue() == null ? "" : entry.getValue().toString());
                        }
                        excel.setExtraFields(extraFields);
                    }
                } catch (Exception e) {
                    // JSON解析失败，忽略
                }
            }

            excelList.add(excel);
        }

        // 4. 动态生成列：收集所有JSON key作为表头
        List<String> jsonKeys = new ArrayList<>();
        for (ProcessInstanceExcel excel : excelList) {
            if (excel.getExtraFields() != null) {
                for (String key : excel.getExtraFields().keySet()) {
                    if (!jsonKeys.contains(key)) {
                        jsonKeys.add(key);
                    }
                }
            }
        }

        // 5. 写入响应
        try {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("utf-8");
            String fileName = java.net.URLEncoder.encode("流程实例导出", "UTF-8").replaceAll("\\+", "%20");
            response.setHeader("Content-disposition", "attachment;filename*=UTF-8''" + fileName + ".xlsx");

            // 扁平化数据：每行 = 固定列 + 动态JSON列
            List<List<Object>> dataList = new ArrayList<>();
            for (ProcessInstanceExcel excel : excelList) {
                List<Object> row = new ArrayList<>();
                row.add(nvl(excel.getProcessName()));
                row.add(nvl(excel.getProcessStatus()));
                row.add(nvl(excel.getDisplayCurrentStep()));
                row.add(nvl(excel.getOrderNum()));
                row.add(nvl(excel.getDisplayName()));
                row.add(nvl(excel.getDeptName()));
                row.add(nvl(excel.getStartDatetime()));
                row.add(nvl(excel.getEndDatetime()));
                row.add(excel.getScore() != null ? excel.getScore() : 0);
                // 动态JSON列
                if (excel.getExtraFields() != null) {
                    for (String key : jsonKeys) {
                        row.add(nvl(excel.getExtraFields().get(key)));
                    }
                } else {
                    for (String key : jsonKeys) {
                        row.add("");
                    }
                }
                dataList.add(row);
            }

            // 使用EasyExcel动态写入
            com.alibaba.excel.EasyExcel.write(response.getOutputStream())
                    .head(generateHead(jsonKeys))
                    .sheet("流程实例")
                    .doWrite(dataList);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** 空值处理 */
    private String nvl(Object obj) {
        return obj == null ? "" : obj.toString();
    }

    /**
     * 生成动态表头
     * @param jsonKeys JSON解析出的key列表
     * @return 表头List<List<String>>
     */
    private List<List<String>> generateHead(List<String> jsonKeys) {
        List<List<String>> head = new ArrayList<>();

        // 固定列：流程名称
        head.add(Arrays.asList("流程名称"));
        head.add(Arrays.asList("流程状态"));
        head.add(Arrays.asList("当前步骤"));
        head.add(Arrays.asList("工单编号"));
        head.add(Arrays.asList("提交人"));
        head.add(Arrays.asList("提交部门"));
        head.add(Arrays.asList("提交时间"));
        head.add(Arrays.asList("结束时间"));
        head.add(Arrays.asList("评分"));

        // 动态JSON列
        for (String key : jsonKeys) {
            head.add(Arrays.asList(key));
        }
        return head;
    }
}
