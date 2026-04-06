package com.sss.yunweiadmin.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sss.yunweiadmin.common.config.GlobalParam;
import com.sss.yunweiadmin.common.utils.ExcelDateUtil;
import com.sss.yunweiadmin.model.entity.*;
import com.sss.yunweiadmin.model.entity.Inspection;
import com.sss.yunweiadmin.mapper.InspectionMapper;
import com.sss.yunweiadmin.model.excel.AsComputerExcel;
import com.sss.yunweiadmin.model.excel.InspectionExcel;
import com.sss.yunweiadmin.service.*;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 任勇林
 * @since 2024-06-23
 */
@Service
public class InspectionServiceImpl extends ServiceImpl<InspectionMapper, Inspection> implements InspectionService {
    @Autowired
    private AsDeviceCommonService asDeviceCommonService;
    @Autowired
    HttpSession httpSession;
    @Autowired
    SysDeptService sysDeptService;
    @Autowired
    ProcessInstanceDataService processInstanceDataService;
    @Autowired
    SysUserService sysUserService;
    @Autowired
    SysRoleUserService sysRoleUserService;



    @Override
    public boolean improve(Integer[] idArr){
        //20250617todo 改造成给所有保密员发送
        Integer orgId = GlobalParam.orgId;


        List<Integer> userIdListForCollaborator = sysRoleUserService.list(new  QueryWrapper<SysRoleUser>().eq("org_id",orgId).eq("role_id", GlobalParam.roleIdForAssist)).stream().map(item -> item.getUserId()).collect(Collectors.toList());
        List<SysUser> sysUserList = sysUserService.list(new  QueryWrapper<SysUser>().eq("org_id",orgId).eq("status","正常").in("id",userIdListForCollaborator));
        List<ProcessInstanceData> processInstanceDataList = new ArrayList<>();

        LocalDate today = LocalDate.now(); // 获取当前日期
        int month = today.getMonthValue();
        Map<Integer, Integer> map = new HashMap<>();//20220625测流程变量
        sysUserList.stream().forEach(item->{
            if(ObjectUtil.isEmpty(map.get(item.getDeptId()))){//协管员只选一个人
                map.put(item.getDeptId(),item.getDeptId());
                SysDept sysDept = sysDeptService.getById(item.getDeptId());
                if(ObjectUtil.isNotEmpty(sysDept)){
                    ProcessInstanceData processInstanceData =  new ProcessInstanceData();
                    //SysUser user = (SysUser) httpSession.getAttribute("user");
                    //SysDept dept = sysDeptService.getById(user.getDeptId());
                    processInstanceData.setProcessName("信息设备自查任务"+"("+month+ "月)");
                    processInstanceData.setDisplayName("自动");
                    processInstanceData.setLoginName(item.getLoginName());
                    processInstanceData.setOrderNum(processInstanceDataService.getTimeAndRandomNum());
                    processInstanceData.setDeptName(sysDept.getName());
                    processInstanceData.setStartDatetime(LocalDateTime.now());
                    processInstanceData.setProcessStatus("定时任务");
                    processInstanceData.setDisplayCurrentStep(item.getDisplayName());
                    processInstanceData.setLoginCurrentStep(item.getLoginName());
                    LocalDateTime localDateTime = LocalDateTime.now();
                    processInstanceData.setLastCommitDatetime(localDateTime);//20241015
                    processInstanceDataList.add(processInstanceData);
                }

            }
        });


//        ProcessInstanceData processInstanceData =  new ProcessInstanceData();
//        LocalDate today = LocalDate.now(); // 获取当前日期
//        int month = today.getMonthValue();
//        processInstanceData.setProcessName("信息设备自查任务"+"("+"1"+ "月)");//
//        processInstanceData.setDisplayName("自动");
//        processInstanceData.setLoginName("李凌霄");
//        processInstanceData.setOrderNum(processInstanceDataService.getTimeAndRandomNum());
//        processInstanceData.setDeptName("伺服系统生产部");
//        processInstanceData.setStartDatetime(LocalDateTime.now());
//        processInstanceData.setProcessStatus("定时任务");
//        processInstanceData.setDisplayCurrentStep("李凌霄");
//        processInstanceData.setLoginCurrentStep("lilingxiao");
//        LocalDateTime localDateTime = LocalDateTime.now();
//        processInstanceData.setLastCommitDatetime(localDateTime);//20241015
//        processInstanceDataList.add(processInstanceData);

        processInstanceDataService.saveBatch(processInstanceDataList);//processInstanceDataService.saveBatch(processInstanceDataList);//保存成功后mybatis会把主键/ID传回参数processInstanceData中
        return true;
    };
    @Override
    public String addExcel(List<InspectionExcel> excelList, String importMode) {
        //去掉db中存在的信息点号，剩下页面上需要导入的信息点号
        List<InspectionExcel> pageList;
        //db中存在的信息点号
        List<Inspection> dbList = new ArrayList<>();

        //处理日期类型
         ExcelDateUtil.converToDate(excelList, InspectionExcel.class);
        /*
            importMode=是，先删除，后全部插入信息点号
            importMode=否，插入不在db中的信息点号
         */
        //20240626 todo改成 “日期+设备编号+检查人”三联组合判重


        pageList = excelList;

        if (ObjectUtil.isNotEmpty(pageList)) {
            //处理日期类型

            for (InspectionExcel inspectionExcel : pageList) {
                if (ObjectUtil.isEmpty(inspectionExcel.getNo())) {
                    throw new RuntimeException("请检查，设备编号不能为空");
                }
                if (ObjectUtil.isEmpty(inspectionExcel.getInspector())) {
                    throw new RuntimeException("请检查，检查人不能为空");
                }
                if (ObjectUtil.isEmpty(inspectionExcel.getInspectDateTmp())) {
                    throw new RuntimeException("请检查，检查日期不能为空");
                }
                AsDeviceCommon asDeviceCommon = asDeviceCommonService.getOne(new  QueryWrapper<AsDeviceCommon>().eq("org_id",GlobalParam.orgId).eq("no",inspectionExcel.getNo()));

                //要写入DB的对象
                Inspection inspection = new Inspection();
                BeanUtils.copyProperties(inspectionExcel, inspection);
                SysUser user = (SysUser) httpSession.getAttribute("user");
                String deptName = sysDeptService.getById(user.getDeptId()).getName();

                if(ObjectUtil.isEmpty(asDeviceCommon)){
                    throw new RuntimeException("设备编号" + inspectionExcel.getNo() + "在系统中不存在，请检查！");
                } else {//自动填充密级、联网类别等字段
                    if(importMode.equals("部门自查") && !asDeviceCommon.getUserDept().equals(deptName))
                        throw new RuntimeException( inspectionExcel.getNo() + "非本部门设备，不能导入！");
                    inspection.setMiji(asDeviceCommon.getMiji());
                    inspection.setUserDept(asDeviceCommon.getUserDept());
                    inspection.setUserName(asDeviceCommon.getUserName());
                    inspection.setNetType(asDeviceCommon.getNetType());
                    inspection.setCreateDatetime(LocalDateTime.now());
                    inspection.setMode(importMode);
                    inspection.setCreator(user.getDisplayName());
                }


                //检查人、检查日期、责任人三者都一样的记录；实际应该就一条
                List<Inspection> dupList = this.list(new  QueryWrapper<Inspection>().eq("org_id", GlobalParam.orgId).eq("no", inspectionExcel.getNo()).eq("inspect_date",inspectionExcel.getInspectDateTmp()).eq("inspector",inspectionExcel.getInspector()));
                //覆盖模式：删除重复值
                if (ObjectUtil.isNotEmpty(dupList)) {
                    List<Integer> idList = dupList.stream().map(Inspection::getId).collect(Collectors.toList());
                    this.removeByIds(idList);
                }


                this.save(inspection);
            }
        }
       // if (importMode.equals("是")) {
           // int pageCount = pageList.size() - dbList.size();
            return "导入" + pageList.size() + "条检查记录;以下设备的当日检查记录" ;
//        } else {
//            int pageCount = pageList.size();
//            if (ObjectUtil.isEmpty(dbList)) {
//                return "导入" + pageCount + "条检查记录";
//            } else {
//                return "导入" + pageCount + "条检查记录;以下设备的当日检查记录：" + dbList.stream().map(Inspection::getNo).collect(Collectors.joining(",")) + "已经存在，未导入";
//            }
//        }
    }
}
