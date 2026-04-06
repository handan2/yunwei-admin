

package com.sss.yunweiadmin.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.sss.yunweiadmin.common.config.GlobalParam;
import com.sss.yunweiadmin.common.utils.ExcelDateUtil;
import com.sss.yunweiadmin.mapper.AsDeviceCommonMapper;
import com.sss.yunweiadmin.model.entity.*;
import com.sss.yunweiadmin.model.excel.*;
import com.sss.yunweiadmin.model.vo.AssetVO;
import com.sss.yunweiadmin.model.vo.RepeaterForAssetListVO;
import com.sss.yunweiadmin.service.*;
import net.sf.cglib.core.Local;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpSession;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
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
    @Autowired
    AsConfigService asConfigService;
    @Autowired
    ProcessInstanceChangeService processInstanceChangeService;
    @Autowired
    HttpSession httpSession;
    @Autowired
    SapAssetService sapAssetService;
    @Autowired
    ProcessFormCustomTypeService  processFormCustomTypeService;
    @Autowired
    ProcessInstanceDataService processInstanceDataService;
    @Autowired
    ProcessFormValue2Service processFormValue2Service;
    @Autowired
    InspectionService inspectionService;



    @Override
    //20250625 用于每月自查计算机列表
    public List<AsDeviceCommon> list( String no, Integer typeId, String name, String netType, String state, Integer userDept, String userName, String miji, Integer customTableId, String processName, String sn, String haveInspect) {


        QueryWrapper<AsDeviceCommon> queryWrapper = new  QueryWrapper<AsDeviceCommon>().eq("org_id",GlobalParam.orgId);
        String deptName = "";
         if (ObjectUtil.isNotEmpty(userDept)) {
            deptName = sysDeptService.getById(userDept).getName();
            queryWrapper.eq("user_dept", deptName);
        }
        LocalDate today = LocalDate.now(); // 获取当前日期
        //20240901 年度内检查
        if (ObjectUtil.isNotEmpty(haveInspect)){
            System.out.println("20241110in-year inspect  checkCondition start ----");
            System.out.println(LocalDateTime.now());

            LocalDate firstDayOfYear = today.withDayOfYear(1).withYear(today.getYear());//获取今年的第一天
            int year = today.getYear();
            LocalDate firstDayOfNextYear = LocalDate.ofYearDay(year + 1, 1);//获取明年的第一天
            QueryWrapper<Inspection> queryWrapperForInspect = new  QueryWrapper<Inspection>().eq("org_id",GlobalParam.orgId).eq("org_id",GlobalParam.orgId);
            queryWrapperForInspect.select("no");
            queryWrapperForInspect.ge("inspect_date", firstDayOfYear);
            queryWrapperForInspect.le("inspect_date", firstDayOfNextYear);

            List<Map<String, Object>> listMaps = inspectionService.listMaps(queryWrapperForInspect);

            List<String> nosForInspect = listMaps.stream().map(item -> String.valueOf(item.get("no"))).collect(Collectors.toList());
            if("是".equals(haveInspect)){
                queryWrapper.in("no",nosForInspect);
            } else
                queryWrapper.notIn("no",nosForInspect);
            System.out.println("20241110in-year inspect  checkCondition end ----");
            System.out.println(LocalDateTime.now());

        }
        List<AsDeviceCommon> list = new ArrayList<>();
        List<Integer> typeIdList = asTypeService.getTypeIdList(GlobalParam.typeIDForCMP);//限定计算机与服务器

        //查询开网口的涉密单机
        QueryWrapper<AsDeviceCommon> queryWrapperForNet = new  QueryWrapper<AsDeviceCommon>().eq("user_dept", deptName).like("net_type","未联网").in("type_id", typeIdList);
        //BeanUtils.copyProperties(queryWrapper, queryWrapperForNet);
        queryWrapperForNet.in("miji",Arrays.asList("机密","秘密")).eq("state","在用");
        List<AsComputerGranted> listForGrantedForNet  = asComputerGrantedService.list(new  QueryWrapper<AsComputerGranted>().eq("org_id",GlobalParam.orgId).eq("net_interface","开启"));
        if(CollUtil.isNotEmpty(listForGrantedForNet)){
            queryWrapperForNet.in("id",listForGrantedForNet.stream().map(AsComputerGranted::getAsId).collect(Collectors.toList()));
            List<AsDeviceCommon> asDeviceCommonListForShemiNet = this.list(queryWrapperForNet);
            asDeviceCommonListForShemiNet.forEach(item->item.setTemp("开网口涉密单机"));
            list.addAll(asDeviceCommonListForShemiNet);
        }
        //查询本月新上账|申领计算机
        QueryWrapper<AsDeviceCommon> queryWrapperForNew = new  QueryWrapper<AsDeviceCommon>().eq("user_dept", deptName).in("type_id", typeIdList);
        //BeanUtils.copyProperties(queryWrapper, queryWrapperForNew);
        LocalDate firstDayOfMonth = today.withDayOfMonth(1);
        queryWrapperForNew.ge("create_datetime",firstDayOfMonth);
        List<AsDeviceCommon> asDeviceCommonListForNew = this.list(queryWrapperForNew);
        asDeviceCommonListForNew.forEach(item->item.setTemp("新上账设备"));
        list.addAll(asDeviceCommonListForNew);

        //本月维修计算机
        QueryWrapper<AsDeviceCommon> queryWrapperForRepair = new  QueryWrapper<AsDeviceCommon>().eq("user_dept", deptName).in("type_id", typeIdList);
        //BeanUtils.copyProperties(queryWrapper, queryWrapperForRepair);
        List<ProcessInstanceData> processInstanceDataList = processInstanceDataService.list(new  QueryWrapper<ProcessInstanceData>().eq("org_id",GlobalParam.orgId).eq("dept_name",deptName).like("process_name","计算机维修").ge("start_datetime",firstDayOfMonth));
        if(CollUtil.isNotEmpty(processInstanceDataList)){
            List<ProcessFormValue2> processFormValue2List = processFormValue2Service.list(new  QueryWrapper<ProcessFormValue2>().eq("org_id",GlobalParam.orgId).in("act_process_instance_id",processInstanceDataList.stream().map(item->item.getActProcessInstanceId()).collect(Collectors.toList())));

            List<AsDeviceCommon> asDeviceCommonListForRepair = this.list( queryWrapperForRepair.in("id",processFormValue2List.stream().map(item->item.getAsId()).collect(Collectors.toList())));
            asDeviceCommonListForRepair.forEach(item->item.setTemp("维修"));
            list.addAll(asDeviceCommonListForRepair);
        }

        //




        System.out.println("20241110in-year inspect  return page ----");
        System.out.println(LocalDateTime.now());
       // List<AsDeviceCommon> list = this.list(queryWrapper);

        return list;

    }


    @Override
    public Boolean saveSapAsset(List<SapAsset> assetList){
        //SAP属性值与运维设备属性值的映射关系：类别、状态、密级（这个估计不用）
        // 类型映射关系
        /*类别映射存在的问题：
        1.SAP类型ID与运维类型不一一对应，理论上可能会把任意id推送给我
        2.结合1，组合判断类型:先通过（预先设定好的几个）类型ID映射给出设备类型，（前面没有命中的话）然后再通过eqktx（设备名称）再判断一轮（这部分判断放在反射遍历时执行）
        3.20250706 sap推来的类型字段（eqart）已经没有下述的“100023l/10000”之类，比如“工作站”推了“J0901”，而“办公计算机”对应字段为空，
         todo 问付宝龙，新对接的Saq接口工程师微信，并且要下相应的类型枚举值（之前的在PC上也没有找到相应记录文档）*/
        Map<String, String> mappingForType = new HashMap<>();
        mappingForType.put("10000", "打印机");//映射成中文类型名吧
        mappingForType.put("100023l", "办公计算机");

        //sap与运维字段的映射
        Map<String, String> mappingForTypeForSAPYunwei = MapUtil.<String, String>builder()
                .put("invnr","no")
                .put("zjlydsc","fundSrc")
                .put("zzsfsm","miji")
                .put("inbdt","buyDate")
//                .put("erdat","buyDate")
//                .put("inbdt","buyDate")
                .put("herst","manufacturer")
                .put("typbz","model")
                .put("serge","sn")
                .put("pltxt","location") //todo问工程师，SAP楼层与房间号更新，我反向更新有没问题？
                .put("fing","userDept")
                .put("zzrp","userName")
                .put("zzszbm","szbm") //20250319上账部门和上账人，
                .put("zzszr","szr")
                .build();
        List<AsType> asTypeList = asTypeService.list(new  QueryWrapper<AsType>().eq("org_id",GlobalParam.orgId));
        Map<String, Integer> typeMap = new HashMap<>();
        asTypeList.stream().forEach(item->{
            typeMap.put(item.getName(),item.getId());
        });
        //用于写入运维
        List<AsDeviceCommon> asDeviceCommonListForCMP = new ArrayList<>();
        List<AsDeviceCommon> asDeviceCommonListForAff = new ArrayList<>();
        List<AsDeviceCommon> asDeviceCommonListForNet = new ArrayList<>();
        List<AsComputerSpecial> asComputerSpecialList = new ArrayList<>();
        List<AsComputerGranted> asComputerGrantedList = new ArrayList<>();
        List<AsNetworkDeviceSpecial> asNetworkDeviceSpecialList = new ArrayList<>();
        List<AsIoSpecial> asIoSpecialList = new ArrayList<>();
        //查询已有设备ID
        List<Map<String, Object>> listMaps1 = this.listMaps(new  QueryWrapper<AsDeviceCommon>().eq("org_id",GlobalParam.orgId).ne("type_id",GlobalParam.typeIDForDisk).select("no"));
        List<String> noListForYunweiAs = listMaps1.stream().map(item -> item.get("no").toString()).collect(Collectors.toList());

        for (SapAsset asset : assetList) {
            //20250308 todo 对字段值进行过滤和“转译”
            // 获取对象的所有属性
            asset.setCreateTime(LocalDateTime.now());
            Field[] fields = ReflectUtil.getFields(asset.getClass());
            //下述sap中的两个字段要需要经过复杂转化：才能被运维记录
            String SAPName = "";//设备名称（用于转化的临时字段值）
            String SAPNetType = "";//联网类别（用于转化的临时字段值）
            // 遍历属性
            for (java.lang.reflect.Field field : fields) {
                // 获取属性名
                String fieldName = field.getName();
                // 获取属性值
                Object fieldValue = ReflectUtil.getFieldValue(asset, fieldName);

                if ("eqktx".equals(fieldName)) {//设备名称
                    SAPName = (String) fieldValue;
                } else if ("zzlwzl".equals(fieldName)) {//联网类别
                    SAPNetType = (String) fieldValue;
                }
            }
            // StringWrapper wrapperForSAPName = new StringWrapper("SAPName");
            AsDeviceCommon asDeviceCommon = new AsDeviceCommon();//sap记录属性值转化后，待塞到DB相应字段中:注意，目前只有"no"不同的才真正添加到运维的设备表中
            //以下的初始值设置也只是在“目前只考虑新增”的设定中有意义
            asDeviceCommon.setName(SAPName);
            asDeviceCommon.setNetType(SAPNetType);
            asDeviceCommon.setState("库存");
            asDeviceCommon.setTypeId(typeMap.get("其他"));//类别初始值


            // 遍历属性
            for (java.lang.reflect.Field field : fields) {
                // 获取属性名
                String fieldName = field.getName();
                // 获取属性值
                Object fieldValue = ReflectUtil.getFieldValue(asset, fieldName);
                if (ObjectUtil.isNotEmpty(fieldValue)) {
                    if ("eqart".equals(fieldName)) {//类别字段的处理
                        String typeName = mappingForType.get(fieldValue);

                        if (false) {//ObjectUtil.isNotEmpty(typeName)  类别字段： 20250915偶尔有值（比如J/JO501），但不研了 ; 20250706这个类别映射表中值已经失效（详见上面相关注释），所以这个分支现在均为false
                            //  ReflectUtil.setFieldValue(asset,fieldName,typeMap.get(typeName));//asset的类别Id还是不要改了
                            ReflectUtil.setFieldValue(asDeviceCommon, "typeId", typeMap.get(typeName));

                        } else {//TODO根据名称字段eqktx来判断一波：逻辑有问题&这里涉及到组合判断：需要两轮遍历：第一轮先把”类别“|”名称“|”联网类别“读出来
                            String targetStr = "Hello, World!";
                            String[] namesForCMP = {"计算机","单机", "工控机","测试设备", "一体机", "笔记本", "终端", "便携机", "台式机"};//, "工作站"
                            //boolean containsAny = Arrays.stream(namesForCMP).anyMatch(str -> StrUtil.contains(targetStr, str));
                            String SAPName1 = SAPName;//因为下面的箭头函数里的不能使用“更改过值的”变量

                            if (Arrays.stream(namesForCMP).anyMatch(str -> StrUtil.contains(SAPName1, str))) {// 计算机得单拿出来：因为运维里的计算机分类也是很特殊的
                                if (SAPNetType.contains("国密网") || SAPNetType.contains("商密网"))
                                    ReflectUtil.setFieldValue(asDeviceCommon, "typeId", typeMap.get("办公计算机"));//ReflectUtil.setFieldValue(asset,fieldName,typeMap.get("办公计算机"));
                                else if (SAPNetType.contains("试验网"))
                                    ReflectUtil.setFieldValue(asDeviceCommon, "typeId", typeMap.get("测试设备"));//ReflectUtil.setFieldValue(asset,fieldName,typeMap.get("测试设备"));
                                else if (SAPName.contains("便携机") || SAPName.contains("笔记本"))
                                    ReflectUtil.setFieldValue(asDeviceCommon, "typeId", typeMap.get("便携机"));
                                else
                                    ReflectUtil.setFieldValue(asDeviceCommon, "typeId", typeMap.get("计算机"));////这种只能手工在运维里改变分类
                            } else {
                                //20250708 遍历一下typeMap，每一次遍历都判断SAPName1里是不是contains typeName,如果有的话，设置asDeviceCommon相关typeid字段
                                typeMap.entrySet().stream().forEach(entry -> {
                                    if (SAPName1.contains(entry.getKey())) {
                                        ReflectUtil.setFieldValue(asDeviceCommon, "typeId", entry.getValue());
                                    }
                                });
                            }
                        }
                    } else if (ObjectUtil.isNotEmpty(mappingForTypeForSAPYunwei.get(fieldName))) { //给asDeviceCommon相应字段赋值
                        if ("inbdt".equals(fieldName)) {//日期转换  || "erdat".equals(fieldName) || "aedat".equals(fieldName)
                            if ((fieldValue).equals("00000000")) {//sAP里空白的日期会以"00000000"记录导致解析报错，但是后来他们改成“空白”
                                System.out.println("日期类型的值出现了：" + fieldValue);
                            } else {
                                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
                                LocalDate localDate = LocalDate.parse((String) fieldValue, formatter);
                                fieldValue = localDate ;//本次循环里，把这个值替换下，todo验证 ReflectUtil.setFieldValue里Set“各种类型值”是不是没问题
//
                            }

                        }
                        ReflectUtil.setFieldValue(asDeviceCommon, mappingForTypeForSAPYunwei.get(fieldName), fieldValue);
                        // 输出属性名和属性值
                        System.out.println("属性名: " + fieldName + ", 属性值: " + fieldValue);
                    }

                }

            }
            //俟写入新增的"no":todo 对已有的修改待后续做
            if (ObjectUtil.isNotEmpty(asDeviceCommon.getNo()) && !noListForYunweiAs.contains(asDeviceCommon.getNo())) {
                asDeviceCommon.setCreateDatetime(LocalDateTime.now());
                asDeviceCommon.setSource("SAP");
                //asDeviceCommonListForCMP.add(asDeviceCommon);
                this.save(asDeviceCommon);//基本表建立
                AsType asType = asTypeService.getLevel2AsTypeById(asDeviceCommon.getTypeId());
                //注：只对能识别的几类设备创建专用表，其他的仅建立基本表：后续手工人工审核处理
                if(asType.getId() == GlobalParam.typeIDForCMP || asType.getId() == GlobalParam.typeIDForFWQ ){
                    //思考一个问题：何时初始化这些属性：感觉应该在“申领”后：todo有时间完整
                    AsComputerGranted asComputerGranted = new AsComputerGranted();
                    asComputerGranted.setAsId(asDeviceCommon.getId());
                    asComputerGrantedList.add(asComputerGranted);
                    AsComputerSpecial asComputerSpecial = new AsComputerSpecial();
                    asComputerSpecial.setAsId(asDeviceCommon.getId());
                    asComputerSpecialList.add(asComputerSpecial);
                } else if(asType.getId() == GlobalParam.typeIDForAff){
                    AsIoSpecial asIoSpecial = new AsIoSpecial();
                    asIoSpecial.setAsId(asDeviceCommon.getId());
                    asIoSpecialList.add(asIoSpecial);
                } else if(asType.getId() == GlobalParam.typeIDForNET){
                    AsNetworkDeviceSpecial asNetworkDeviceSpecial = new AsNetworkDeviceSpecial();
                    asNetworkDeviceSpecial.setAsId(asDeviceCommon.getId());
                    asNetworkDeviceSpecialList.add(asNetworkDeviceSpecial);
                }

            }
            System.out.println(asset);
        }
        //写入专用表
        if(CollUtil.isNotEmpty(asComputerSpecialList))
            asComputerSpecialService.saveBatch(asComputerSpecialList);
        if(CollUtil.isNotEmpty(asComputerGrantedList))
            asComputerGrantedService.saveBatch(asComputerGrantedList);
        if(CollUtil.isNotEmpty(asIoSpecialList))
            asIoSpecialService.saveBatch(asIoSpecialList);
        if(CollUtil.isNotEmpty(asNetworkDeviceSpecialList))
            asNetworkDeviceSpecialService.saveBatch(asNetworkDeviceSpecialList);

        //写入sapAsset
        int m = 100;
        for (int i = 0; i < assetList.size(); i += m) {
            List<SapAsset> assetList1=  new ArrayList<>(assetList.subList(i, Math.min(i + m, assetList.size())));
            sapAssetService.saveBatch(assetList1);
        }
        //sapAssetService.saveBatch(assetList);


        return true;

       


    }
    //20230213
    @Override
    public String makeBaomiNo(AsType asType, String miji, LocalDate useDate) {
        String baomiNo = "0094";
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter fmt_now;
        String dateStr;
        if (ObjectUtil.isNotEmpty(useDate)) {//如果有”启用日期“，那么保密编号的年份就以这个为准；暂不考虑”更新“模式导入资产EXCEl的情况
            DateTimeFormatter fmt_useDate = DateTimeFormatter.ofPattern("yyyy");
            fmt_now = DateTimeFormatter.ofPattern("MMdd");
            dateStr = useDate.format(fmt_useDate) + now.format(fmt_now);

        } else {
            fmt_now = DateTimeFormatter.ofPattern("yyyyMMdd");
            dateStr = now.format(fmt_now);

        }


        Random rnd = new Random();
        int code = rnd.nextInt(10000000) + 1000000;
        String randomNum = Integer.toString(code);

        AsType asTypeLevel2 = asTypeService.getLevel2AsTypeById(asType.getId());

        if (asType.getId() == GlobalParam.typeIDForPrint) {//打印机
            baomiNo = baomiNo + "D";
        } else if (asType.getId() == GlobalParam.typeIDForPrint)//复印机
            baomiNo = baomiNo + "F";
        else if (asTypeLevel2.getId() == GlobalParam.typeIDForStor)//存储介质：通过二级类型ID判断
            baomiNo = baomiNo + "M";
        else if (asTypeLevel2.getId() ==  GlobalParam.typeIDForSafe)//安全产品：通过二级类型ID判断
            baomiNo = baomiNo + "A";
            //20230213“网络外设”与“办公自动化”根据联网类别分开：20230214暂不分，统称B
        else if (asTypeLevel2.getId() ==  GlobalParam.typeIDForAff) //外设办公自动化：通过二级类型ID判断
            baomiNo = baomiNo + "B";
        else if (asTypeLevel2.getId() ==  GlobalParam.typeIDForCMP) //20230429 计算机：通过二级类型ID判断
            baomiNo = baomiNo + "J";
        else if (asTypeLevel2.getId() ==  GlobalParam.typeIDForFWQ) //20230429 服务器与存储设备：通过二级类型ID判断
            baomiNo = baomiNo + "S";
        else
            baomiNo = baomiNo + "Q";
        baomiNo = baomiNo + dateStr + randomNum;
        if ("机密".equals(miji))
            baomiNo = baomiNo + "5";
        else if ("秘密".equals(miji))
            baomiNo = baomiNo + "3";
        else if ("普通商密".equals(miji))
            baomiNo = baomiNo + "0";
        else if ("非密".equals(miji))
            baomiNo = baomiNo + "2";
        else //无任何密级的情况：其实此时可以在打印标签时对此类密级情况屏敝
            baomiNo = baomiNo + "X";
        return baomiNo;
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
    private void callByAddComputer(String sn, String model, String size, AsDeviceCommon asDeviceCommon, List<AsDeviceCommon> asDeviceCommonListForDiskAdd, List<AsDeviceCommon> asDeviceCommonListForDiskUpdate, Map<String, Integer> ypNoIdMap) {
        String[] sns = sn.split("\\,");
        String[] models = new String[0];
        String[] sizes = new String[0];
        if (ObjectUtil.isNotEmpty(model))
            models = model.split("\\,");
        if (ObjectUtil.isNotEmpty(size))
            sizes = size.split("\\,");
        for (int i = 0; i < sns.length; i++) {
            AsDeviceCommon asDeviceCommon1 = new AsDeviceCommon();
            asDeviceCommon1.setSn(sns[i]);
            if (i < models.length)
                asDeviceCommon1.setModel(models[i]);
            if (i < sizes.length)
                asDeviceCommon1.setPrice(Integer.valueOf(sizes[i]));
            asDeviceCommon1.setUserName(asDeviceCommon.getUserName());
            asDeviceCommon1.setUserDept(asDeviceCommon.getUserDept());
            asDeviceCommon1.setMiji(asDeviceCommon.getMiji());
            asDeviceCommon1.setNetType(asDeviceCommon.getNetType());
            asDeviceCommon1.setUserMiji(asDeviceCommon.getUserMiji());//20221115
            asDeviceCommon1.setState(asDeviceCommon.getState());//20221115
            asDeviceCommon1.setName("硬盘");
            asDeviceCommon1.setTypeId(GlobalParam.typeIDForDisk);
            if (ObjectUtil.isNotEmpty(ypNoIdMap.get(sns[i]))) {//硬盘在DB中存在，则更新此硬盘信息
                asDeviceCommon1.setId(ypNoIdMap.get(sns[i]));
                asDeviceCommonListForDiskUpdate.add(asDeviceCommon1);
            } else {//硬盘在DB不存在：则新增
                LocalDateTime date = LocalDateTime.now();
                DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
                //防止序列号重复
                ThreadUtil.sleep(1);

                String dateStr = date.format(fmt);
                asDeviceCommon1.setNo("YP" + dateStr);
                asDeviceCommon1.setTypeId(GlobalParam.typeIDForDisk);
                asDeviceCommon1.setHostAsId(asDeviceCommon.getId());
                asDeviceCommonListForDiskAdd.add(asDeviceCommon1);
            }
        }
    }

    //20211115 不管啥类型，把所有专有表清清
    @Override
    public boolean delete(Integer[] idArr1) {
        List<Integer> idArr = Stream.of(idArr1).collect(Collectors.toList());
        asComputerSpecialService.remove(new  QueryWrapper<AsComputerSpecial>().eq("org_id",GlobalParam.orgId).in("as_id", idArr));
        asNetworkDeviceSpecialService.remove(new  QueryWrapper<AsNetworkDeviceSpecial>().eq("org_id",GlobalParam.orgId).in("as_id", idArr));
        asComputerGrantedService.remove(new  QueryWrapper<AsComputerGranted>().eq("org_id",GlobalParam.orgId).in("as_id", idArr));
        asIoSpecialService.remove(new  QueryWrapper<AsIoSpecial>().eq("org_id",GlobalParam.orgId).in("as_id", idArr));
        asSecurityProductsSpecialService.remove(new  QueryWrapper<AsSecurityProductsSpecial>().eq("org_id",GlobalParam.orgId).in("as_id", idArr));
        asApplicationSpecialService.remove(new  QueryWrapper<AsApplicationSpecial>().eq("org_id",GlobalParam.orgId).in("as_id", idArr));
        //删除硬盘信息
        List<AsDeviceCommon> diskList = this.list(new  QueryWrapper<AsDeviceCommon>().eq("org_id",GlobalParam.orgId).in("host_as_id", idArr));
        this.removeByIds(diskList.stream().map(item -> item.getId()).collect(Collectors.toList()));
        //删除asDeviceCommon“主设备”
        this.removeByIds(idArr);
        return true;
    }

    private Statistics classifyStatics(LocalDate date_end, List<Integer> listAsId, String miji, String period, int asTypeId) {
        LocalDate date_now = LocalDate.now();
        // List<Integer> listAsId = listMap.stream().map(item->Integer.parseInt(item.get("id").toString())).collect(Collectors.toList());
        //指定周期内&&计算机类别的操作系统安装数量
        Statistics statistics = new Statistics();
        statistics.setPeriod(period);
        statistics.setAsTypeId(asTypeId);
        statistics.setMiji(miji);
        statistics.setCreateTime(LocalDateTime.now());
        statistics.setAmount(listAsId.size());
        if (CollUtil.isNotEmpty(listAsId)) {//queryRapper/in语句中参数中：list为0会报错
            List<AsComputerSpecial> listAsComputerSpecialOSdateFiltered = asComputerSpecialService.list(new  QueryWrapper<AsComputerSpecial>().eq("org_id", GlobalParam.orgId).between("os_date", date_end, date_now).in("as_id", listAsId));
            statistics.setReinstallAmount(listAsComputerSpecialOSdateFiltered.size());
        } else {
            statistics.setReinstallAmount(0);
        }
        return statistics;
        //  statisticsService.save(statistics);
    }

    @Override
    public boolean addStatistics() {//目前只考虑涉密：密级后续由参数传进来，不全部都统计
        statisticsService.remove(new  QueryWrapper<Statistics>().eq("org_id",GlobalParam.orgId).eq("org_id",GlobalParam.orgId));//清空所有记录
        List<Statistics> statisticsList = new ArrayList<>();
        //先增加“桌面计算机” 的本年度重装查询
        LocalDate date_now = LocalDate.now();
        LocalDate date_end;
        Map<String, Integer> periodMap = ImmutableMap.of("本年度", 365, "本季度", 91, "本月", 30);//<周期，“回退”天数>
        for (Map.Entry<String, Integer> entryPeriod : periodMap.entrySet()) {
            date_end = date_now.minusDays(entryPeriod.getValue());
            Integer[] allowedTypeIdArray = {23, 22, 21, 8, 9};//需要记录的typeId
            List<Integer> list = Arrays.asList(allowedTypeIdArray);
            List<AsDeviceCommon> asDeviceCommonList = this.list(new  QueryWrapper<AsDeviceCommon>().eq("org_id",GlobalParam.orgId).in("type_id", list).ne("state", "停用").ne("state", "报废").ne("state", "库存").and(qw -> qw.eq("miji", "秘密").or().eq("miji", "机密")));
            Map<Integer, List<Integer>> mapList = new HashMap<>();//<类型ID，资产IdList>
            for (Integer typeId : allowedTypeIdArray) {
                mapList.put(typeId, new ArrayList<Integer>());
            }
            //组装mapList
            for (AsDeviceCommon a : asDeviceCommonList) {
                mapList.get(a.getTypeId()).add(a.getId());
            }
            //遍历mapList
            for (Map.Entry<Integer, List<Integer>> entry : mapList.entrySet()) {
                Statistics statistics = classifyStatics(date_end, entry.getValue(), "涉密", entryPeriod.getKey(), entry.getKey());
                if (ObjectUtil.isNotEmpty(statistics))
                    statisticsList.add(statistics);
            }
        }
        if (CollUtil.isNotEmpty(statisticsList))
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
        if (asTypeId ==  GlobalParam.typeIDForCMP|| asTypeId ==  GlobalParam.typeIDForFWQ) {//20230107 添加服务器与存储设备id
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
        } else if (asTypeId ==  GlobalParam.typeIDForNET) {
            //网络设备
            AsNetworkDeviceSpecial asNetworkDeviceSpecial = assetVO.getAsNetworkDeviceSpecial();
            if (ObjectUtil.isNotEmpty(asNetworkDeviceSpecial)) {
                asNetworkDeviceSpecial.setAsId(asDeviceCommon.getId());
                flag = flag && asNetworkDeviceSpecialService.save(asNetworkDeviceSpecial);
            }
        } else if (asTypeId ==  GlobalParam.typeIDForAff) {
            //外设
            AsIoSpecial asIoSpecial = assetVO.getAsIoSpecial();
            if (ObjectUtil.isNotEmpty(asIoSpecial)) {
                asIoSpecial.setAsId(asDeviceCommon.getId());
                flag = flag && asIoSpecialService.save(asIoSpecial);
            }
        } else if (asTypeId ==  GlobalParam.typeIDForSafe) {
            //安全产品（硬件）
            AsSecurityProductsSpecial asSecurityProductsSpecial = assetVO.getAsSecurityProductsSpecial();
            if (ObjectUtil.isNotEmpty(asSecurityProductsSpecial)) {
                asSecurityProductsSpecial.setAsId(asDeviceCommon.getId());
                flag = flag && asSecurityProductsSpecialService.save(asSecurityProductsSpecial);
            }
        } else if (asTypeId ==  GlobalParam.typeIDForYingyong) {
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
        if (ObjectUtil.isNotEmpty(asDeviceCommon.getMiji()) && !asDeviceCommon.getMiji().equals(asDeviceCommonDB.getMiji())) {//20230215密级变化需要触发保密编号生成；可能“类型变化”也需要触发（先考虑类型目前能不能变化：暂不研）
            AsType asType = asTypeService.getById(asDeviceCommon.getTypeId());
            asDeviceCommon.setBaomiNo(this.makeBaomiNo(asType, asDeviceCommon.getMiji(), asDeviceCommon.getUseDate()));
        }
        if (asDeviceCommon.getTypeId() == GlobalParam.typeIDForDisk && ObjectUtil.isNotEmpty(asDeviceCommon.getPortNo())) {
            //20230527 硬盘的宿主机字段（前端借用“信息点号”字段传来）；
            List<AsDeviceCommon> asDeviceCommonListForHost = this.list(new  QueryWrapper<AsDeviceCommon>().eq("org_id",GlobalParam.orgId).eq("no", asDeviceCommon.getPortNo()));
            asDeviceCommon.setPortNo("");//置空：本来就是借用字段，无须留值
            if (CollUtil.isNotEmpty(asDeviceCommonListForHost)) {
                asDeviceCommon.setHostAsId(asDeviceCommonListForHost.get(0).getId());
            } else
                throw new RuntimeException("该宿主机设备编号不存在！");
        }
        //20240328 todo增加 手动修改设备信息的“属性变更记录”

        //AsDeviceCommon asDeviceCommonDB = this.getById(asDeviceCommon.getId());
//        Field[] fields = asDeviceCommonDB.getClass().getFields();
//        for(Field field : fields){
//            System.out.println(field.getName());
//
//        }
        AsType asType = asTypeService.getLevel2AsTypeById(asDeviceCommon.getTypeId());
        Integer asTypeId = asType.getId();

        SysUser user = (SysUser) httpSession.getAttribute("user");
        SysDept dept = sysDeptService.getById(user.getDeptId());
        List<ProcessInstanceChange> changeList = Lists.newArrayList();
        List<AsConfig> asConfigList = asConfigService.list(new  QueryWrapper<AsConfig>().eq("org_id",GlobalParam.orgId).eq("en_table_name", "as_device_common"));
        //Boolean takeOffDisk = false;
        asConfigList.forEach(i -> {

            ProcessInstanceChange change = new ProcessInstanceChange();
            change.setAsId(asDeviceCommon.getId());
            change.setName(i.getZhColumnName());
            change.setIsFinish("是");
            change.setModifyDatetime(LocalDateTime.now());
            change.setDeptName(dept.getName());
            change.setDisplayName(user.getDisplayName());
            Object dbValueObj = ReflectUtil.getFieldValue(asDeviceCommonDB, StrUtil.toCamelCase(i.getEnColumnName()));
            String dbValueObj_str = "";
            Object pageValueObj = ReflectUtil.getFieldValue(asDeviceCommon, StrUtil.toCamelCase(i.getEnColumnName()));
            String pageValueObj_str = "";
           // if (ObjectUtil.isNotEmpty(dbValueObj)) {

            if (i.getType().equals("字符串") || i.getType().equals("数字")) {

                dbValueObj_str =ObjectUtil.isEmpty(dbValueObj)?"":dbValueObj.toString();//i.getType().equals("字符串")
                pageValueObj_str = ObjectUtil.isEmpty(pageValueObj)?"":pageValueObj.toString();
            } else if (i.getType().contains("日期")){
                if(i.getType().equals("日期")){
                    DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                    dbValueObj_str =ObjectUtil.isEmpty(dbValueObj)?"":((LocalDate) dbValueObj).format(fmt);
                    pageValueObj_str = ObjectUtil.isEmpty(pageValueObj)?"":((LocalDate) pageValueObj).format(fmt);
                }
            }
            if( !(dbValueObj_str.equals("0") && pageValueObj_str.equals("")) && !(ObjectUtil.isEmpty(dbValueObj) && ObjectUtil.isEmpty(pageValueObj)) && ((ObjectUtil.isEmpty(dbValueObj) && ObjectUtil.isNotEmpty(pageValueObj)) || !dbValueObj_str.equals(pageValueObj_str))){
                change.setOldValue(dbValueObj_str);
                change.setNewValue(pageValueObj_str);
                //20150129，增加对设备类型变化的判断与阻止
                if(change.getName().equals("设备类别")){
                    if(ObjectUtil.isNotEmpty(pageValueObj) && ObjectUtil.isNotEmpty(dbValueObj)){
                        System.out.println("设备类型变化");
                        AsType asTypeLevelForNewType = asTypeService.getLevel2AsTypeById(Integer.valueOf(dbValueObj.toString()));
                        AsType asTypeLevelForOldType = asTypeService.getLevel2AsTypeById(Integer.valueOf(pageValueObj.toString()));
                        int[] typeIdsForFwqOrCMP = {GlobalParam.typeIDForCMP,GlobalParam.typeIDForFWQ};
                        if((asTypeLevelForOldType.getId() != asTypeLevelForNewType.getId()) && !(ArrayUtil.contains(typeIdsForFwqOrCMP,asTypeLevelForNewType.getId()) && ArrayUtil.contains(typeIdsForFwqOrCMP,asTypeLevelForOldType.getId()))){
                           throw new RuntimeException("不能跨二级分类变更设备类型");
                        }
                    }


                }

                if(asDeviceCommon.getTypeId() == GlobalParam.typeIDForDisk){//如果是直接在硬盘表单中变更，也同步到宿主机; 目前仅同步“序列号变化”
                    if(change.getName().equals("设备序列号") && !("摘除".equals(asDeviceCommon.getState()) && !("报废".equals(asDeviceCommon.getState())))){
                        if(ObjectUtil.isNotEmpty(asDeviceCommon.getHostAsId())){
                            //2AsDeviceCommon asDeviceCommonPC = this.getById(asDeviceCommon.getHostAsId());
                            ProcessInstanceChange changePC = new ProcessInstanceChange();
                            BeanUtils.copyProperties(change,changePC);
                            changePC.setAsId(asDeviceCommon.getHostAsId());
                            changePC.setName("硬盘修改");
                            //changePC.setOldValue();
                            change.setNewValue("原序列号为" + dbValueObj_str + "的硬盘序列号修改为" + pageValueObj_str);
                            changePC.setIsFinish("是");
                            changePC.setModifyDatetime(LocalDateTime.now());
                            changePC.setDeptName(dept.getName());
                            changePC.setDisplayName(user.getDisplayName());

                            changeList.add(changePC);
                        }



                    }
                }

                changeList.add(change);
            }
            //  }
        });
        if (ObjectUtil.isNotEmpty(changeList)) {
            processInstanceChangeService.saveBatch(changeList);
        }


        flag = this.updateById(asDeviceCommon);

        //专用表
        if (asTypeId ==  GlobalParam.typeIDForCMP|| asTypeId ==  GlobalParam.typeIDForFWQ) {//20230107 添加服务器存储设备id
            AsComputerSpecial asComputerSpecial = assetVO.getAsComputerSpecial();
            AsComputerSpecial asComputerSpecialDB = asComputerSpecialService.getOne(new  QueryWrapper<AsComputerSpecial>().eq("org_id",GlobalParam.orgId).eq("as_id", asDeviceCommon.getId()));
            //20250106 记录“变更记录”
            List<ProcessInstanceChange> changeList1 = Lists.newArrayList();
            List<AsConfig> asConfigList2 =  asConfigService.list(new  QueryWrapper<AsConfig>().eq("org_id",GlobalParam.orgId).eq("en_table_name", "as_computer_special"));
            asConfigList2.forEach(i -> {
                ProcessInstanceChange change = new ProcessInstanceChange();
                change.setAsId(asDeviceCommon.getId());
                change.setName(i.getZhColumnName());
                change.setIsFinish("是");
                change.setModifyDatetime(LocalDateTime.now());
                change.setDeptName(dept.getName());
                change.setDisplayName(user.getDisplayName());
                Object dbValueObj = ReflectUtil.getFieldValue(asComputerSpecialDB, StrUtil.toCamelCase(i.getEnColumnName()));
                String dbValueObj_str = "";
                Object pageValueObj = ReflectUtil.getFieldValue(asComputerSpecial, StrUtil.toCamelCase(i.getEnColumnName()));
                String pageValueObj_str = "";
                // if (ObjectUtil.isNotEmpty(dbValueObj)) {
                if (i.getType().equals("字符串") || i.getType().equals("数字")) {
                    dbValueObj_str =ObjectUtil.isEmpty(dbValueObj)?"":dbValueObj.toString();//i.getType().equals("字符串")
                    pageValueObj_str = ObjectUtil.isEmpty(pageValueObj)?"":pageValueObj.toString();
                } else if (i.getType().contains("日期")){
                    if(i.getType().equals("日期")){
                        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                        dbValueObj_str =ObjectUtil.isEmpty(dbValueObj)?"":((LocalDate) dbValueObj).format(fmt);
                        pageValueObj_str = ObjectUtil.isEmpty(pageValueObj)?"":((LocalDate) pageValueObj).format(fmt);
                    }
                }
                if( !(dbValueObj_str.equals("0") && pageValueObj_str.equals("")) && !(ObjectUtil.isEmpty(dbValueObj) && ObjectUtil.isEmpty(pageValueObj)) && ((ObjectUtil.isEmpty(dbValueObj) && ObjectUtil.isNotEmpty(pageValueObj)) || !dbValueObj_str.equals(pageValueObj_str))){
                    change.setOldValue(dbValueObj_str);
                    change.setNewValue(pageValueObj_str);
                    changeList1.add(change);
                }
                //  }
            });
            if (ObjectUtil.isNotEmpty(changeList1)) {
                processInstanceChangeService.saveBatch(changeList1);
            }
            if (ObjectUtil.isNotEmpty(asComputerSpecial)) {
                asComputerSpecial.setAsId(asDeviceCommon.getId());
                flag = flag && asComputerSpecialService.saveOrUpdate(asComputerSpecial);
            }


            AsComputerGranted asComputerGranted = assetVO.getAsComputerGranted();
            AsComputerGranted asComputerGrantedDB = asComputerGrantedService.getOne(new  QueryWrapper<AsComputerGranted>().eq("org_id",GlobalParam.orgId).eq("as_id", asDeviceCommon.getId()));
            //20250106 记录“变更记录”
            List<ProcessInstanceChange> changeList2 = Lists.newArrayList();
            List<AsConfig> asConfigList3 =  asConfigService.list(new  QueryWrapper<AsConfig>().eq("org_id",GlobalParam.orgId).eq("en_table_name", "as_computer_granted"));
            asConfigList3.forEach(i -> {
                ProcessInstanceChange change = new ProcessInstanceChange();
                change.setAsId(asDeviceCommon.getId());
                change.setName(i.getZhColumnName());
                change.setIsFinish("是");
                change.setModifyDatetime(LocalDateTime.now());
                change.setDeptName(dept.getName());
                change.setDisplayName(user.getDisplayName());
                Object dbValueObj = ReflectUtil.getFieldValue(asComputerGrantedDB, StrUtil.toCamelCase(i.getEnColumnName()));
                String dbValueObj_str = "";
                Object pageValueObj = ReflectUtil.getFieldValue(asComputerGranted, StrUtil.toCamelCase(i.getEnColumnName()));
                String pageValueObj_str = "";
                // if (ObjectUtil.isNotEmpty(dbValueObj)) {
                if (i.getType().equals("字符串") || i.getType().equals("数字")) {
                    dbValueObj_str =ObjectUtil.isEmpty(dbValueObj)?"":dbValueObj.toString();//i.getType().equals("字符串")
                    pageValueObj_str = ObjectUtil.isEmpty(pageValueObj)?"":pageValueObj.toString();
                } else if (i.getType().contains("日期")){
                    if(i.getType().equals("日期")){
                        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                        dbValueObj_str =ObjectUtil.isEmpty(dbValueObj)?"":((LocalDate) dbValueObj).format(fmt);
                        pageValueObj_str = ObjectUtil.isEmpty(pageValueObj)?"":((LocalDate) pageValueObj).format(fmt);
                    }
                }
                if( !(ObjectUtil.isEmpty(dbValueObj) && ObjectUtil.isEmpty(pageValueObj)) && ((ObjectUtil.isEmpty(dbValueObj) && ObjectUtil.isNotEmpty(pageValueObj)) || !dbValueObj_str.equals(pageValueObj_str))){
                    change.setOldValue(dbValueObj_str);
                    change.setNewValue(pageValueObj_str);
                    changeList2.add(change);
                }
                //  }
            });
            if (ObjectUtil.isNotEmpty(changeList2)) {
                processInstanceChangeService.saveBatch(changeList2);
            }
            if (ObjectUtil.isNotEmpty(asComputerGranted)) {
                asComputerGranted.setAsId(asDeviceCommon.getId());
                flag = flag && asComputerGrantedService.saveOrUpdate(asComputerGranted);
            }
            //20220612 保存硬盘信息 及变更记录（含计算机与硬盘两个主体的）
            List<AsDeviceCommon> diskListForHis = assetVO.getDiskListForHis();
            if (CollUtil.isNotEmpty(diskListForHis)) {
                List<AsDeviceCommon> diskListForHisForDel = diskListForHis.stream().filter(item -> item.getTemp().equals("删除")).collect(Collectors.toList());
                List<AsDeviceCommon> diskListForHisForSaveOrUpdate = diskListForHis.stream().filter(item -> !(item.getTemp().equals("删除"))).collect(Collectors.toList());
                if (CollUtil.isNotEmpty(diskListForHisForDel)){
                    //20250328 增加删除时的变更记录
                    List<String> diskSnList = diskListForHisForDel.stream().map(AsDeviceCommon::getSn).collect(Collectors.toList());
                    String diskSn =  String.join(",", diskSnList);
                    ProcessInstanceChange change = new ProcessInstanceChange();
                    change.setDeptName(dept.getName());
                    change.setAsId(asDeviceCommon.getId());
                    change.setIsFinish("是");//这个字段必须有
                    change.setModifyDatetime(LocalDateTime.now());
                    change.setDeptName(dept.getName());
                    change.setDisplayName(user.getDisplayName());
                    change.setName("硬盘删除");
                    change.setNewValue("删除硬盘的序列号为：" + diskSn);
                    processInstanceChangeService.save(change);//删除时仅记录PC为主体的硬盘变更
                    this.removeByIds(diskListForHisForDel.stream().map(AsDeviceCommon::getId).collect(Collectors.toList()));
                }
                //注：即使硬盘没有编辑：现有硬盘信息也会记录在diskListForHisForSaveOrUpdate中
                if (CollUtil.isNotEmpty(diskListForHisForSaveOrUpdate)) {
                    for (AsDeviceCommon item : diskListForHisForSaveOrUpdate) {
                        ///20230608 同步硬盘与计算机同值的相关字段
                        item.setMiji(assetVO.getAsDeviceCommon().getMiji());
                        item.setUserName(assetVO.getAsDeviceCommon().getUserName());
                        item.setUserDept(assetVO.getAsDeviceCommon().getUserDept());
                        //计算机状态变化时，对（新增与修改的）硬盘硬盘状态的强制同步：排除对硬盘本体处于“停用|填错|报废|摘除”状态的同步：只有PC本体为“在用”||“停用”||“库存”时（这里不分PC状态是更改后的，还是<本过程未变化>原值）才会强制同步
                        if ((assetVO.getAsDeviceCommon().getState().equals("在用") || assetVO.getAsDeviceCommon().getState().equals("停用") || assetVO.getAsDeviceCommon().getState().equals("库存")) && (!item.getState().equals("摘除") && !item.getState().equals("报废") && !item.getState().equals("填错")))//只有这两种状态同步;硬盘的“摘除”||""||""状态不受宿主机同步
                            item.setState(assetVO.getAsDeviceCommon().getState());
                        item.setNetType(assetVO.getAsDeviceCommon().getNetType());
                        //20250825 修改时 PC与硬盘都记录了 && 新增时只记录PC && 删除时都没记录
                        AsDeviceCommon diskDB = this.getById(item.getId());
                        ProcessInstanceChange change = new ProcessInstanceChange();
                        change.setDeptName(dept.getName());
                        change.setAsId(asDeviceCommon.getId());
                        change.setIsFinish("是");//这个字段必须有
                        change.setModifyDatetime(LocalDateTime.now());
                        change.setDeptName(dept.getName());
                        change.setDisplayName(user.getDisplayName());
                        if(ObjectUtil.isNotEmpty(item.getId()) && !item.getSn().equals(diskDB.getSn())){
                            change.setName("硬盘修改(序列号)");
                            change.setOldValue(diskDB.getSn());
                            change.setNewValue(item.getSn());
                        } else if(ObjectUtil.isNotEmpty(item.getId()) && !item.getState().equals(diskDB.getState())){//序列号与状态同时修改的情况 暂不考虑
                            change.setName("硬盘修改(状态)");
                            change.setNewValue("原序列号为" + diskDB.getSn() + "的状态修改为" + item.getState());
                        }  else if(item.getTemp().equals("新增")){
                            change.setName("硬盘新增");
                            change.setNewValue("新增硬盘的序列号为" + item.getSn());
                        }
                        item.setTemp("");
                        this.saveOrUpdate(item);
                        if(ObjectUtil.isNotEmpty(change.getNewValue())){
                            processInstanceChangeService.save(change);//记录PC为主体的硬盘变更
                         //   if (ObjectUtil.isNotEmpty(item.getId())){//硬盘修改时，此时没写入DB没有id
                                ProcessInstanceChange changeDisk = new ProcessInstanceChange();//同步记录在硬盘表中
                                BeanUtils.copyProperties(change, changeDisk);
                                changeDisk.setAsId(item.getId());//记录硬盘本身为主体的硬盘变更
                                processInstanceChangeService.save(changeDisk);
//                            } else { //硬盘新增时
//
//
//                            }

                        }

                    }
                    flag = flag && this.saveOrUpdateBatch(diskListForHisForSaveOrUpdate);
                }


            }
        } else if (asTypeId ==  GlobalParam.typeIDForNET) {
            //网络设备
            AsNetworkDeviceSpecial asNetworkDeviceSpecial = assetVO.getAsNetworkDeviceSpecial();
            AsNetworkDeviceSpecial asNetworkDeviceSpecialDB = asNetworkDeviceSpecialService.getOne(new  QueryWrapper<AsNetworkDeviceSpecial>().eq("org_id",GlobalParam.orgId).eq("as_id", asDeviceCommon.getId()));
            //20250106 记录“变更记录”
            List<ProcessInstanceChange> changeList4 = Lists.newArrayList();
            List<AsConfig> asConfigList3 =  asConfigService.list(new  QueryWrapper<AsConfig>().eq("org_id",GlobalParam.orgId).eq("en_table_name", "as_network_device_special"));
            asConfigList3.forEach(i -> {
                ProcessInstanceChange change = new ProcessInstanceChange();
                change.setAsId(asDeviceCommon.getId());
                change.setName(i.getZhColumnName());
                change.setIsFinish("是");
                change.setModifyDatetime(LocalDateTime.now());
                change.setDeptName(dept.getName());
                change.setDeptName(dept.getName());
                change.setDisplayName(user.getDisplayName());
                Object dbValueObj = ReflectUtil.getFieldValue(asNetworkDeviceSpecialDB, StrUtil.toCamelCase(i.getEnColumnName()));
                String dbValueObj_str = "";
                Object pageValueObj = ReflectUtil.getFieldValue(asNetworkDeviceSpecial, StrUtil.toCamelCase(i.getEnColumnName()));
                String pageValueObj_str = "";
                // if (ObjectUtil.isNotEmpty(dbValueObj)) {
                if (i.getType().equals("字符串") || i.getType().equals("数字")) {
                    dbValueObj_str =ObjectUtil.isEmpty(dbValueObj)?"":dbValueObj.toString();//i.getType().equals("字符串")
                    pageValueObj_str = ObjectUtil.isEmpty(pageValueObj)?"":pageValueObj.toString();
                } else if (i.getType().contains("日期")){
                    if(i.getType().equals("日期")){
                        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                        dbValueObj_str =ObjectUtil.isEmpty(dbValueObj)?"":((LocalDate) dbValueObj).format(fmt);
                        pageValueObj_str = ObjectUtil.isEmpty(pageValueObj)?"":((LocalDate) pageValueObj).format(fmt);
                    }
                }
                if(!(ObjectUtil.isEmpty(dbValueObj) && ObjectUtil.isEmpty(pageValueObj)) && ((ObjectUtil.isEmpty(dbValueObj) && ObjectUtil.isNotEmpty(pageValueObj)) || !dbValueObj_str.equals(pageValueObj_str))){
                    change.setOldValue(dbValueObj_str);
                    change.setNewValue(pageValueObj_str);
                    changeList4.add(change);
                }
                //  }
            });
            if (ObjectUtil.isNotEmpty(changeList4)) {
                processInstanceChangeService.saveBatch(changeList4);
            }
            if (ObjectUtil.isNotEmpty(asNetworkDeviceSpecial)) {
                asNetworkDeviceSpecial.setAsId(asDeviceCommon.getId());
                flag = flag && asNetworkDeviceSpecialService.saveOrUpdate(asNetworkDeviceSpecial);
            }
        } else if (asTypeId ==  GlobalParam.typeIDForAff) {
            //外设
            AsIoSpecial asIoSpecial = assetVO.getAsIoSpecial();
            AsIoSpecial asIoSpecialDB = asIoSpecialService.getOne(new  QueryWrapper<AsIoSpecial>().eq("org_id",GlobalParam.orgId).eq("as_id", asDeviceCommon.getId()));
            //20250106 记录“变更记录”
            List<ProcessInstanceChange> changeList4 = Lists.newArrayList();
            List<AsConfig> asConfigList3 =  asConfigService.list(new  QueryWrapper<AsConfig>().eq("org_id",GlobalParam.orgId).eq("en_table_name", "as_io_special"));
            asConfigList3.forEach(i -> {
                ProcessInstanceChange change = new ProcessInstanceChange();
                change.setAsId(asDeviceCommon.getId());
                change.setName(i.getZhColumnName());
                change.setIsFinish("是");
                change.setModifyDatetime(LocalDateTime.now());
                change.setDeptName(dept.getName());
                change.setDisplayName(user.getDisplayName());
                Object dbValueObj = ReflectUtil.getFieldValue(asIoSpecialDB, StrUtil.toCamelCase(i.getEnColumnName()));
                String dbValueObj_str = "";
                Object pageValueObj = ReflectUtil.getFieldValue(asIoSpecial, StrUtil.toCamelCase(i.getEnColumnName()));
                String pageValueObj_str = "";
                // if (ObjectUtil.isNotEmpty(dbValueObj)) {
                if (i.getType().equals("字符串") || i.getType().equals("数字")) {
                    dbValueObj_str =ObjectUtil.isEmpty(dbValueObj)?"":dbValueObj.toString();//i.getType().equals("字符串")
                    pageValueObj_str = ObjectUtil.isEmpty(pageValueObj)?"":pageValueObj.toString();
                } else if (i.getType().contains("日期")){
                    if(i.getType().equals("日期")){
                        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                        dbValueObj_str =ObjectUtil.isEmpty(dbValueObj)?"":((LocalDate) dbValueObj).format(fmt);
                        pageValueObj_str = ObjectUtil.isEmpty(pageValueObj)?"":((LocalDate) pageValueObj).format(fmt);
                    }
                }
                if(!(ObjectUtil.isEmpty(dbValueObj) && ObjectUtil.isEmpty(pageValueObj)) && ((ObjectUtil.isEmpty(dbValueObj) && ObjectUtil.isNotEmpty(pageValueObj)) || !dbValueObj_str.equals(pageValueObj_str))){
                    change.setOldValue(dbValueObj_str);
                    change.setNewValue(pageValueObj_str);
                    changeList4.add(change);
                }
                //  }
            });
            if (ObjectUtil.isNotEmpty(changeList4)) {
                processInstanceChangeService.saveBatch(changeList4);
            }
            if (ObjectUtil.isNotEmpty(asIoSpecial)) {
                asIoSpecial.setAsId(asDeviceCommon.getId());
                flag = flag && asIoSpecialService.saveOrUpdate(asIoSpecial);
            }
        } else if (asTypeId ==  GlobalParam.typeIDForSafe) {
            //安全产品（硬件）
            AsSecurityProductsSpecial asSecurityProductsSpecial = assetVO.getAsSecurityProductsSpecial();
            if (ObjectUtil.isNotEmpty(asSecurityProductsSpecial)) {
                asSecurityProductsSpecial.setAsId(asDeviceCommon.getId());
                flag = flag && asSecurityProductsSpecialService.saveOrUpdate(asSecurityProductsSpecial);
            }
        } else if (asTypeId ==  GlobalParam.typeIDForYingyong) {
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
        List<SysUser> userList = sysUserService.list(new  QueryWrapper<SysUser>().eq("org_id",GlobalParam.orgId).eq("status", "正常"));
        List<SysDept> deptList = sysDeptService.list(new  QueryWrapper<SysDept>().eq("org_id",GlobalParam.orgId).eq("pid", GlobalParam.depSubRootID));
        //格式：<信息化中心,13>
        Map<Integer, String> mapDept = new HashMap<>();
        for (SysDept dept : deptList) {
            mapDept.put(dept.getId(), dept.getName());
        }
        //格式：<信息化中心.张三,机密>
        Map<String, String> mapUserMiji = new HashMap<>();
        for (SysUser user : userList) {
            mapUserMiji.put(mapDept.get(user.getDeptId()) + "." + user.getDisplayName(), user.getSecretDegree());
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
            List<Integer> asTypeIdList = asTypeService.getTypeIdList(GlobalParam.typeIDForCMP);//约定了计算机的id:4
            List<Integer> asTypeIdList2 = asTypeService.getTypeIdList(GlobalParam.typeIDForFWQ);//约定了服务器存储设备的id:29
            asTypeIdList.addAll(asTypeIdList2);
            List<Map<String, Object>> listMaps = this.listMaps(new  QueryWrapper<AsDeviceCommon>().eq("org_id",GlobalParam.orgId).in("type_id", asTypeIdList).select("no", "id"));
            List<String> updateFailedList = new ArrayList<>();
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
                        ///20250102 todo在其他类型导入时也同步写下判空逻辑
                        AsComputerSpecial a = asComputerSpecialService.getOne(new  QueryWrapper<AsComputerSpecial>().eq("org_id",GlobalParam.orgId).eq("as_id", asDeviceCommon.getId()));
                        if(ObjectUtil.isEmpty(a))
                            throw new RuntimeException(asDeviceCommon.getNo() + "的计算机专用表不存在");
                        asComputerSpecial.setId(a.getId());
                        asComputerGranted.setAsId(asDeviceCommon.getId());
                        asComputerGranted.setId(asComputerGrantedService.getOne(new  QueryWrapper<AsComputerGranted>().eq("org_id",GlobalParam.orgId).eq("as_id", asDeviceCommon.getId())).getId());
                        asComputerSpecialService.updateById(asComputerSpecial);
                        asComputerGrantedService.updateById(asComputerGranted);
                        List<AsDeviceCommon> asDeviceCommonListForDiskAdd = new ArrayList<>();
                        List<AsDeviceCommon> asDeviceCommonListForDiskUpdate = new ArrayList<>();
                        List<AsDeviceCommon> asDeviceCommonListForDiskInDB = this.list(new  QueryWrapper<AsDeviceCommon>().eq("org_id",GlobalParam.orgId).eq("type_id", GlobalParam.typeIDForDisk).eq("host_as_id", noIdMap.get(asDeviceCommon.getNo())));
                        if (CollUtil.isNotEmpty(asDeviceCommonListForDiskInDB)) {
                            for (AsDeviceCommon item : asDeviceCommonListForDiskInDB) {
                                /// 同步硬盘与计算机同值的相关字段
                                item.setMiji(asDeviceCommon.getMiji());
                                item.setUserName(asDeviceCommon.getUserName());
                                item.setUserDept(asDeviceCommon.getUserDept());
                                if ((asDeviceCommon.getState().equals("在用") || asDeviceCommon.getState().equals("停用")) && (!item.getState().equals("摘除") && !item.getState().equals("报废")))//只有这两种状态同步;硬盘的“摘除”状态不受宿主机同步
                                    item.setState(asDeviceCommon.getState());
                                item.setNetType(asDeviceCommon.getNetType());

                            }
                            this.saveOrUpdateBatch(asDeviceCommonListForDiskInDB);
                        }

                        //查找出目前DB中该计算机对应的硬盘：这里包括“非在用”
                        List<Map<String, Object>> ypListMaps = this.listMaps(new  QueryWrapper<AsDeviceCommon>().eq("org_id",GlobalParam.orgId).eq("type_id", GlobalParam.typeIDForDisk).eq("host_as_id", noIdMap.get(asDeviceCommon.getNo())).select("sn", "id"));
                        Map<String, Integer> ypNoIdMap = ypListMaps.stream().collect(Collectors.toMap(v -> v.get("sn").toString(), v -> Integer.valueOf(v.get("id").toString()), (key1, key2) -> key2));
                        if (ObjectUtil.isNotEmpty(asComputerExcel.getDiskSn1())) {
                            callByAddComputer(asComputerExcel.getDiskSn1(), asComputerExcel.getDiskMode1(), asComputerExcel.getDiskSize1(), asDeviceCommon, asDeviceCommonListForDiskAdd, asDeviceCommonListForDiskUpdate, ypNoIdMap);
                        }
                        if (ObjectUtil.isNotEmpty(asComputerExcel.getDiskSn2())) {
                            callByAddComputer(asComputerExcel.getDiskSn2(), asComputerExcel.getDiskMode2(), asComputerExcel.getDiskSize2(), asDeviceCommon, asDeviceCommonListForDiskAdd, asDeviceCommonListForDiskUpdate, ypNoIdMap);
                        }
                        if (ObjectUtil.isNotEmpty(asComputerExcel.getDiskSn3())) {
                            callByAddComputer(asComputerExcel.getDiskSn3(), asComputerExcel.getDiskMode3(), asComputerExcel.getDiskSize3(), asDeviceCommon, asDeviceCommonListForDiskAdd, asDeviceCommonListForDiskUpdate, ypNoIdMap);
                        }
                        if (ObjectUtil.isNotEmpty(asComputerExcel.getDiskSn4())) {
                            callByAddComputer(asComputerExcel.getDiskSn4(), asComputerExcel.getDiskMode4(), asComputerExcel.getDiskSize4(), asDeviceCommon, asDeviceCommonListForDiskAdd, asDeviceCommonListForDiskUpdate, ypNoIdMap);
                        }
                        this.updateBatchById(asDeviceCommonListForDiskUpdate);
                        this.saveBatch(asDeviceCommonListForDiskAdd);
                        //删除未在导入表格sn字段中登记的老硬盘:只有在硬盘数据有导入的情况，才考虑删除; 20230403有时间考虑下要过滤（阻止删除）正在走流程的硬盘
                        if (CollUtil.isNotEmpty(asDeviceCommonListForDiskAdd) || CollUtil.isNotEmpty(asDeviceCommonListForDiskUpdate)) {
                            List<AsDeviceCommon> handledList = asDeviceCommonListForDiskAdd.stream().collect(Collectors.toList());
                            handledList.addAll(asDeviceCommonListForDiskUpdate);
                            List<String> handledSnList = handledList.stream().map(item -> item.getSn().toString()).collect(Collectors.toList());
                            //注：只统计“在用”硬盘：下面的根据导入excel清理计算机对应的DB中的老硬盘：也不清理之前的“特殊状态”硬盘：暂不细研
                            List<String> excludeState = Arrays.asList(new String[]{"报废", "待报废", "销毁", "归库"});
                            List<AsDeviceCommon> dbHarkDiskListForDeleteForCurrentComputer;
                            if (CollUtil.isNotEmpty(handledSnList))
                                dbHarkDiskListForDeleteForCurrentComputer = this.list(new  QueryWrapper<AsDeviceCommon>().eq("org_id",GlobalParam.orgId).notIn("state", excludeState).notIn("sn", handledSnList).eq("host_as_id", asDeviceCommon.getId()));
                            else
                                dbHarkDiskListForDeleteForCurrentComputer = this.list(new  QueryWrapper<AsDeviceCommon>().eq("org_id",GlobalParam.orgId).notIn("state", excludeState).eq("host_as_id", asDeviceCommon.getId()));
                            this.removeByIds(dbHarkDiskListForDeleteForCurrentComputer.stream().map(item -> Integer.valueOf(item.getId().toString())).collect(Collectors.toList()));
                        }
                    } else {  //20221227资产号在DB不存在的设备：做一个记录
                        updateFailedList.add(asDeviceCommon.getNo());
                    }

                }
            }
            int updateSucessCount = excelList.size() - updateFailedList.size();
            if (CollUtil.isNotEmpty(updateFailedList))
                return "更新了" + updateSucessCount + "条设备信息；" + "此外，" + updateFailedList.stream().collect(Collectors.joining(",")) + "不存在，请先导入系统";
            else
                return "更新了" + updateSucessCount + "条设备信息";
        } else { //这个分支是之前的导入模式：都是需要删除老记录&&设备ID会变
            if (importMode.equals("覆盖")) {//可重复&需覆盖，把重复的从DB删除
                List<AsDeviceCommon> list = this.list(new  QueryWrapper<AsDeviceCommon>().eq("org_id",GlobalParam.orgId).in("no", excelList.stream().map(AsComputerExcel::getNo).collect(Collectors.toList())));
                if (ObjectUtil.isNotEmpty(list)) {
                    dbList = list;
                    List<Integer> asIdList = list.stream().map(AsDeviceCommon::getId).collect(Collectors.toList());
                    this.removeByIds(asIdList);
                    //20221226加 删除计算机硬盘:效果待观察
                    List<Map<String, Object>> listMaps1 = this.listMaps(new  QueryWrapper<AsDeviceCommon>().eq("org_id",GlobalParam.orgId).in("host_as_id", asIdList).select("id"));
                    List<Integer> ypIdList = listMaps1.stream().map(item -> Integer.valueOf(item.get("id").toString())).collect(Collectors.toList());
                    this.removeByIds(ypIdList);
                    asComputerSpecialService.remove(new  QueryWrapper<AsComputerSpecial>().eq("org_id",GlobalParam.orgId).in("as_id", asIdList));
                    asComputerGrantedService.remove(new  QueryWrapper<AsComputerGranted>().eq("org_id",GlobalParam.orgId).in("as_id", asIdList));
                }
                pageList = excelList;
            } else {//不可重复&不覆盖; 属于“纯粹新增模式”
                //20211116读出DB与EXCEL资产号重复的记录
                List<AsDeviceCommon> list = this.list(new  QueryWrapper<AsDeviceCommon>().eq("org_id",GlobalParam.orgId).in("no", excelList.stream().map(AsComputerExcel::getNo).collect(Collectors.toList())));
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
                        if (asComputerExcel.getState().equals("在用") && ObjectUtil.isEmpty(asComputerExcel.getMiji()))
                            throw new RuntimeException("“在用”设备的密级不能为空");
                    }
                    AsDeviceCommon asDeviceCommon = new AsDeviceCommon();
                   // asDeviceCommon.setCreateDatetime(LocalDateTime.now());
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
                    if (ObjectUtil.isEmpty(asDeviceCommon.getBaomiNo())) {
                        //20230312 todo往 makeBaomiNo()传入”设备启用时间“参数：这里已经把"Date"字段处理成localdate类型了，直接用即可
                        asDeviceCommon.setBaomiNo(this.makeBaomiNo(asType, asDeviceCommon.getMiji(), asDeviceCommon.getUseDate()));
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
                        callByAddComputer(asComputerExcel.getDiskSn1(), asComputerExcel.getDiskMode1(), asComputerExcel.getDiskSize1(), asDeviceCommon, asDeviceCommonListForDisk, asDeviceCommonListForDiskUpdate, ypNoIdMap);
                    }
                    if (ObjectUtil.isNotEmpty(asComputerExcel.getDiskSn2())) {
                        callByAddComputer(asComputerExcel.getDiskSn2(), asComputerExcel.getDiskMode2(), asComputerExcel.getDiskSize2(), asDeviceCommon, asDeviceCommonListForDisk, asDeviceCommonListForDiskUpdate, ypNoIdMap);
                    }
                    if (ObjectUtil.isNotEmpty(asComputerExcel.getDiskSn3())) {
                        callByAddComputer(asComputerExcel.getDiskSn3(), asComputerExcel.getDiskMode3(), asComputerExcel.getDiskSize3(), asDeviceCommon, asDeviceCommonListForDisk, asDeviceCommonListForDiskUpdate, ypNoIdMap);
                    }
                    if (ObjectUtil.isNotEmpty(asComputerExcel.getDiskSn4())) {
                        callByAddComputer(asComputerExcel.getDiskSn4(), asComputerExcel.getDiskMode4(), asComputerExcel.getDiskSize4(), asDeviceCommon, asDeviceCommonListForDisk, asDeviceCommonListForDiskUpdate, ypNoIdMap);
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
            List<Integer> asTypeIdList = asTypeService.getTypeIdList(GlobalParam.typeIDForNET);//约定了网络设备的typeid:5
            List<Map<String, Object>> listMaps = this.listMaps(new  QueryWrapper<AsDeviceCommon>().eq("org_id",GlobalParam.orgId).in("type_id", asTypeIdList).select("no", "id"));
            List<String> updateFailedList = new ArrayList<>();
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
                        AsNetworkDeviceSpecial a = asNetworkDeviceSpecialService.getOne(new  QueryWrapper<AsNetworkDeviceSpecial>().eq("org_id",GlobalParam.orgId).eq("as_id", asDeviceCommon.getId()));
                        if(ObjectUtil.isEmpty(a))
                            throw new RuntimeException(asDeviceCommon.getNo() + "的网络设备专用表不存在");
                        asNetworkDeviceSpecial.setId(a.getId());
                        asNetworkDeviceSpecial.setAsId(asDeviceCommon.getId());
                        asNetworkDeviceSpecialService.updateById(asNetworkDeviceSpecial);
                    } else //20221227资产号在DB不存在的设备：做一个记录
                        updateFailedList.add(asDeviceCommon.getNo());
                }
            }
            int updateSucessCount = excelList.size() - updateFailedList.size();
            if (CollUtil.isNotEmpty(updateFailedList))
                return "更新了" + updateSucessCount + "条设备信息；" + "此外，" + updateFailedList.stream().collect(Collectors.joining(",")) + "不存在，请先导入系统";
            else
                return "更新了" + updateSucessCount + "条设备信息";
        } else {
            if (importMode.equals("覆盖")) {
                List<AsDeviceCommon> list = this.list(new  QueryWrapper<AsDeviceCommon>().eq("org_id",GlobalParam.orgId).in("no", excelList.stream().map(AsNetworkDeviceExcel::getNo).collect(Collectors.toList())));
                if (ObjectUtil.isNotEmpty(list)) {
                    dbList = list;
                    List<Integer> asIdList = list.stream().map(AsDeviceCommon::getId).collect(Collectors.toList());
                    this.removeByIds(asIdList);
                    asNetworkDeviceSpecialService.remove(new  QueryWrapper<AsNetworkDeviceSpecial>().eq("org_id",GlobalParam.orgId).in("as_id", asIdList));
                }
                pageList = excelList;
            } else {
                List<AsDeviceCommon> list = this.list(new  QueryWrapper<AsDeviceCommon>().eq("org_id",GlobalParam.orgId).in("no", excelList.stream().map(AsNetworkDeviceExcel::getNo).collect(Collectors.toList())));
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
                    if (asNetworkDeviceExcel.getState().equals("在用") && ObjectUtil.isEmpty(asNetworkDeviceExcel.getMiji()))
                        throw new RuntimeException("“在用”设备的密级不能为空");
                }
                AsDeviceCommon asDeviceCommon = new AsDeviceCommon();
                asDeviceCommon.setCreateDatetime(LocalDateTime.now());
                AsNetworkDeviceSpecial asNetworkDeviceSpecial = new AsNetworkDeviceSpecial();
                //
                BeanUtils.copyProperties(asNetworkDeviceExcel, asDeviceCommon);
                BeanUtils.copyProperties(asNetworkDeviceExcel, asNetworkDeviceSpecial);
                //
                asDeviceCommon.setTypeId(asType.getId());
                //20230213 添加 保密编号的非空判断
                if (ObjectUtil.isEmpty(asDeviceCommon.getBaomiNo())) {
                    asDeviceCommon.setBaomiNo(this.makeBaomiNo(asType, asDeviceCommon.getMiji(), asDeviceCommon.getUseDate()));
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
            List<Integer> asTypeIdList = asTypeService.getTypeIdList(GlobalParam.typeIDForSafe);//约定了安全产品的typeid:7
            List<Map<String, Object>> listMaps = this.listMaps(new  QueryWrapper<AsDeviceCommon>().eq("org_id",GlobalParam.orgId).in("type_id", asTypeIdList).select("no", "id"));
            List<String> updateFailedList = new ArrayList<>();
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
                        asSecurityProductsSpecial.setId(asSecurityProductsSpecialService.getOne(new  QueryWrapper<AsSecurityProductsSpecial>().eq("org_id",GlobalParam.orgId).eq("as_id", asDeviceCommon.getId())).getId());
                        asSecurityProductsSpecial.setAsId(asDeviceCommon.getId());
                        asSecurityProductsSpecialService.updateById(asSecurityProductsSpecial);
                    } else //20221227资产号在DB不存在的设备：做一个记录
                        updateFailedList.add(asDeviceCommon.getNo());
                }
            }
            int updateSucessCount = excelList.size() - updateFailedList.size();
            if (CollUtil.isNotEmpty(updateFailedList))
                return "更新了" + updateSucessCount + "条设备信息；" + "此外，" + updateFailedList.stream().collect(Collectors.joining(",")) + "不存在，请先导入系统";
            else
                return "更新了" + updateSucessCount + "条设备信息";
        } else {
            if (importMode.equals("覆盖")) {
                List<AsDeviceCommon> list = this.list(new  QueryWrapper<AsDeviceCommon>().eq("org_id",GlobalParam.orgId).in("no", excelList.stream().map(AsSecurityProductExcel::getNo).collect(Collectors.toList())));
                if (ObjectUtil.isNotEmpty(list)) {
                    dbList = list;
                    List<Integer> asIdList = list.stream().map(AsDeviceCommon::getId).collect(Collectors.toList());
                    this.removeByIds(asIdList);
                    asNetworkDeviceSpecialService.remove(new  QueryWrapper<AsNetworkDeviceSpecial>().eq("org_id",GlobalParam.orgId).in("as_id", asIdList));
                }
                pageList = excelList;
            } else {
                List<AsDeviceCommon> list = this.list(new  QueryWrapper<AsDeviceCommon>().eq("org_id",GlobalParam.orgId).in("no", excelList.stream().map(AsSecurityProductExcel::getNo).collect(Collectors.toList())));
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
                    if (asSecurityProductExcel.getState().equals("在用") && ObjectUtil.isEmpty(asSecurityProductExcel.getMiji()))
                        throw new RuntimeException("“在用”设备的密级不能为空");
                }
                AsDeviceCommon asDeviceCommon = new AsDeviceCommon();
                asDeviceCommon.setCreateDatetime(LocalDateTime.now());
                AsSecurityProductsSpecial asSecurityProductsSpecial = new AsSecurityProductsSpecial();
                //
                BeanUtils.copyProperties(asSecurityProductExcel, asDeviceCommon);
                BeanUtils.copyProperties(asSecurityProductExcel, asSecurityProductsSpecial);
                //
                asDeviceCommon.setTypeId(asType.getId());
                //20230213 添加 保密编号的非空判断
                if (ObjectUtil.isEmpty(asDeviceCommon.getBaomiNo())) {
                    asDeviceCommon.setBaomiNo(this.makeBaomiNo(asType, asDeviceCommon.getMiji(), asDeviceCommon.getUseDate()));
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
            List<Integer> asTypeIdList = asTypeService.getTypeIdList(GlobalParam.typeIDForAff);//约定了外设的typeid:6
            List<Map<String, Object>> listMaps = this.listMaps(new  QueryWrapper<AsDeviceCommon>().eq("org_id",GlobalParam.orgId).in("type_id", asTypeIdList).select("no", "id"));
            List<String> updateFailedList = new ArrayList<>();
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
                        AsIoSpecial a = asIoSpecialService.getOne(new  QueryWrapper<AsIoSpecial>().eq("org_id",GlobalParam.orgId).eq("as_id", asDeviceCommon.getId()));
                        if(ObjectUtil.isEmpty(a))
                            throw new RuntimeException(asDeviceCommon.getNo() + "的外设专用表不存在");
                        asIoSpecial.setId(a.getId());
                        asIoSpecial.setAsId(asDeviceCommon.getId());
                        asIoSpecialService.updateById(asIoSpecial);
                    } else //20221227资产号在DB不存在的设备：做一个记录
                        updateFailedList.add(asDeviceCommon.getNo());
                }
            }
            int updateSucessCount = excelList.size() - updateFailedList.size();
            if (CollUtil.isNotEmpty(updateFailedList))
                return "更新了" + updateSucessCount + "条设备信息；" + "此外，" + updateFailedList.stream().collect(Collectors.joining(",")) + "不存在，请先导入系统";
            else
                return "更新了" + updateSucessCount + "条设备信息";
        } else {
            if (importMode.equals("覆盖")) {
                List<AsDeviceCommon> list = this.list(new  QueryWrapper<AsDeviceCommon>().eq("org_id",GlobalParam.orgId).in("no", excelList.stream().map(AsIoExcel::getNo).collect(Collectors.toList())));
                if (ObjectUtil.isNotEmpty(list)) {
                    dbList = list;
                    List<Integer> asIdList = list.stream().map(AsDeviceCommon::getId).collect(Collectors.toList());
                    this.removeByIds(asIdList);
                    asIoSpecialService.remove(new  QueryWrapper<AsIoSpecial>().eq("org_id",GlobalParam.orgId).in("as_id", asIdList));
                }
                pageList = excelList;
            } else {
                List<AsDeviceCommon> list = this.list(new  QueryWrapper<AsDeviceCommon>().eq("org_id",GlobalParam.orgId).in("no", excelList.stream().map(AsIoExcel::getNo).collect(Collectors.toList())));
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
                if (ObjectUtil.isEmpty(asIoExcel.getState())) {
                    throw new RuntimeException("设备状态不能为空");
                } else {
                    if (asIoExcel.getState().equals("在用") && ObjectUtil.isEmpty(asIoExcel.getMiji()))
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
                if (ObjectUtil.isEmpty(asDeviceCommon.getBaomiNo())) {
                    asDeviceCommon.setBaomiNo(this.makeBaomiNo(asType, asDeviceCommon.getMiji(), asDeviceCommon.getUseDate()));
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

    private String addAppExcel(List<AppExcel> excelList, String importMode) {
        //去掉db中存在的设备，剩下页面上需要导入的设备
        List<AppExcel> pageList;
        //db中存在的设备
        List<AsDeviceCommon> dbList = new ArrayList<>();
        //处理日期类型
        ExcelDateUtil.converToDate(excelList, AppExcel.class);
        if (importMode.equals("更新")) {//20221227
            List<Integer> asTypeIdList = asTypeService.getTypeIdList(GlobalParam.typeIDForApp);//约定了APP的typeid:58
            List<Map<String, Object>> listMaps = this.listMaps(new  QueryWrapper<AsDeviceCommon>().eq("org_id",GlobalParam.orgId).in("type_id", asTypeIdList).select("no", "id"));
            List<String> updateFailedList = new ArrayList<>();
            Map<String, Integer> noIdMap = new HashMap<>();
            for (Map<String, Object> map : listMaps) {
                noIdMap.put(map.get("no").toString(), Integer.valueOf(map.get("id").toString()));
            }
            if (ObjectUtil.isNotEmpty(excelList)) {
                for (AppExcel appExcel : excelList) {
                    if (ObjectUtil.isEmpty(appExcel.getNo())) {
                        throw new RuntimeException("编号不能为空");
                    }
                    AsDeviceCommon asDeviceCommon = new AsDeviceCommon();
                    AsIoSpecial asIoSpecial = new AsIoSpecial();
                    //
                    BeanUtils.copyProperties(appExcel, asDeviceCommon);
                    BeanUtils.copyProperties(appExcel, asIoSpecial);
                    //
                    // asDeviceCommon.setTypeId(asType.getId());
                    if (ObjectUtil.isNotEmpty(noIdMap.get(asDeviceCommon.getNo()))) {
                        asDeviceCommon.setId(noIdMap.get(asDeviceCommon.getNo()));
                        this.updateById(asDeviceCommon);

                    } else //20221227资产号在DB不存在的设备：做一个记录
                        updateFailedList.add(asDeviceCommon.getNo());
                }
            }
            int updateSucessCount = excelList.size() - updateFailedList.size();
            if (CollUtil.isNotEmpty(updateFailedList))
                return "更新了" + updateSucessCount + "条设备信息；" + "此外，" + updateFailedList.stream().collect(Collectors.joining(",")) + "不存在，请先导入系统";
            else
                return "更新了" + updateSucessCount + "条设备信息";
        } else {
            if (importMode.equals("覆盖")) {
                List<AsDeviceCommon> list = this.list(new  QueryWrapper<AsDeviceCommon>().eq("org_id",GlobalParam.orgId).in("no", excelList.stream().map(AppExcel::getNo).collect(Collectors.toList())));
                if (ObjectUtil.isNotEmpty(list)) {
                    dbList = list;
                    List<Integer> asIdList = list.stream().map(AsDeviceCommon::getId).collect(Collectors.toList());
                    this.removeByIds(asIdList);
                    asIoSpecialService.remove(new  QueryWrapper<AsIoSpecial>().eq("org_id",GlobalParam.orgId).in("as_id", asIdList));
                }
                pageList = excelList;
            } else {
                List<AsDeviceCommon> list = this.list(new  QueryWrapper<AsDeviceCommon>().eq("org_id",GlobalParam.orgId).in("no", excelList.stream().map(AppExcel::getNo).collect(Collectors.toList())));
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
            for (AppExcel appExcel : pageList) {
                if (ObjectUtil.isEmpty(appExcel.getNo())) {
                    throw new RuntimeException(appExcel.getName() + "的编号不能为空");
                }
                AsType asType = asTypeService.getAsType(appExcel.getTypeName());
                if (asType == null) {
                    throw new RuntimeException("类别不存在");
                }
                if (ObjectUtil.isEmpty(appExcel.getPortNo())) {
                    throw new RuntimeException(appExcel.getName() + "的子类不能为空");
                }
                AsDeviceCommon asDeviceCommon = new AsDeviceCommon();

                AsIoSpecial asIoSpecial = new AsIoSpecial();
                //
                BeanUtils.copyProperties(appExcel, asDeviceCommon);
                BeanUtils.copyProperties(appExcel, asIoSpecial);
                //
                asDeviceCommon.setTypeId(asType.getId());
                //20230213 添加 保密编号的非空判断
                if (ObjectUtil.isEmpty(asDeviceCommon.getBaomiNo())) {
                    asDeviceCommon.setBaomiNo(this.makeBaomiNo(asType, asDeviceCommon.getMiji(), asDeviceCommon.getUseDate()));
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
                return "导入" + pageCount + "条白名单;编号：" + dbList.stream().map(AsDeviceCommon::getNo).collect(Collectors.joining(",")) + ",已经被覆盖";
            } else {
                int pageCount = pageList.size();
                if (ObjectUtil.isEmpty(dbList)) {
                    return "导入" + pageCount + "条白名单";
                } else {
                    return "导入" + pageCount + "条白名单;编号：" + dbList.stream().map(AsDeviceCommon::getNo).collect(Collectors.joining(",")) + "已经存在，未导入";
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
            List<Integer> asTypeIdList = asTypeService.getTypeIdList(GlobalParam.typeIDForStor);//约定了存储介质的typeid:31
            asTypeIdList.addAll(asTypeService.getTypeIdList(GlobalParam.typeIDForOthers));//20230424 把“其他”类型也加入这里
            List<Map<String, Object>> listMaps = this.listMaps(new  QueryWrapper<AsDeviceCommon>().eq("org_id",GlobalParam.orgId).in("type_id", asTypeIdList).select("no", "id"));
            List<String> updateFailedList = new ArrayList<>();
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
            if (CollUtil.isNotEmpty(updateFailedList))
                return "更新了" + updateSucessCount + "条设备信息；" + "此外，" + updateFailedList.stream().collect(Collectors.joining(",")) + "不存在，请先导入系统";
            else
                return "更新了" + updateSucessCount + "条设备信息";
        } else {
            if (importMode.equals("覆盖")) {
                List<AsDeviceCommon> list = this.list(new  QueryWrapper<AsDeviceCommon>().eq("org_id",GlobalParam.orgId).in("no", excelList.stream().map(StorageExcel::getNo).collect(Collectors.toList())));
                if (ObjectUtil.isNotEmpty(list)) {
                    dbList = list;
                    List<Integer> asIdList = list.stream().map(AsDeviceCommon::getId).collect(Collectors.toList());
                    this.removeByIds(asIdList);
                }
                pageList = excelList;
            } else {
                List<AsDeviceCommon> list = this.list(new  QueryWrapper<AsDeviceCommon>().eq("org_id",GlobalParam.orgId).in("no", excelList.stream().map(StorageExcel::getNo).collect(Collectors.toList())));
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
                asDeviceCommon.setCreateDatetime(LocalDateTime.now());
                BeanUtils.copyProperties(storageExcel, asDeviceCommon);
                //
                asDeviceCommon.setTypeId(asType.getId());
                //20230213 添加 保密编号的非空判断
                if (ObjectUtil.isEmpty(asDeviceCommon.getBaomiNo())) {
                    asDeviceCommon.setBaomiNo(this.makeBaomiNo(asType, asDeviceCommon.getMiji(), asDeviceCommon.getUseDate()));
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
    @Transactional
    public List<String> addExcel(List<AsComputerExcel> list0, List<AsNetworkDeviceExcel> list1, List<AsSecurityProductExcel> list2, List<AsIoExcel> list3, List<StorageExcel> list4, List<AppExcel> list5, String importMode) {
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
        if (ObjectUtil.isNotEmpty(list5)) {
            resultList.add("白名单:" + addAppExcel(list5, importMode));
        }
        //
        if (ObjectUtil.isEmpty(resultList)) {
            resultList.add("无设备数据被导入");
        }
        return resultList;
    }
}
