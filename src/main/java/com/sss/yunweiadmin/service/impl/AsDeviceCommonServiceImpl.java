

package com.sss.yunweiadmin.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.common.collect.ImmutableMap;
import com.sss.yunweiadmin.common.utils.ExcelDateUtil;
import com.sss.yunweiadmin.mapper.AsDeviceCommonMapper;
import com.sss.yunweiadmin.model.entity.*;
import com.sss.yunweiadmin.model.excel.*;
import com.sss.yunweiadmin.model.vo.AssetVO;
import com.sss.yunweiadmin.service.*;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 任勇林
 * @since 2021-10-08
 */
@Service
public class AsDeviceCommonServiceImpl extends ServiceImpl<AsDeviceCommonMapper, AsDeviceCommon> implements AsDeviceCommonService {
    @Autowired
    AsComputerSpecialService asComputerSpecialService;
    @Autowired
    private AsComputerGrantedService asComputerGrantedService;
    @Autowired
    AsNetworkDeviceSpecialService asNetworkDeviceSpecialService;
    @Autowired
    AsSecurityProductsSpecialService asSecurityProductsSpecialService;
    @Autowired
    AsIoSpecialService asIoSpecialService;
    @Autowired
    AsTypeService asTypeService;
    @Autowired
    AsApplicationSpecialService asApplicationSpecialService;
    @Autowired
    StatisticsService statisticsService;
    @Autowired
    SysUserService sysUserService;
    @Autowired
    SysDeptService sysDeptService;

    //20230213
    @Override
    public String makeBaomiNo(AsType asType, String miji, LocalDate useDate) {
        String baomiNo = "0094";
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter fmt_now;
        String dateStr;
        if(ObjectUtil.isNotEmpty(useDate)){//如果有”启用日期“，那么保密编号的年份就以这个为准；暂不考虑”更新“模式导入资产EXCEl的情况
            DateTimeFormatter fmt_useDate = DateTimeFormatter.ofPattern("yyyy");
            fmt_now = DateTimeFormatter.ofPattern("MMdd");
            dateStr = useDate.format(fmt_useDate) + now.format(fmt_now);

        } else {
            fmt_now = DateTimeFormatter.ofPattern("yyyyMMdd");
            dateStr = now.format(fmt_now);

        }


        Random rnd = new Random();
        int code = rnd.nextInt(10000000) + 1000000;
        String randomNum =Integer.toString(code);

        AsType asTypeLevel2 = asTypeService.getLevel2AsTypeById(asType.getId());

        if(asType.getId()==7){//打印机
            baomiNo = baomiNo + "D";
        } else if(asType.getId()==26)//复印机
            baomiNo = baomiNo + "F";
        else if(asTypeLevel2.getId()==31)//存储介质：通过二级类型ID判断
            baomiNo = baomiNo + "M";
        else if(asTypeLevel2.getId()==7)//安全产品：通过二级类型ID判断
            baomiNo = baomiNo + "A";
            //20230213“网络外设”与“办公自动化”根据联网类别分开：20230214暂不分，统称B
        else if(asTypeLevel2.getId()==6) //外设办公自动化：通过二级类型ID判断
            baomiNo = baomiNo + "B";
        else if(asTypeLevel2.getId()==4) //20230429 计算机：通过二级类型ID判断
            baomiNo = baomiNo + "J";
        else if(asTypeLevel2.getId()==29) //20230429 服务器与存储设备：通过二级类型ID判断
            baomiNo = baomiNo + "S";
        else
            baomiNo = baomiNo + "Q";
        baomiNo = baomiNo + dateStr + randomNum;
        if("机密".equals(miji))
            baomiNo = baomiNo + "5";
        else if("秘密".equals(miji))
            baomiNo = baomiNo + "3";
        else if("普通商密".equals(miji))
            baomiNo = baomiNo + "0";
        else if("非密".equals(miji))
            baomiNo = baomiNo + "2";
        else //无任何密级的情况：其实此时可以在打印标签时对此类密级情况屏敝
            baomiNo = baomiNo + "X";
        return  baomiNo;
    }



    //20230213
//    @Override
//    public String makeBaomiNo(AsType asType,String miji) {
//        String baomiNo = "0094";
//        LocalDateTime date = LocalDateTime.now();
//        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyyMMdd");
//        String dateStr = date.format(fmt);
//        Random rnd = new Random();
//        int code = rnd.nextInt(10000000) + 1000000;
//        String randomNum =Integer.toString(code);
//
//        AsType asTypeLevel2 = asTypeService.getLevel2AsTypeById(asType.getId());
//
//        if(asType.getId()==7){//打印机
//            baomiNo = baomiNo + "D";
//        } else if(asType.getId()==26)//复印机
//            baomiNo = baomiNo + "D";
//        else if(asTypeLevel2.getId()==31)//存储介质：通过二级类型ID判断
//            baomiNo = baomiNo + "M";
//        else if(asTypeLevel2.getId()==7)//安全产品：通过二级类型ID判断
//            baomiNo = baomiNo + "S";
//        //20230213“网络外设”与“办公自动化”根据联网类别分开：20230214暂不分，统称B
//        else if(asTypeLevel2.getId()==6) {//外设办公自动化：通过二级类型ID判断
////            if(ObjectUtil.isNotEmpty(netType)){
////                if(netType.contains("网"))
////                    baomiNo = baomiNo + "N";
////                else
////                    baomiNo = baomiNo + "E";
////
////            }else
//                baomiNo = baomiNo + "B";
//        }
//        else
//            baomiNo = baomiNo + "Q";
//        baomiNo = baomiNo + dateStr + randomNum;
//        if("机密".equals(miji))
//            baomiNo = baomiNo + "5";
//        else if("秘密".equals(miji))
//            baomiNo = baomiNo + "3";
//        else if("普通商密".equals(miji))
//            baomiNo = baomiNo + "0";
//        else if("非密".equals(miji))
//            baomiNo = baomiNo + "2";
//        else //无任何密级的情况：其实此时可以在打印标签时对此类密级情况屏敝
//            baomiNo = baomiNo + "X";
//        return  baomiNo;
//    }

    //20230107 添加对硬盘序列号/硬盘型号是“由逗号分隔的字符串”格式的处理
    private void callByAddComputer(String sn,String model,AsDeviceCommon asDeviceCommon,List<AsDeviceCommon> asDeviceCommonListForDiskAdd ,List<AsDeviceCommon> asDeviceCommonListForDiskUpdate , Map<String, Integer> ypNoIdMap){
        String[] sns = sn.split("\\,");
        String[] models = new String[0];
        if(ObjectUtil.isNotEmpty(model))
            models  = model.split("\\,");
        for (int i = 0; i< sns.length; i++ ){
            AsDeviceCommon asDeviceCommon1 = new AsDeviceCommon();
            asDeviceCommon1.setSn(sns[i]);
            if(i < models.length)
                asDeviceCommon1.setModel(models[i]);
            asDeviceCommon1.setUserName(asDeviceCommon.getUserName());
            asDeviceCommon1.setUserDept(asDeviceCommon.getUserDept());
            asDeviceCommon1.setMiji(asDeviceCommon.getMiji());
            asDeviceCommon1.setNetType(asDeviceCommon.getNetType());
            asDeviceCommon1.setUserMiji(asDeviceCommon.getUserMiji());//20221115
            asDeviceCommon1.setState(asDeviceCommon.getState());//20221115
            asDeviceCommon1.setName("硬盘");
            asDeviceCommon1.setTypeId(30);
            if (ObjectUtil.isNotEmpty(ypNoIdMap.get(sns[i]))) {//硬盘在DB中存在，则更新此硬盘信息
                asDeviceCommon1.setId(ypNoIdMap.get(sns[i]));
                asDeviceCommonListForDiskUpdate.add(asDeviceCommon1);
            } else {//硬盘在DB不存在：则新增
                LocalDateTime date = LocalDateTime.now();
                DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
                String dateStr = date.format(fmt);
                asDeviceCommon1.setNo("YP" + dateStr);
                asDeviceCommon1.setTypeId(30);
                asDeviceCommon1.setHostAsId(asDeviceCommon.getId());
                asDeviceCommonListForDiskAdd.add(asDeviceCommon1);
            }
        }
    }

    //20211115 不管啥类型，把所有专有表清清
    @Override
    public boolean delete(Integer[] idArr1) {
        List<Integer> idArr = Stream.of(idArr1).collect(Collectors.toList());
        asComputerSpecialService.remove(new QueryWrapper<AsComputerSpecial>().in("as_id", idArr));
        asNetworkDeviceSpecialService.remove(new QueryWrapper<AsNetworkDeviceSpecial>().in("as_id", idArr));
        asComputerGrantedService.remove(new QueryWrapper<AsComputerGranted>().in("as_id", idArr));
        asIoSpecialService.remove(new QueryWrapper<AsIoSpecial>().in("as_id", idArr));
        asSecurityProductsSpecialService.remove(new QueryWrapper<AsSecurityProductsSpecial>().in("as_id", idArr));
        asApplicationSpecialService.remove(new QueryWrapper<AsApplicationSpecial>().in("as_id", idArr));
        //删除硬盘信息
        List<AsDeviceCommon> diskList = this.list(new QueryWrapper<AsDeviceCommon>().in("host_as_id", idArr));
        this.removeByIds(diskList.stream().map(item -> item.getId()).collect(Collectors.toList()));
        //删除asDeviceCommon“主设备”
        this.removeByIds(idArr);
        return true;
    }

    private Statistics classifyStatics(LocalDate date_end,List<Integer> listAsId,String miji,String period,int asTypeId){
        LocalDate date_now = LocalDate.now();
       // List<Integer> listAsId = listMap.stream().map(item->Integer.parseInt(item.get("id").toString())).collect(Collectors.toList());
        //指定周期内&&计算机类别的操作系统安装数量
        Statistics statistics = new Statistics();
        statistics.setPeriod(period);
        statistics.setAsTypeId(asTypeId);
        statistics.setMiji(miji);
        statistics.setCreateTime(LocalDateTime.now());
        statistics.setAmount(listAsId.size());
        if(CollUtil.isNotEmpty(listAsId)){//queryRapper/in语句中参数中：list为0会报错
            List<AsComputerSpecial> listAsComputerSpecialOSdateFiltered = asComputerSpecialService.list(new QueryWrapper<AsComputerSpecial>().between("os_date", date_end,  date_now).in("as_id",listAsId));
            statistics.setReinstallAmount(listAsComputerSpecialOSdateFiltered.size());
        } else {
            statistics.setReinstallAmount(0);
        }
        return statistics;
      //  statisticsService.save(statistics);
    }

    @Override
    public boolean addStatistics() {//目前只考虑涉密：密级后续由参数传进来，不全部都统计
        statisticsService.remove(new QueryWrapper<>());//清空所有记录
        List<Statistics> statisticsList = new ArrayList<>();
        //先增加“桌面计算机” 的本年度重装查询
        LocalDate date_now = LocalDate.now();
        LocalDate date_end;
        Map<String, Integer> periodMap = ImmutableMap.of("本年度", 365, "本季度", 91, "本月", 30);//<周期，“回退”天数>
        for(Map.Entry<String, Integer> entryPeriod : periodMap.entrySet()){
            date_end = date_now.minusDays(entryPeriod.getValue());
            Integer[] allowedTypeIdArray= {23,22,21,8,9};//需要记录的typeId
            List<Integer> list =  Arrays.asList(allowedTypeIdArray);
            List<AsDeviceCommon> asDeviceCommonList = this.list(new QueryWrapper<AsDeviceCommon>().in("type_id", list).ne("state","停用").ne("state","报废").ne("state","库存").and(qw -> qw.eq("miji", "秘密").or().eq("miji", "机密")));
            Map<Integer,List<Integer>> mapList = new HashMap<>();//<类型ID，资产IdList>
            for(Integer typeId : allowedTypeIdArray){
                mapList.put(typeId,new ArrayList<Integer>());
            }
            //组装mapList
            for(AsDeviceCommon a : asDeviceCommonList){
                mapList.get(a.getTypeId()).add(a.getId());
            }
            //遍历mapList
            for (Map.Entry<Integer,List<Integer>> entry : mapList.entrySet()) {
                Statistics statistics = classifyStatics(date_end,entry.getValue(),"涉密",entryPeriod.getKey(),entry.getKey());
                if(ObjectUtil.isNotEmpty(statistics))
                    statisticsList.add(statistics);
            }
        }
        if(CollUtil.isNotEmpty(statisticsList))
            statisticsService.saveBatch(statisticsList);
        return true;
    }

    @Override
    public boolean add(AssetVO assetVO) {
        boolean flag;
        AsDeviceCommon asDeviceCommon = assetVO.getAsDeviceCommon();
        flag = this.save(asDeviceCommon);
        //资产类型
        AsType asType = asTypeService.getLevel2AsTypeById(asDeviceCommon.getTypeId());
        Integer asTypeId = asType.getId();
        if (asTypeId == 4 || asTypeId == 29) {//20230107 添加服务器与存储设备id
            //计算机
            AsComputerSpecial asComputerSpecial = assetVO.getAsComputerSpecial();
            if (ObjectUtil.isNotEmpty(asComputerSpecial)) {
                asComputerSpecial.setAsId(asDeviceCommon.getId());
                flag = flag && asComputerSpecialService.save(asComputerSpecial);
            }
            AsComputerGranted asComputerGranted = assetVO.getAsComputerGranted();
            if (ObjectUtil.isNotEmpty(asComputerGranted)) {
                asComputerGranted.setAsId(asDeviceCommon.getId());
                flag = flag && asComputerGrantedService.save(asComputerGranted);
            }
            //20220620 保存硬盘信息
            List<AsDeviceCommon> diskListForHis = assetVO.getDiskListForHis();
            if (CollUtil.isNotEmpty(diskListForHis)) {
                flag = flag && this.saveBatch(diskListForHis.stream().map(item -> {
                    item.setHostAsId(asDeviceCommon.getId());
                    item.setMiji(asDeviceCommon.getMiji());
                    item.setUserName(asDeviceCommon.getMiji());
                    item.setNetType(asDeviceCommon.getNetType());
                    item.setUserDept(asDeviceCommon.getUserDept());
                    return item;
                }).collect(Collectors.toList()));//20220614这个flag设置有点小问题：暂不改
            }
        } else if (asTypeId == 5) {
            //网络设备
            AsNetworkDeviceSpecial asNetworkDeviceSpecial = assetVO.getAsNetworkDeviceSpecial();
            if (ObjectUtil.isNotEmpty(asNetworkDeviceSpecial)) {
                asNetworkDeviceSpecial.setAsId(asDeviceCommon.getId());
                flag = flag && asNetworkDeviceSpecialService.save(asNetworkDeviceSpecial);
            }
        } else if (asTypeId == 6) {
            //外设
            AsIoSpecial asIoSpecial = assetVO.getAsIoSpecial();
            if (ObjectUtil.isNotEmpty(asIoSpecial)) {
                asIoSpecial.setAsId(asDeviceCommon.getId());
                flag = flag && asIoSpecialService.save(asIoSpecial);
            }
        } else if (asTypeId == 7) {
            //安全产品（硬件）
            AsSecurityProductsSpecial asSecurityProductsSpecial = assetVO.getAsSecurityProductsSpecial();
            if (ObjectUtil.isNotEmpty(asSecurityProductsSpecial)) {
                asSecurityProductsSpecial.setAsId(asDeviceCommon.getId());
                flag = flag && asSecurityProductsSpecialService.save(asSecurityProductsSpecial);
            }
        } else if (asTypeId == 19) {
            //应用系统
            AsApplicationSpecial asApplicationSpecial = assetVO.getAsApplicationSpecial();
            if (ObjectUtil.isNotEmpty(asApplicationSpecial)) {
                asApplicationSpecial.setAsId(asDeviceCommon.getId());
                flag = flag && asApplicationSpecialService.save(asApplicationSpecial);
            }
        }
        return flag;
    }

    @Override
    public boolean edit(AssetVO assetVO) {
        boolean flag;
        AsDeviceCommon asDeviceCommon = assetVO.getAsDeviceCommon();
        AsDeviceCommon asDeviceCommonDB = this.getById(asDeviceCommon.getId());
        if(!asDeviceCommon.getMiji().equals(asDeviceCommonDB.getMiji())){//20230215密级变化需要触发保密编号生成；可能“类型变化”也需要触发（先考虑类型目前能不能变化：暂不研）
            AsType asType = asTypeService.getById(asDeviceCommon.getTypeId());
            asDeviceCommon.setBaomiNo(this.makeBaomiNo(asType,asDeviceCommon.getMiji(),asDeviceCommon.getUseDate()));
        }
        flag = this.updateById(asDeviceCommon);
        //下面是针对于专用表的处理
        AsType asType = asTypeService.getLevel2AsTypeById(asDeviceCommon.getTypeId());
        Integer asTypeId = asType.getId();
        if (asTypeId == 4 || asTypeId == 29) {//20230107 添加服务器与存储设备id
            //计算机
            AsComputerSpecial asComputerSpecial = assetVO.getAsComputerSpecial();
            if (ObjectUtil.isNotEmpty(asComputerSpecial)) {
                asComputerSpecial.setAsId(asDeviceCommon.getId());
                flag = flag && asComputerSpecialService.saveOrUpdate(asComputerSpecial);
            }
            AsComputerGranted asComputerGranted = assetVO.getAsComputerGranted();
            if (ObjectUtil.isNotEmpty(asComputerGranted)) {
                asComputerGranted.setAsId(asDeviceCommon.getId());
                flag = flag && asComputerGrantedService.saveOrUpdate(asComputerGranted);
            }
            //20220612 保存硬盘信息
            List<AsDeviceCommon> diskListForHis = assetVO.getDiskListForHis();
            List<AsDeviceCommon> diskListForHisForDel = diskListForHis.stream().filter(item -> item.getTemp().equals("删除")).collect(Collectors.toList());
            List<AsDeviceCommon> diskListForHisForSaveOrUpdate = diskListForHis.stream().filter(item -> !(item.getTemp().equals("删除"))).collect(Collectors.toList());
            if (CollUtil.isNotEmpty(diskListForHis)) {
                if (CollUtil.isNotEmpty(diskListForHisForDel))
                    this.removeByIds(diskListForHisForDel.stream().map(AsDeviceCommon::getId).collect(Collectors.toList()));
                if (CollUtil.isNotEmpty(diskListForHisForSaveOrUpdate)) {
                    diskListForHisForSaveOrUpdate.forEach(item -> item.setTemp(""));
                    flag = flag && this.saveOrUpdateBatch(diskListForHisForSaveOrUpdate);//20220614这个flag设置有点小问题：暂不改
                }
            }
        } else if (asTypeId == 5) {
            //网络设备
            AsNetworkDeviceSpecial asNetworkDeviceSpecial = assetVO.getAsNetworkDeviceSpecial();
            if (ObjectUtil.isNotEmpty(asNetworkDeviceSpecial)) {
                asNetworkDeviceSpecial.setAsId(asDeviceCommon.getId());
                flag = flag && asNetworkDeviceSpecialService.saveOrUpdate(asNetworkDeviceSpecial);
            }
        } else if (asTypeId == 6) {
            //外设
            AsIoSpecial asIoSpecial = assetVO.getAsIoSpecial();
            if (ObjectUtil.isNotEmpty(asIoSpecial)) {
                asIoSpecial.setAsId(asDeviceCommon.getId());
                flag = flag && asIoSpecialService.saveOrUpdate(asIoSpecial);
            }
        } else if (asTypeId == 7) {
            //安全产品（硬件）
            AsSecurityProductsSpecial asSecurityProductsSpecial = assetVO.getAsSecurityProductsSpecial();
            if (ObjectUtil.isNotEmpty(asSecurityProductsSpecial)) {
                asSecurityProductsSpecial.setAsId(asDeviceCommon.getId());
                flag = flag && asSecurityProductsSpecialService.saveOrUpdate(asSecurityProductsSpecial);
            }
        } else if (asTypeId == 19) {
            //应用系统
            AsApplicationSpecial asApplicationSpecial = assetVO.getAsApplicationSpecial();
            if (ObjectUtil.isNotEmpty(asApplicationSpecial)) {
                asApplicationSpecial.setAsId(asDeviceCommon.getId());
                flag = flag && asApplicationSpecialService.saveOrUpdate(asApplicationSpecial);
            }
        }
        //20220612 todo问张强，这里是不要flag为false时，抛个异常（可以引发回滚吗？），让其回滚？
        return flag;
    }

    private String addAsComputerExcel(List<AsComputerExcel> excelList, String importMode) {
        //20221115从用户表读“部门+用户名”匹配上的人员信息，将其密级赋给计算机本身和硬盘
        List<SysUser> userList = sysUserService.list(new QueryWrapper<SysUser>().eq("status","正常"));
        List<SysDept> deptList = sysDeptService.list(new QueryWrapper<SysDept>().eq("pid",2));
        //格式：<信息化中心,13>
        Map<Integer,String> mapDept = new HashMap<>();
        for(SysDept dept : deptList){
            mapDept.put(dept.getId(),dept.getName());
        }
        //格式：<信息化中心.张三,机密>
        Map<String, String> mapUserMiji = new HashMap<>();
        for(SysUser user : userList){
            mapUserMiji.put(mapDept.get(user.getDeptId())+"."+ user.getDisplayName(), user.getSecretDegree());
        }

        //去掉db中存在的设备，剩下页面上需要导入的设备
        List<AsComputerExcel> pageList;
        //db中存在的设备
        List<AsDeviceCommon> dbList = new ArrayList<>();

        //处理日期类型
        ExcelDateUtil.converToDate(excelList, AsComputerExcel.class);
        /*
            importMode=覆盖，先删除，后全部插入设备：会影响设备ID（会新生成）
            importMode=新增，插入不在db中的设备
            20221227 增加”字段级更新“导入模式的逻辑
            注意：更新模式中/对应硬盘的更新：CRUD都有：对DB中有&&导入表中没有的硬盘&&状态是"非 待报废|销毁|归库"的进行删除：但对于更新/新增（时的比较判断逻辑）来说：非在用的硬盘也要考虑进行去
         */
        if (importMode.equals("更新")) {
            List<Integer> asTypeIdList = asTypeService.getTypeIdList(4);//约定了计算机的id:4
            List<Integer> asTypeIdList2 = asTypeService.getTypeIdList(29);//约定了服务器存储设备的id:29
            asTypeIdList.addAll(asTypeIdList2);
            List<Map<String, Object>> listMaps = this.listMaps(new QueryWrapper<AsDeviceCommon>().in("type_id", asTypeIdList).select("no", "id"));
            List<String> updateFailedList  = new ArrayList<>();
            Map<String, Integer> noIdMap = new HashMap<>();
            for (Map<String, Object> map : listMaps) {//可以直接用java8直接转map:见下面更新硬盘代码
                noIdMap.put(map.get("no").toString(), Integer.valueOf(map.get("id").toString()));
            }
            if (ObjectUtil.isNotEmpty(excelList)) {
                for (AsComputerExcel asComputerExcel : excelList) {
                    if (ObjectUtil.isEmpty(asComputerExcel.getNo())) {
                        throw new RuntimeException("设备编号不能为空");
                    }
//                    if (ObjectUtil.isEmpty(asComputerExcel.getMiji())) {
//                        throw new RuntimeException("设备密级不能为空");
//                    }
                    AsType asType = asTypeService.getAsType(asComputerExcel.getTypeName());
                    if (asType == null) {
                        throw new RuntimeException("设备类别不存在");
                    }
//                    if (ObjectUtil.isEmpty(asComputerExcel.getState())) {
//                        throw new RuntimeException("设备状态不能为空");
//                    }
                    AsDeviceCommon asDeviceCommon = new AsDeviceCommon();
                    AsComputerSpecial asComputerSpecial = new AsComputerSpecial();
                    AsComputerGranted asComputerGranted = new AsComputerGranted();
                    //
                    BeanUtils.copyProperties(asComputerExcel, asDeviceCommon);
                    BeanUtils.copyProperties(asComputerExcel, asComputerSpecial);
                    BeanUtils.copyProperties(asComputerExcel, asComputerGranted);
                   // asDeviceCommon.setTypeId(asType.getId());//20230129先注释： 有时间加判断：设备类型不能变化：如更填写了&&与DB中不一样，要提示报错
                    if (ObjectUtil.isNotEmpty(noIdMap.get(asDeviceCommon.getNo()))) {
                        asDeviceCommon.setId(noIdMap.get(asDeviceCommon.getNo()));
                        this.updateById(asDeviceCommon);
                        //提前准备新增硬盘需要填充的（需来自“主设备”的）数据：如果excel中没有就从DB中获取
                        if (ObjectUtil.isEmpty(asDeviceCommon.getUserMiji()))
                            asDeviceCommon.setUserMiji(this.getById(noIdMap.get(asDeviceCommon.getNo())).getUserMiji());
                        if (ObjectUtil.isEmpty(asDeviceCommon.getUserName()))
                            asDeviceCommon.setUserName(this.getById(noIdMap.get(asDeviceCommon.getNo())).getUserName());
                        if (ObjectUtil.isEmpty(asDeviceCommon.getUserDept()))
                            asDeviceCommon.setUserDept(this.getById(noIdMap.get(asDeviceCommon.getNo())).getUserDept());
                        if (ObjectUtil.isEmpty(asDeviceCommon.getMiji()))
                            asDeviceCommon.setMiji(this.getById(noIdMap.get(asDeviceCommon.getNo())).getMiji());
                        if (ObjectUtil.isEmpty(asDeviceCommon.getNetType()))
                            asDeviceCommon.setNetType(this.getById(noIdMap.get(asDeviceCommon.getNo())).getNetType());
                        if (ObjectUtil.isEmpty(asDeviceCommon.getState()))
                            asDeviceCommon.setState(this.getById(noIdMap.get(asDeviceCommon.getNo())).getState());
                        asComputerSpecial.setAsId(asDeviceCommon.getId());
                        asComputerSpecial.setId(asComputerSpecialService.getOne(new QueryWrapper<AsComputerSpecial>().eq("as_id",asDeviceCommon.getId())).getId());
                        asComputerGranted.setAsId(asDeviceCommon.getId());
                        asComputerGranted.setId(asComputerGrantedService.getOne(new QueryWrapper<AsComputerGranted>().eq("as_id",asDeviceCommon.getId())).getId());
                        asComputerSpecialService.updateById(asComputerSpecial);
                        asComputerGrantedService.updateById(asComputerGranted);
                        List<AsDeviceCommon> asDeviceCommonListForDiskAdd = new ArrayList<>();
                        List<AsDeviceCommon> asDeviceCommonListForDiskUpdate = new ArrayList<>();
                        //查找出目前DB中该计算机对应的硬盘：这里包括“非在用”
                        List<Map<String, Object>> ypListMaps = this.listMaps(new QueryWrapper<AsDeviceCommon>().eq("type_id", 30).eq("host_as_id", noIdMap.get(asDeviceCommon.getNo())).select("sn", "id"));
                        Map<String, Integer> ypNoIdMap = ypListMaps.stream().collect(Collectors.toMap(v -> v.get("sn").toString(), v -> Integer.valueOf(v.get("id").toString()), (key1, key2) -> key2));
                        if (ObjectUtil.isNotEmpty(asComputerExcel.getDiskSn1())) {
                            callByAddComputer(asComputerExcel.getDiskSn1(),asComputerExcel.getDiskMode1(), asDeviceCommon,asDeviceCommonListForDiskAdd , asDeviceCommonListForDiskUpdate , ypNoIdMap);
                        }
                        if (ObjectUtil.isNotEmpty(asComputerExcel.getDiskSn2())) {
                            callByAddComputer(asComputerExcel.getDiskSn2(),asComputerExcel.getDiskMode2(), asDeviceCommon,asDeviceCommonListForDiskAdd , asDeviceCommonListForDiskUpdate , ypNoIdMap);
                        }
                        if (ObjectUtil.isNotEmpty(asComputerExcel.getDiskSn3())) {
                            callByAddComputer(asComputerExcel.getDiskSn3(),asComputerExcel.getDiskMode3(), asDeviceCommon,asDeviceCommonListForDiskAdd , asDeviceCommonListForDiskUpdate , ypNoIdMap);
                        }
                        if (ObjectUtil.isNotEmpty(asComputerExcel.getDiskSn4())) {
                            callByAddComputer(asComputerExcel.getDiskSn4(),asComputerExcel.getDiskMode4(), asDeviceCommon,asDeviceCommonListForDiskAdd , asDeviceCommonListForDiskUpdate , ypNoIdMap);
                        }
                        this.updateBatchById(asDeviceCommonListForDiskUpdate);
                        this.saveBatch(asDeviceCommonListForDiskAdd);
                        //删除未在导入表格sn字段中登记的老硬盘:只有在硬盘数据有导入的情况，才考虑删除; 20230403有时间考虑下要过滤（阻止删除）正在走流程的硬盘
                        if(CollUtil.isNotEmpty(asDeviceCommonListForDiskAdd) || CollUtil.isNotEmpty(asDeviceCommonListForDiskUpdate)) {
                            List<AsDeviceCommon> handledList = asDeviceCommonListForDiskAdd.stream().collect(Collectors.toList());
                            handledList.addAll(asDeviceCommonListForDiskUpdate);
                            List<String> handledSnList = handledList.stream().map(item -> item.getSn().toString()).collect(Collectors.toList());
                            //注：只统计“在用”硬盘：下面的根据导入excel清理计算机对应的DB中的老硬盘：也不清理之前的“特殊状态”硬盘：暂不细研
                            List<String> excludeState = Arrays.asList(new String[]{"报废", "待报废", "销毁", "归库"});
                            List<AsDeviceCommon> dbHarkDiskListForDeleteForCurrentComputer;
                            if (CollUtil.isNotEmpty(handledSnList))
                                dbHarkDiskListForDeleteForCurrentComputer = this.list(new QueryWrapper<AsDeviceCommon>().notIn("state", excludeState).notIn("sn", handledSnList).eq("host_as_id", asDeviceCommon.getId()));
                            else
                                dbHarkDiskListForDeleteForCurrentComputer = this.list(new QueryWrapper<AsDeviceCommon>().notIn("state", excludeState).eq("host_as_id", asDeviceCommon.getId()));
                            this.removeByIds(dbHarkDiskListForDeleteForCurrentComputer.stream().map(item -> Integer.valueOf(item.getId().toString())).collect(Collectors.toList()));
                        }
                    } else {  //20221227资产号在DB不存在的设备：做一个记录
                        updateFailedList.add(asDeviceCommon.getNo());
                    }

                }
            }
            int updateSucessCount = excelList.size() - updateFailedList.size();
            if(CollUtil.isNotEmpty(updateFailedList))
                return "更新了" + updateSucessCount + "条设备信息；" + "此外，" + updateFailedList.stream().collect(Collectors.joining(","))+ "不存在，请先导入系统";
            else
                return "更新了" + updateSucessCount + "条设备信息";
        } else  { //这个分支是之前的导入模式：都是需要删除老记录&&设备ID会变
            if (importMode.equals("覆盖")) {//可重复&需覆盖，把重复的从DB删除
                List<AsDeviceCommon> list = this.list(new QueryWrapper<AsDeviceCommon>().in("no", excelList.stream().map(AsComputerExcel::getNo).collect(Collectors.toList())));
                if (ObjectUtil.isNotEmpty(list)) {
                    dbList = list;
                    List<Integer> asIdList = list.stream().map(AsDeviceCommon::getId).collect(Collectors.toList());
                    this.removeByIds(asIdList);
                    //20221226加 删除计算机硬盘:效果待观察
                    List<Map<String, Object>> listMaps1 = this.listMaps(new QueryWrapper<AsDeviceCommon>().in("host_as_id", asIdList).select("id"));
                    List<Integer> ypIdList = listMaps1.stream().map(item -> Integer.valueOf(item.get("id").toString())).collect(Collectors.toList());
                    this.removeByIds(ypIdList);
                    asComputerSpecialService.remove(new QueryWrapper<AsComputerSpecial>().in("as_id", asIdList));
                    asComputerGrantedService.remove(new QueryWrapper<AsComputerGranted>().in("as_id", asIdList));
                }
                pageList = excelList;
            } else {//不可重复&不覆盖; 属于“纯粹新增模式”
            //20211116读出DB与EXCEL资产号重复的记录
                List<AsDeviceCommon> list = this.list(new QueryWrapper<AsDeviceCommon>().in("no", excelList.stream().map(AsComputerExcel::getNo).collect(Collectors.toList())));
                if (ObjectUtil.isEmpty(list)) {//此时导入的NO均不存在DB中
                    pageList = excelList;
                } else {//有与DB重复的情况，注意：如果数据均是重复的，这里处理完后pageList里没数据了
                    dbList = list;
                    Set<String> noSet = list.stream().map(AsDeviceCommon::getNo).collect(Collectors.toSet());
                    pageList = excelList.stream().filter(item -> !noSet.contains(item.getNo())).collect(Collectors.toList());
                }
            }
            if (ObjectUtil.isNotEmpty(pageList)) {
                List<AsComputerSpecial> asComputerSpecialList = new ArrayList<>();
                List<AsComputerGranted> asComputerGrantedList = new ArrayList<>();
                for (AsComputerExcel asComputerExcel : pageList) {
                    if (ObjectUtil.isEmpty(asComputerExcel.getNo())) {
                        throw new RuntimeException("设备编号不能为空");
                    }
                    AsType asType = asTypeService.getAsType(asComputerExcel.getTypeName());
                    if (asType == null) {
                        throw new RuntimeException("设备类别不存在");
                    }
                    if (ObjectUtil.isEmpty(asComputerExcel.getState())) {
                        throw new RuntimeException("设备状态不能为空");
                    } else {
                        if(asComputerExcel.getState().equals("在用") && ObjectUtil.isEmpty(asComputerExcel.getMiji()) )
                            throw new RuntimeException("“在用”设备的密级不能为空");
                    }
                    AsDeviceCommon asDeviceCommon = new AsDeviceCommon();
                    AsComputerSpecial asComputerSpecial = new AsComputerSpecial();
                    AsComputerGranted asComputerGranted = new AsComputerGranted();
                    //
                    BeanUtils.copyProperties(asComputerExcel, asDeviceCommon);
                    BeanUtils.copyProperties(asComputerExcel, asComputerSpecial);
                    BeanUtils.copyProperties(asComputerExcel, asComputerGranted);
                    //
                    asDeviceCommon.setTypeId(asType.getId());
                    //20221115比对用户表获取用户密级，并赋值给计算机及硬盘
                    String userMiji = mapUserMiji.get(asDeviceCommon.getUserDept() + "." + asDeviceCommon.getUserName());
                    if (ObjectUtil.isNotEmpty(userMiji))
                        asDeviceCommon.setUserMiji(userMiji);
                    //20230213 添加 保密编号的非空判断
                    if(ObjectUtil.isEmpty(asDeviceCommon.getBaomiNo())){
                        //20230312 todo往 makeBaomiNo()传入”设备启用时间“参数：这里已经把"Date"字段处理成localdate类型了，直接用即可
                        asDeviceCommon.setBaomiNo(this.makeBaomiNo(asType,asDeviceCommon.getMiji(),asDeviceCommon.getUseDate()));
                    }
                    //20211116save之后会把主键ID写入asDeviceCommon
                    this.save(asDeviceCommon);
                    asComputerSpecial.setAsId(asDeviceCommon.getId());
                    asComputerGranted.setAsId(asDeviceCommon.getId());
                    //
                    List<AsDeviceCommon> asDeviceCommonListForDisk = new ArrayList<>();
                    //  asDeviceCommonList.add(asDeviceCommon);
                    asComputerSpecialList.add(asComputerSpecial);
                    asComputerGrantedList.add(asComputerGranted);

                    //20220623组织硬盘信息，后面还要加对“逗号隔开”的处理
                    //20230109 以下两个参数是空值&&无用：仅为了兼容调用函数
                    List<AsDeviceCommon> asDeviceCommonListForDiskUpdate = new ArrayList<>();
                    Map<String, Integer> ypNoIdMap = new HashMap<>();
                    if (ObjectUtil.isNotEmpty(asComputerExcel.getDiskSn1())) {
                        callByAddComputer(asComputerExcel.getDiskSn1(),asComputerExcel.getDiskMode1(), asDeviceCommon,asDeviceCommonListForDisk , asDeviceCommonListForDiskUpdate , ypNoIdMap);
                    }
                    if (ObjectUtil.isNotEmpty(asComputerExcel.getDiskSn2())) {
                        callByAddComputer(asComputerExcel.getDiskSn2(),asComputerExcel.getDiskMode2(), asDeviceCommon,asDeviceCommonListForDisk , asDeviceCommonListForDiskUpdate , ypNoIdMap);
                    }
                    if (ObjectUtil.isNotEmpty(asComputerExcel.getDiskSn3())) {
                        callByAddComputer(asComputerExcel.getDiskSn3(),asComputerExcel.getDiskMode3(), asDeviceCommon,asDeviceCommonListForDisk , asDeviceCommonListForDiskUpdate , ypNoIdMap);
                    }
                    if (ObjectUtil.isNotEmpty(asComputerExcel.getDiskSn4())) {
                        callByAddComputer(asComputerExcel.getDiskSn4(),asComputerExcel.getDiskMode4(), asDeviceCommon,asDeviceCommonListForDisk , asDeviceCommonListForDiskUpdate , ypNoIdMap);
                    }
                    this.saveBatch(asDeviceCommonListForDisk);
                }
                //
                // this.saveBatch(asDeviceCommonList);
                asComputerSpecialService.saveBatch(asComputerSpecialList);
                asComputerGrantedService.saveBatch(asComputerGrantedList);
            }
            if (importMode.equals("覆盖")) {
                int pageCount = pageList.size() - dbList.size();
                return "导入" + pageCount + "条资产;设备编号：" + dbList.stream().map(AsDeviceCommon::getNo).collect(Collectors.joining(",")) + ",已经被覆盖";
            } else {
                int pageCount = pageList.size();
                if (ObjectUtil.isEmpty(dbList)) {
                    return "导入" + pageCount + "条资产";
                } else {
                    return "导入" + pageCount + "条资产;设备编号：" + dbList.stream().map(AsDeviceCommon::getNo).collect(Collectors.joining(",")) + "已经存在，未导入";
                }
            }
        }
    }

    private String addAsNetworkDeviceSpecialExcel(List<AsNetworkDeviceExcel> excelList, String importMode) {
        //去掉db中存在的设备，剩下页面上需要导入的设备
        List<AsNetworkDeviceExcel> pageList;
        //db中存在的设备
        List<AsDeviceCommon> dbList = new ArrayList<>();
        //处理日期类型
        ExcelDateUtil.converToDate(excelList, AsNetworkDeviceExcel.class);
        if (importMode.equals("更新")) {//20221227
            List<Integer> asTypeIdList = asTypeService.getTypeIdList(5);//约定了网络设备的typeid:5
            List<Map<String, Object>> listMaps = this.listMaps(new QueryWrapper<AsDeviceCommon>().in("type_id", asTypeIdList).select("no", "id"));
            List<String> updateFailedList  = new ArrayList<>();
            Map<String, Integer> noIdMap = new HashMap<>();
            for (Map<String, Object> map : listMaps) {
                noIdMap.put(map.get("no").toString(), Integer.valueOf(map.get("id").toString()));
            }
            if (ObjectUtil.isNotEmpty(excelList)) {
                for (AsNetworkDeviceExcel asNetworkDeviceExcel : excelList) {
                    if (ObjectUtil.isEmpty(asNetworkDeviceExcel.getNo())) {
                        throw new RuntimeException("设备编号不能为空");
                    }
                    AsDeviceCommon asDeviceCommon = new AsDeviceCommon();
                    AsNetworkDeviceSpecial asNetworkDeviceSpecial = new AsNetworkDeviceSpecial();
                    //
                    BeanUtils.copyProperties(asNetworkDeviceExcel, asDeviceCommon);
                    BeanUtils.copyProperties(asNetworkDeviceExcel, asNetworkDeviceSpecial);
                    //
                    //asDeviceCommon.setTypeId(asType.getId());
                    if (ObjectUtil.isNotEmpty(noIdMap.get(asDeviceCommon.getNo()))) {
                        asDeviceCommon.setId(noIdMap.get(asDeviceCommon.getNo()));
                        this.updateById(asDeviceCommon);
                        asNetworkDeviceSpecial.setId(asNetworkDeviceSpecialService.getOne(new QueryWrapper<AsNetworkDeviceSpecial>().eq("as_id",asDeviceCommon.getId())).getId());
                        asNetworkDeviceSpecial.setAsId(asDeviceCommon.getId());
                        asNetworkDeviceSpecialService.updateById(asNetworkDeviceSpecial);
                    } else //20221227资产号在DB不存在的设备：做一个记录
                        updateFailedList.add(asDeviceCommon.getNo());
                }
            }
            int updateSucessCount = excelList.size() - updateFailedList.size();
            if(CollUtil.isNotEmpty(updateFailedList))
                return "更新了" + updateSucessCount + "条设备信息；" + "此外，" + updateFailedList.stream().collect(Collectors.joining(","))+ "不存在，请先导入系统";
            else
                return "更新了" + updateSucessCount + "条设备信息";
        } else {
            if(importMode.equals("覆盖")) {
                List<AsDeviceCommon> list = this.list(new QueryWrapper<AsDeviceCommon>().in("no", excelList.stream().map(AsNetworkDeviceExcel::getNo).collect(Collectors.toList())));
                if (ObjectUtil.isNotEmpty(list)) {
                    dbList = list;
                    List<Integer> asIdList = list.stream().map(AsDeviceCommon::getId).collect(Collectors.toList());
                    this.removeByIds(asIdList);
                    asNetworkDeviceSpecialService.remove(new QueryWrapper<AsNetworkDeviceSpecial>().in("as_id", asIdList));
                }
                pageList = excelList;
            } else {
                List<AsDeviceCommon> list = this.list(new QueryWrapper<AsDeviceCommon>().in("no", excelList.stream().map(AsNetworkDeviceExcel::getNo).collect(Collectors.toList())));
                if (ObjectUtil.isEmpty(list)) {
                    pageList = excelList;
                } else {
                    dbList = list;
                    Set<String> noSet = list.stream().map(AsDeviceCommon::getNo).collect(Collectors.toSet());
                    pageList = excelList.stream().filter(item -> !noSet.contains(item.getNo())).collect(Collectors.toList());
                }
            }
            //
            List<AsDeviceCommon> asDeviceCommonList = new ArrayList<>();
            List<AsNetworkDeviceSpecial> asNetworkDeviceSpecialList = new ArrayList<>();

            //
            for (AsNetworkDeviceExcel asNetworkDeviceExcel : pageList) {
                if (ObjectUtil.isEmpty(asNetworkDeviceExcel.getNo())) {
                    throw new RuntimeException(asNetworkDeviceExcel.getName() + "的设备编号不能为空");
                }
                AsType asType = asTypeService.getAsType(asNetworkDeviceExcel.getTypeName());
                if (asType == null) {
                    throw new RuntimeException("设备类别不存在");
                }
                if (ObjectUtil.isEmpty(asNetworkDeviceExcel.getState())) {
                    throw new RuntimeException("设备状态不能为空");
                } else {
                    if(asNetworkDeviceExcel.getState().equals("在用") && ObjectUtil.isEmpty(asNetworkDeviceExcel.getMiji()) )
                        throw new RuntimeException("“在用”设备的密级不能为空");
                }
                AsDeviceCommon asDeviceCommon = new AsDeviceCommon();
                AsNetworkDeviceSpecial asNetworkDeviceSpecial = new AsNetworkDeviceSpecial();
                //
                BeanUtils.copyProperties(asNetworkDeviceExcel, asDeviceCommon);
                BeanUtils.copyProperties(asNetworkDeviceExcel, asNetworkDeviceSpecial);
                //
                asDeviceCommon.setTypeId(asType.getId());
                //20230213 添加 保密编号的非空判断
                if(ObjectUtil.isEmpty(asDeviceCommon.getBaomiNo())){
                    asDeviceCommon.setBaomiNo(this.makeBaomiNo(asType,asDeviceCommon.getMiji(),asDeviceCommon.getUseDate()));
                }
                this.save(asDeviceCommon);
                asNetworkDeviceSpecial.setAsId(asDeviceCommon.getId());
                //
                asDeviceCommonList.add(asDeviceCommon);
                asNetworkDeviceSpecialList.add(asNetworkDeviceSpecial);
            }
            asNetworkDeviceSpecialService.saveBatch(asNetworkDeviceSpecialList);
            //
            if (importMode.equals("覆盖")) {
                int pageCount = pageList.size() - dbList.size();
                return "导入" + pageCount + "条资产;设备编号：" + dbList.stream().map(AsDeviceCommon::getNo).collect(Collectors.joining(",")) + ",已经被覆盖";
            } else {
                int pageCount = pageList.size();
                if (ObjectUtil.isEmpty(dbList)) {
                    return "导入" + pageCount + "条资产";
                } else {
                    return "导入" + pageCount + "条资产;设备编号：" + dbList.stream().map(AsDeviceCommon::getNo).collect(Collectors.joining(",")) + "已经存在，未导入";
                }
            }
        }

    }

    private String addAsSecurityProductsSpecialExcel(List<AsSecurityProductExcel> excelList, String importMode) {
        //去掉db中存在的设备，剩下页面上需要导入的设备
        List<AsSecurityProductExcel> pageList;
        //db中存在的设备
        List<AsDeviceCommon> dbList = new ArrayList<>();
        //处理日期类型
        ExcelDateUtil.converToDate(excelList, AsSecurityProductExcel.class);
        if (importMode.equals("更新")) {//20221227
            List<Integer> asTypeIdList = asTypeService.getTypeIdList(7);//约定了安全产品的typeid:7
            List<Map<String, Object>> listMaps = this.listMaps(new QueryWrapper<AsDeviceCommon>().in("type_id", asTypeIdList).select("no", "id"));
            List<String> updateFailedList  = new ArrayList<>();
            Map<String, Integer> noIdMap = new HashMap<>();
            for (Map<String, Object> map : listMaps) {
                noIdMap.put(map.get("no").toString(), Integer.valueOf(map.get("id").toString()));
            }
            if (ObjectUtil.isNotEmpty(excelList)) {
                for (AsSecurityProductExcel asSecurityProductExcel : excelList) {
                    if (ObjectUtil.isEmpty(asSecurityProductExcel.getNo())) {
                        throw new RuntimeException("设备编号不能为空");
                    }
                    AsDeviceCommon asDeviceCommon = new AsDeviceCommon();
                    AsSecurityProductsSpecial asSecurityProductsSpecial = new AsSecurityProductsSpecial();
                    //
                    BeanUtils.copyProperties(asSecurityProductExcel, asDeviceCommon);
                    BeanUtils.copyProperties(asSecurityProductExcel, asSecurityProductsSpecial);
                    //
                    //asDeviceCommon.setTypeId(asType.getId());
                    if (ObjectUtil.isNotEmpty(noIdMap.get(asDeviceCommon.getNo()))) {
                        asDeviceCommon.setId(noIdMap.get(asDeviceCommon.getNo()));
                        this.updateById(asDeviceCommon);
                        asSecurityProductsSpecial.setId(asSecurityProductsSpecialService.getOne(new QueryWrapper<AsSecurityProductsSpecial>().eq("as_id",asDeviceCommon.getId())).getId());
                        asSecurityProductsSpecial.setAsId(asDeviceCommon.getId());
                        asSecurityProductsSpecialService.updateById(asSecurityProductsSpecial);
                    } else //20221227资产号在DB不存在的设备：做一个记录
                        updateFailedList.add(asDeviceCommon.getNo());
                }
            }
            int updateSucessCount = excelList.size() - updateFailedList.size();
            if(CollUtil.isNotEmpty(updateFailedList))
                return "更新了" + updateSucessCount + "条设备信息；" + "此外，" + updateFailedList.stream().collect(Collectors.joining(","))+ "不存在，请先导入系统";
            else
                return "更新了" + updateSucessCount + "条设备信息";
        } else {
            if (importMode.equals("覆盖")) {
                List<AsDeviceCommon> list = this.list(new QueryWrapper<AsDeviceCommon>().in("no", excelList.stream().map(AsSecurityProductExcel::getNo).collect(Collectors.toList())));
                if (ObjectUtil.isNotEmpty(list)) {
                    dbList = list;
                    List<Integer> asIdList = list.stream().map(AsDeviceCommon::getId).collect(Collectors.toList());
                    this.removeByIds(asIdList);
                    asNetworkDeviceSpecialService.remove(new QueryWrapper<AsNetworkDeviceSpecial>().in("as_id", asIdList));
                }
                pageList = excelList;
            } else {
                List<AsDeviceCommon> list = this.list(new QueryWrapper<AsDeviceCommon>().in("no", excelList.stream().map(AsSecurityProductExcel::getNo).collect(Collectors.toList())));
                if (ObjectUtil.isEmpty(list)) {
                    pageList = excelList;
                } else {
                    dbList = list;
                    Set<String> noSet = list.stream().map(AsDeviceCommon::getNo).collect(Collectors.toSet());
                    pageList = excelList.stream().filter(item -> !noSet.contains(item.getNo())).collect(Collectors.toList());
                }
            }
            //
            List<AsDeviceCommon> asDeviceCommonList = new ArrayList<>();
            List<AsSecurityProductsSpecial> asSecurityProductsSpecialList = new ArrayList<>();

            //
            for (AsSecurityProductExcel asSecurityProductExcel : pageList) {
                if (ObjectUtil.isEmpty(asSecurityProductExcel.getNo())) {
                    throw new RuntimeException(asSecurityProductExcel.getName() + "的设备编号不能为空");
                }
                AsType asType = asTypeService.getAsType(asSecurityProductExcel.getTypeName());
                if (asType == null) {
                    throw new RuntimeException("设备类别不存在");
                }
                if (ObjectUtil.isEmpty(asSecurityProductExcel.getState())) {
                    throw new RuntimeException("设备状态不能为空");
                } else {
                    if(asSecurityProductExcel.getState().equals("在用") && ObjectUtil.isEmpty(asSecurityProductExcel.getMiji()) )
                        throw new RuntimeException("“在用”设备的密级不能为空");
                }
                AsDeviceCommon asDeviceCommon = new AsDeviceCommon();
                AsSecurityProductsSpecial asSecurityProductsSpecial = new AsSecurityProductsSpecial();
                //
                BeanUtils.copyProperties(asSecurityProductExcel, asDeviceCommon);
                BeanUtils.copyProperties(asSecurityProductExcel, asSecurityProductsSpecial);
                //
                asDeviceCommon.setTypeId(asType.getId());
                //20230213 添加 保密编号的非空判断
                if(ObjectUtil.isEmpty(asDeviceCommon.getBaomiNo())){
                    asDeviceCommon.setBaomiNo(this.makeBaomiNo(asType,asDeviceCommon.getMiji(),asDeviceCommon.getUseDate()));
                }
                this.save(asDeviceCommon);
                asSecurityProductsSpecial.setAsId(asDeviceCommon.getId());
                asSecurityProductsSpecialList.add(asSecurityProductsSpecial);
            }
            //
            asSecurityProductsSpecialService.saveBatch(asSecurityProductsSpecialList);
            //
            if (importMode.equals("覆盖")) {
                int pageCount = pageList.size() - dbList.size();
                return "导入" + pageCount + "条资产;设备编号：" + dbList.stream().map(AsDeviceCommon::getNo).collect(Collectors.joining(",")) + ",已经被覆盖";
            } else {
                int pageCount = pageList.size();
                if (ObjectUtil.isEmpty(dbList)) {
                    return "导入" + pageCount + "条资产";
                } else {
                    return "导入" + pageCount + "条资产;设备编号：" + dbList.stream().map(AsDeviceCommon::getNo).collect(Collectors.joining(",")) + "已经存在，未导入";
                }
            }

        }

    }

    private String addAsIoSpecialExcel(List<AsIoExcel> excelList, String importMode) {
        //去掉db中存在的设备，剩下页面上需要导入的设备
        List<AsIoExcel> pageList;
        //db中存在的设备
        List<AsDeviceCommon> dbList = new ArrayList<>();
        //处理日期类型
        ExcelDateUtil.converToDate(excelList, AsIoExcel.class);
        if (importMode.equals("更新")) {//20221227
            List<Integer> asTypeIdList = asTypeService.getTypeIdList(6);//约定了外设的typeid:6
            List<Map<String, Object>> listMaps = this.listMaps(new QueryWrapper<AsDeviceCommon>().in("type_id", asTypeIdList).select("no", "id"));
            List<String> updateFailedList  = new ArrayList<>();
            Map<String, Integer> noIdMap = new HashMap<>();
            for (Map<String, Object> map : listMaps) {
                noIdMap.put(map.get("no").toString(), Integer.valueOf(map.get("id").toString()));
            }
            if (ObjectUtil.isNotEmpty(excelList)) {
                for (AsIoExcel asIoExcel : excelList) {
                    if (ObjectUtil.isEmpty(asIoExcel.getNo())) {
                        throw new RuntimeException("设备编号不能为空");
                    }
                    AsDeviceCommon asDeviceCommon = new AsDeviceCommon();
                    AsIoSpecial asIoSpecial = new AsIoSpecial();
                    //
                    BeanUtils.copyProperties(asIoExcel, asDeviceCommon);
                    BeanUtils.copyProperties(asIoExcel, asIoSpecial);
                    //
                   // asDeviceCommon.setTypeId(asType.getId());
                    if (ObjectUtil.isNotEmpty(noIdMap.get(asDeviceCommon.getNo()))) {
                        asDeviceCommon.setId(noIdMap.get(asDeviceCommon.getNo()));
                        this.updateById(asDeviceCommon);
                        asIoSpecial.setId(asIoSpecialService.getOne(new QueryWrapper< AsIoSpecial>().eq("as_id",asDeviceCommon.getId())).getId());
                        asIoSpecial.setAsId(asDeviceCommon.getId());
                        asIoSpecialService.updateById(asIoSpecial);
                    } else //20221227资产号在DB不存在的设备：做一个记录
                        updateFailedList.add(asDeviceCommon.getNo());
                }
            }
            int updateSucessCount = excelList.size() - updateFailedList.size();
            if(CollUtil.isNotEmpty(updateFailedList))
                return "更新了" + updateSucessCount + "条设备信息；" + "此外，" + updateFailedList.stream().collect(Collectors.joining(","))+ "不存在，请先导入系统";
            else
                return "更新了" + updateSucessCount + "条设备信息";
        } else {
            if (importMode.equals("覆盖")) {
                List<AsDeviceCommon> list = this.list(new QueryWrapper<AsDeviceCommon>().in("no", excelList.stream().map(AsIoExcel::getNo).collect(Collectors.toList())));
                if (ObjectUtil.isNotEmpty(list)) {
                    dbList = list;
                    List<Integer> asIdList = list.stream().map(AsDeviceCommon::getId).collect(Collectors.toList());
                    this.removeByIds(asIdList);
                    asIoSpecialService.remove(new QueryWrapper<AsIoSpecial>().in("as_id", asIdList));
                }
                pageList = excelList;
            } else {
                List<AsDeviceCommon> list = this.list(new QueryWrapper<AsDeviceCommon>().in("no", excelList.stream().map(AsIoExcel::getNo).collect(Collectors.toList())));
                if (ObjectUtil.isEmpty(list)) {
                    pageList = excelList;
                } else {
                    dbList = list;
                    Set<String> noSet = list.stream().map(AsDeviceCommon::getNo).collect(Collectors.toSet());
                    pageList = excelList.stream().filter(item -> !noSet.contains(item.getNo())).collect(Collectors.toList());
                }
            }
            //
            List<AsDeviceCommon> asDeviceCommonList = new ArrayList<>();
            List<AsIoSpecial> asIoSpecialList = new ArrayList<>();

            //
            for (AsIoExcel asIoExcel : pageList) {
                if (ObjectUtil.isEmpty(asIoExcel.getNo())) {
                    throw new RuntimeException(asIoExcel.getName() + "的设备编号不能为空");
                }
                AsType asType = asTypeService.getAsType(asIoExcel.getTypeName());
                if (asType == null) {
                    throw new RuntimeException("设备类别不存在");
                }
                if (ObjectUtil.isEmpty( asIoExcel.getState())) {
                    throw new RuntimeException("设备状态不能为空");
                } else {
                    if( asIoExcel.getState().equals("在用") && ObjectUtil.isEmpty(asIoExcel.getMiji()))
                        throw new RuntimeException("“在用”设备的密级不能为空");
                }
                AsDeviceCommon asDeviceCommon = new AsDeviceCommon();
                AsIoSpecial asIoSpecial = new AsIoSpecial();
                //
                BeanUtils.copyProperties(asIoExcel, asDeviceCommon);
                BeanUtils.copyProperties(asIoExcel, asIoSpecial);
                //
                asDeviceCommon.setTypeId(asType.getId());
                //20230213 添加 保密编号的非空判断
                if(ObjectUtil.isEmpty(asDeviceCommon.getBaomiNo())){
                    asDeviceCommon.setBaomiNo(this.makeBaomiNo(asType,asDeviceCommon.getMiji(),asDeviceCommon.getUseDate()));
                }
                this.save(asDeviceCommon);
                asIoSpecial.setAsId(asDeviceCommon.getId());
                asIoSpecialList.add(asIoSpecial);
            }
            //
            asIoSpecialService.saveBatch(asIoSpecialList);
            //
            if (importMode.equals("覆盖")) {
                int pageCount = pageList.size() - dbList.size();
                return "导入" + pageCount + "条资产;设备编号：" + dbList.stream().map(AsDeviceCommon::getNo).collect(Collectors.joining(",")) + ",已经被覆盖";
            } else {
                int pageCount = pageList.size();
                if (ObjectUtil.isEmpty(dbList)) {
                    return "导入" + pageCount + "条资产";
                } else {
                    return "导入" + pageCount + "条资产;设备编号：" + dbList.stream().map(AsDeviceCommon::getNo).collect(Collectors.joining(",")) + "已经存在，未导入";
                }
            }
        }
    }

    private String addStorageExcel(List<StorageExcel> excelList, String importMode) {
        //去掉db中存在的设备，剩下页面上需要导入的设备
        List<StorageExcel> pageList;
        //db中存在的设备
        List<AsDeviceCommon> dbList = new ArrayList<>();
        //处理日期类型
        ExcelDateUtil.converToDate(excelList, StorageExcel.class);
        if (importMode.equals("更新")) {//20221227
            List<Integer> asTypeIdList = asTypeService.getTypeIdList(31);//约定了存储介质的typeid:31;20230424 todo把“其他”类型也加入这里
            List<Map<String, Object>> listMaps = this.listMaps(new QueryWrapper<AsDeviceCommon>().in("type_id", asTypeIdList).select("no", "id"));
            List<String> updateFailedList  = new ArrayList<>();
            Map<String, Integer> noIdMap = new HashMap<>();
            for (Map<String, Object> map : listMaps) {
                noIdMap.put(map.get("no").toString(), Integer.valueOf(map.get("id").toString()));
            }
            if (ObjectUtil.isNotEmpty(excelList)) {
                for (StorageExcel storageExcel : excelList) {
                    if (ObjectUtil.isEmpty(storageExcel.getNo())) {
                        throw new RuntimeException("设备编号不能为空");
                    }
                    AsDeviceCommon asDeviceCommon = new AsDeviceCommon();
                    //
                    BeanUtils.copyProperties(storageExcel, asDeviceCommon);
//                    asDeviceCommon.setTypeId(asType.getId());
                    if (ObjectUtil.isNotEmpty(noIdMap.get(asDeviceCommon.getNo()))) {
                        asDeviceCommon.setId(noIdMap.get(asDeviceCommon.getNo()));
                        this.updateById(asDeviceCommon);
                    } else //20221227资产号在DB不存在的设备：做一个记录
                        updateFailedList.add(asDeviceCommon.getNo());
                }
            }
            int updateSucessCount = excelList.size() - updateFailedList.size();
            if(CollUtil.isNotEmpty(updateFailedList))
                return "更新了" + updateSucessCount + "条设备信息；" + "此外，" + updateFailedList.stream().collect(Collectors.joining(","))+ "不存在，请先导入系统";
            else
                return "更新了" + updateSucessCount + "条设备信息";
        } else {
            if (importMode.equals("覆盖")) {
                List<AsDeviceCommon> list = this.list(new QueryWrapper<AsDeviceCommon>().in("no", excelList.stream().map(StorageExcel::getNo).collect(Collectors.toList())));
                if (ObjectUtil.isNotEmpty(list)) {
                    dbList = list;
                    List<Integer> asIdList = list.stream().map(AsDeviceCommon::getId).collect(Collectors.toList());
                    this.removeByIds(asIdList);
                }
                pageList = excelList;
            } else {
                List<AsDeviceCommon> list = this.list(new QueryWrapper<AsDeviceCommon>().in("no", excelList.stream().map(StorageExcel::getNo).collect(Collectors.toList())));
                if (ObjectUtil.isEmpty(list)) {
                    pageList = excelList;
                } else {
                    dbList = list;
                    Set<String> noSet = list.stream().map(AsDeviceCommon::getNo).collect(Collectors.toSet());
                    pageList = excelList.stream().filter(item -> !noSet.contains(item.getNo())).collect(Collectors.toList());
                }
            }
            //
            List<AsDeviceCommon> asDeviceCommonList = new ArrayList<>();
            for (StorageExcel storageExcel : pageList) {
                if (ObjectUtil.isEmpty(storageExcel.getNo())) {
                    throw new RuntimeException(storageExcel.getName() + "的设备编号不能为空");
                }
                AsType asType = asTypeService.getAsType(storageExcel.getTypeName());
                if (asType == null) {
                    throw new RuntimeException("设备类别不存在");
                }
                if (ObjectUtil.isEmpty(storageExcel.getState())) {
                    throw new RuntimeException("设备状态不能为空");
                } else {
                    if (storageExcel.getState().equals("在用") && ObjectUtil.isEmpty(storageExcel.getMiji()))
                        throw new RuntimeException("“在用”设备的密级不能为空");
                }
                AsDeviceCommon asDeviceCommon = new AsDeviceCommon();
                //
                BeanUtils.copyProperties(storageExcel, asDeviceCommon);
                //
                asDeviceCommon.setTypeId(asType.getId());
                //20230213 添加 保密编号的非空判断
                if(ObjectUtil.isEmpty(asDeviceCommon.getBaomiNo())){
                    asDeviceCommon.setBaomiNo(this.makeBaomiNo(asType,asDeviceCommon.getMiji(),asDeviceCommon.getUseDate()));
                }
                this.save(asDeviceCommon);
            }
            if (importMode.equals("覆盖")) {
                int pageCount = pageList.size() - dbList.size();
                return "导入" + pageCount + "条资产;设备编号：" + dbList.stream().map(AsDeviceCommon::getNo).collect(Collectors.joining(",")) + ",已经被覆盖";
            } else {
                int pageCount = pageList.size();
                if (ObjectUtil.isEmpty(dbList)) {
                    return "导入" + pageCount + "条资产";
                } else {
                    return "导入" + pageCount + "条资产;设备编号：" + dbList.stream().map(AsDeviceCommon::getNo).collect(Collectors.joining(",")) + "已经存在，未导入";
                }
            }
        }
    }
    //是否覆盖原资产信息，是=不判重，只提示导入多少个资产，否=判重，提示导入多少个资产;设备编号xx、yy已经存在，未导入。
    @Override
    public List<String> addExcel(List<AsComputerExcel> list0, List<AsNetworkDeviceExcel> list1, List<AsSecurityProductExcel> list2, List<AsIoExcel> list3, List<StorageExcel> list4, String importMode) {
        List<String> resultList = new ArrayList<>();
        //
        if (ObjectUtil.isNotEmpty(list0)) {
            resultList.add("计算机:" + addAsComputerExcel(list0, importMode));
        }
        if (ObjectUtil.isNotEmpty(list1)) {
            resultList.add("网络设备:" + addAsNetworkDeviceSpecialExcel(list1, importMode));
        }
        if (ObjectUtil.isNotEmpty(list2)) {
            resultList.add("安全产品:" + addAsSecurityProductsSpecialExcel(list2, importMode));
        }
        if (ObjectUtil.isNotEmpty(list3)) {
            resultList.add("外部设备:" + addAsIoSpecialExcel(list3, importMode));
        }
        if (ObjectUtil.isNotEmpty(list4)) {
            resultList.add("存储介质:" + addStorageExcel(list4, importMode));
        }
        //
        if (ObjectUtil.isEmpty(resultList)) {
            resultList.add("无设备数据被导入");
        }
        return resultList;
    }
}
