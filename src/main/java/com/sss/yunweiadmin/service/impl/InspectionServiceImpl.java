package com.sss.yunweiadmin.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sss.yunweiadmin.common.config.GlobalParam;
import com.sss.yunweiadmin.common.utils.ExcelDateUtil;
import com.sss.yunweiadmin.model.entity.AsDeviceCommon;
import com.sss.yunweiadmin.model.entity.Inspection;
import com.sss.yunweiadmin.model.entity.Inspection;
import com.sss.yunweiadmin.mapper.InspectionMapper;
import com.sss.yunweiadmin.model.entity.SysUser;
import com.sss.yunweiadmin.model.excel.AsComputerExcel;
import com.sss.yunweiadmin.model.excel.InspectionExcel;
import com.sss.yunweiadmin.service.AsComputerGrantedService;
import com.sss.yunweiadmin.service.AsDeviceCommonService;
import com.sss.yunweiadmin.service.InspectionService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sss.yunweiadmin.service.SysDeptService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
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
