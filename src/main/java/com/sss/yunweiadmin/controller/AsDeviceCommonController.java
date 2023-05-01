package com.sss.yunweiadmin.controller;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.csv.CsvUtil;
import cn.hutool.core.text.csv.CsvWriter;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelReader;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.read.metadata.ReadSheet;
import com.alibaba.excel.support.ExcelTypeEnum;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.alibaba.excel.write.metadata.style.WriteCellStyle;
import com.alibaba.excel.write.metadata.style.WriteFont;
import com.alibaba.excel.write.style.HorizontalCellStyleStrategy;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.segments.MergeSegments;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sss.yunweiadmin.common.operate.OperateLog;
import com.sss.yunweiadmin.common.result.ResponseResultWrapper;
import com.sss.yunweiadmin.common.utils.ExcelDateUtil;
import com.sss.yunweiadmin.model.entity.*;
import com.sss.yunweiadmin.model.excel.*;
import com.sss.yunweiadmin.model.vo.AssetVO;
import com.sss.yunweiadmin.model.vo.ValueLabelVO;
import com.sss.yunweiadmin.service.*;
import com.sun.javafx.binding.SelectBinding;
import lombok.SneakyThrows;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author 任勇林
 * @since 2021-04-15
 */
@RestController
@RequestMapping("/asDeviceCommon")
@ResponseResultWrapper
public class AsDeviceCommonController {
    @Autowired
    private AsDeviceCommonService asDeviceCommonService;
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
    AsApplicationSpecialService asApplicationSpecialService;
    @Autowired
    AsTypeService asTypeService;
    @Autowired
    SysDeptService sysDeptService;
    @Autowired
    DiskService diskService;
    @Autowired
    ProcessFormCustomTypeService processFormCustomTypeService;
    @Autowired
    StatisticsService statisticsService;
    @Autowired
    ProcessInstanceChangeService processInstanceChangeService;
    @Autowired
    ProcessInstanceDataService processInstanceDataService;
    @Autowired
    ProcessFormValue2Service processFormValue2Service;

    @GetMapping("test")
    public List<Integer> test(int typeId) {
        return asTypeService.getTypeIdList(typeId);
    }

    @GetMapping("statistics")
    public boolean statistics()
    {
        return asDeviceCommonService.addStatistics();
    }
    //20230304获得指定excel导出模板的所有属性名称
    @GetMapping("getTemplateFieldNames")
    public List<ValueLabelVO> getTemplateFieldNames(Integer typeId) throws Exception {
        List<ValueLabelVO> valueLabelVOList = new ArrayList<>();
        if(ObjectUtil.isNotEmpty(typeId)){
            int level2AsTypeId = asTypeService.getLevel2AsTypeById(typeId).getId();
            Class<?> clazz = null;
            if(level2AsTypeId == 4) //计算机
                clazz = AsComputerDownload.class;//Class.forName("com.sss.yunweiadmin.model.excel.AsComputerDownload");
            else if(level2AsTypeId == 6) //外设
                clazz = AsAffDownload.class;
            else //20230312 待进一步扩展
                clazz = AsCommonDownload.class;
            if(ObjUtil.isNotEmpty(clazz)){
                System.out.println("===============本类属性===============");
                // 取得本类的全部属性
                Field[] field = clazz.getDeclaredFields();
                for (int i = 0; i < field.length; i++) {
                    if (field[i].isAnnotationPresent(ExcelProperty.class)) {//判断是不是含有“ExcelProperty”注解
                        // 权限修饰符 Ps:某些属性没有用到，只是为了学习/体验
                        int mo = field[i].getModifiers();
                        String priv = Modifier.toString(mo);
                        // 属性类型
                        Class<?> type = field[i].getType();
                        valueLabelVOList.add(new ValueLabelVO( field[i].getName(), field[i].getAnnotation(ExcelProperty.class).value()[0]));
                        System.out.println(priv + " " + type.getName() + " " + field[i].getName() + " " +field[i].getAnnotation(ExcelProperty.class).value()[0] + ";");
                    }
                }
            }
        }
        return valueLabelVOList;
       // return asDeviceCommonService.addStatistics();
    }

    //20211115
    @OperateLog(module = "资产模块", type = "删除资产")
    @GetMapping("delete")
    public boolean delete(Integer[] idArr) {
        System.out.println(idArr[0]);
        return asDeviceCommonService.delete(idArr);
    }

    @GetMapping("list")
    public IPage<AsDeviceCommon> list(int currentPage, int pageSize, String no, Integer typeId, String name, String netType, String state, Integer userDept, String userName, String miji, Integer customTableId, String stateForExcludeForJsonStr,String processName) {
        QueryWrapper<AsDeviceCommon> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByDesc("id");
        if (ObjectUtil.isNotEmpty(no)) {
            queryWrapper.like("no", no);
        }
        if (ObjectUtil.isNotEmpty(miji)) {
            if("未定密".equals(miji))
                queryWrapper.and(qw->qw.eq("miji", "").or().eq("miji", "未定密"));
            else
                queryWrapper.like("miji", miji);
        }
        if (ObjectUtil.isNotEmpty(typeId)) {
            List<Integer> typeIdList = asTypeService.getTypeIdList(typeId);
            queryWrapper.in("type_id", typeIdList);
        } else  {
            if (ObjectUtil.isEmpty(customTableId))
                queryWrapper.ne("type_id",30);//20220905把硬盘排除; 20221203 挪此处：排除可能就是需要查询硬盘的情况
        }
        int level2AsTypeId = -1 ;
        ProcessFormCustomType processFormCustomType = null;
        if (ObjectUtil.isNotEmpty(customTableId)) {//20220716
            processFormCustomType = processFormCustomTypeService.getById(customTableId);
            String str = processFormCustomType.getProps();
            JSONObject jsonObject = JSON.parseObject(str);
            int asTypeId = jsonObject.getInteger("asTypeId");
            level2AsTypeId = asTypeService.getLevel2AsTypeById(asTypeId).getId();
            List<Integer> typeIdList = new ArrayList<>();
            //20221219  对下面流程的设备类型加严约束
            if (ObjectUtil.isNotEmpty(processName) && (processName.contains("存储介质借用"))){
               typeIdList.add(32);//红盘
               typeIdList.add(36);//U盘
            } else
                typeIdList = asTypeService.getTypeIdList(asTypeId);//获取当前类型ID及子类型的IDList
            queryWrapper.in("type_id", typeIdList);
        }
        if (ObjectUtil.isNotEmpty(name)) {
            queryWrapper.like("name", name);
        }
        if (ObjectUtil.isNotEmpty(netType)) {
            queryWrapper.eq("net_type", netType);
        }
        if (ObjectUtil.isNotEmpty(state)) {//如为资产选择界面传来的请求:此值为空
            queryWrapper.eq("state", state);
        }
        if(ObjectUtil.isNotEmpty(processName)){//20221109 如果processName有值：是从流程发起/资产选择界面传来的请求：此时state&user_dept的值需要特殊处理
            if (StrUtil.isNotEmpty(stateForExcludeForJsonStr)) {//20220817 stateForExcludeForJsonStr 原型是一个数组
                String bbb =stateForExcludeForJsonStr.replaceAll("[\"\\[\\]]","");
                List<String> stateForExcludeList = Stream.of( bbb.split(",")).collect(Collectors.toList());
                queryWrapper.notIn("state", stateForExcludeList);
            }
            if (ObjectUtil.isNotEmpty(userDept)) {
                String deptName = sysDeptService.getById(userDept).getName();
                //20221225 添加“申领”/"定密及启用"流程时：对“库存”与“归库”状态 && 部门名称的“组合约束”：归库不限本部门&&其他限
                //注：对于“外设申领”次设备（计算机）：是不过第一分支的：因为前
                if((processName.contains("申领") && !processName.contains("外设") ) || (processName.contains("申领") && processName.contains("外设") && level2AsTypeId != 4) || processName.contains("定密及启用") )
                    queryWrapper.and(qw->qw.eq("state","停用").eq("user_dept", deptName).or().eq("state","库存").eq("user_dept", deptName).or().eq("state","归库").or().eq("state","停用"));//.eq("user_dept", deptName)
                else if(processName.contains("外设") && level2AsTypeId == 4)//申领和变更时 ，计算机都需要是在用
                    queryWrapper.and(qw->qw.eq("state","在用").eq("user_dept", deptName));
                else if(processName.contains("密钥更换") && processFormCustomType.getName().contains("密钥信息表2"))
                    queryWrapper.eq("state","库存");//更换“新”密钥时不限制部门了
                else if(processName.contains("密钥更换") && processFormCustomType.getName().equals("密钥信息表"))
                    queryWrapper.eq("state","在用");
                else
                    queryWrapper.eq("user_dept", deptName);
            }
            //20221226 "借用"流程:增加过滤正在走审批的资产/机制：先组装/利用 processInstanceChange表中”is_finish==否“的设备IdList
            if(processName.contains("借用")){
                List<Map<String,Object>> listMaps1 = processInstanceDataService.listMaps(new QueryWrapper<ProcessInstanceData>().ne("process_status","完成").eq("process_name","借用").select("Distinct act_process_instance_id"));
                List<String> actProcInstIdList = listMaps1.stream().map(item->item.get("act_process_instance_id").toString()).collect(Collectors.toList());
                if(CollUtil.isNotEmpty(actProcInstIdList)){//in语句条件List的size为0会报错
                    List<Map<String,Object>> listMaps2 = processFormValue2Service.listMaps(new QueryWrapper<ProcessFormValue2>().in("act_process_instance_id",actProcInstIdList).select("Distinct as_id"));
                    List<Integer> unFininshedAsIdList2 = listMaps2.stream().map(item->Integer.parseInt(item.get("as_id").toString())).collect(Collectors.toList());
                    queryWrapper.notIn("id",unFininshedAsIdList2);
                }
            }

        } else {
            if (ObjectUtil.isNotEmpty(userDept)) {
                String deptName = sysDeptService.getById(userDept).getName();
                queryWrapper.eq("user_dept", deptName);
            }

        }

        if (ObjectUtil.isNotEmpty(userName)) {
            queryWrapper.like("user_name", userName);
        }
  
        IPage<AsDeviceCommon> page = asDeviceCommonService.page(new Page<>(currentPage, pageSize), queryWrapper);
        //20211115临时借用temp字段存储设备类型
        page.getRecords().stream().forEach(item ->item.setTemp(asTypeService.getById(item.getTypeId()).getName()));
        //对下面两类流程的设备状态与部门的组合约束干预
//        if(ObjectUtil.isNotEmpty(processName) && (processName.contains("申领")||processName.contains("定密及启用"))){
//            if (ObjectUtil.isNotEmpty(userDept)) {
//                String deptName = sysDeptService.getById(userDept).getName();
//                page.getRecords().stream().filter(item->{
//                    return (item.getState().equals("归库")||(item.getState().equals("库存") && ObjectUtil.isEmpty(item.getUserDept()))||((item.getState().equals("库存") && item.getUserDept().equals(deptName))));
//                }).collect(Collectors.toList());
//            }
//        }

        return page;
    }

    @GetMapping("getAsDeviceCommonNoVL")
    public List<ValueLabelVO> getAsDeviceCommonNoVL(Integer userDeptId, Integer customTableId, String stateForExcludeForJsonStr) {
        QueryWrapper<AsDeviceCommon> queryWrapper = new QueryWrapper<>();
        queryWrapper.ne("type_id",30);//20220905把硬盘排除
        if (ObjectUtil.isNotEmpty(customTableId)) {//20220716
            String str = processFormCustomTypeService.getById(customTableId).getProps();
            JSONObject jsonObject = JSON.parseObject(str);
            int asTypeId = jsonObject.getInteger("asTypeId");
            List<Integer> typeIdList = asTypeService.getTypeIdList(asTypeId);//获取当前类型ID及子类型的IDList
            queryWrapper.in("type_id", typeIdList);
        }
        if (StrUtil.isNotEmpty(stateForExcludeForJsonStr)) {//20220817
            String bbb =stateForExcludeForJsonStr.replaceAll("[\"\\[\\]]","");
            List<String> stateForExcludeList = Stream.of( bbb.split(",")).collect(Collectors.toList());
            queryWrapper.notIn("state", stateForExcludeList);
        }
        if (ObjectUtil.isNotEmpty(userDeptId)) {
            String deptName = sysDeptService.getById(userDeptId).getName();
            queryWrapper.eq("user_dept", deptName);
        }
        List<AsDeviceCommon> list = asDeviceCommonService.list(queryWrapper);
        return list.stream().map(item -> new ValueLabelVO(item.getNo(), item.getNo())).collect(Collectors.toList());
    }



    @GetMapping("getLevelTwoAsTypeById")
    public AsType getLevel2AsTypeById(Integer typeId) {
        return asTypeService.getLevel2AsTypeById(typeId);
    }

//    @GetMapping("getByNo")
//    public AsDeviceCommon getByNo(String no) {
//        if (ObjectUtil.isNotEmpty(no)) {
//            List<AsDeviceCommon> list = asDeviceCommonService.list(new QueryWrapper<AsDeviceCommon>().eq("no", no));
//            if (CollUtil.isNotEmpty(list))
//                return list.get(0);
//        }
//        return null;
//    }


    //与前端path.js里路径对应，所有的页面都是用的这个，先不改了
    @GetMapping("get")
    public AssetVO get(Integer id,String no) {
        AssetVO assetVO = new AssetVO();
        AsDeviceCommon asDeviceCommon = new AsDeviceCommon();
        QueryWrapper<AsDeviceCommon> queryWrapper = new QueryWrapper<>();
        if(ObjectUtil.isNotEmpty(id))
            queryWrapper.eq("id",id);
        if(ObjectUtil.isNotEmpty(no))
            queryWrapper.eq("no",no);
       List<AsDeviceCommon> list = asDeviceCommonService.list(queryWrapper);
       if(CollUtil.isNotEmpty(list))
           asDeviceCommon = list.get(0);
        assetVO.setAsDeviceCommon(asDeviceCommon);
        //资产类型：去查了相应的第二层typeID,但未处理typeID是第一级的情况 ，这里还有问题：应用系统分类肯定是第一级，这里未处理，其实放在eles里就行，那个资产签怎么展示呢？
        AsType asType = getLevel2AsTypeById(asDeviceCommon.getTypeId());
        Integer asTypeId = asType.getId();
        if (asTypeId == 4 || asTypeId == 29) {
            //计算机
            AsComputerSpecial asComputerSpecial = asComputerSpecialService.getOne(new QueryWrapper<AsComputerSpecial>().eq("as_id", id));
            if (asComputerSpecial != null) {
                assetVO.setAsComputerSpecial(asComputerSpecial);
            }
            AsComputerGranted asComputerGranted = asComputerGrantedService.getOne(new QueryWrapper<AsComputerGranted>().eq("as_id", id));
            if (asComputerGranted != null) {
                assetVO.setAsComputerGranted(asComputerGranted);
            }
            //202206112加载硬盘信息
            List<AsDeviceCommon> diskList = asDeviceCommonService.list(new QueryWrapper<AsDeviceCommon>().eq("host_as_id", id));
            assetVO.setDiskList(diskList);
        } else if (asTypeId == 5) {
            //网络设备
            AsNetworkDeviceSpecial asNetworkDeviceSpecial = asNetworkDeviceSpecialService.getOne(new QueryWrapper<AsNetworkDeviceSpecial>().eq("as_id", id));
            if (asNetworkDeviceSpecial != null) {
                assetVO.setAsNetworkDeviceSpecial(asNetworkDeviceSpecial);
            }
        } else if (asTypeId == 6) {
            //外设
            AsIoSpecial asIoSpecial = asIoSpecialService.getOne(new QueryWrapper<AsIoSpecial>().eq("as_id", id));
            if (asIoSpecial != null) {
                assetVO.setAsIoSpecial(asIoSpecial);
            }
        } else if (asTypeId == 7) {
            //安全防护产品
            AsSecurityProductsSpecial asSecurityProductsSpecial = asSecurityProductsSpecialService.getOne(new QueryWrapper<AsSecurityProductsSpecial>().eq("as_id", id));
            if (asSecurityProductsSpecial != null) {
                assetVO.setAsSecurityProductsSpecial(asSecurityProductsSpecial);
            }
        } else if (asTypeId == 19) {
            //应用系统 20211121
            AsApplicationSpecial asApplicationSpecial = asApplicationSpecialService.getOne(new QueryWrapper<AsApplicationSpecial>().eq("as_id", id));
            if (asApplicationSpecial != null) {
                assetVO.setAsApplicationSpecial(asApplicationSpecial);
            }
        }

        return assetVO;
    }

    @OperateLog(module = "资产模块", type = "添加资产")
    @PostMapping("add")
    public boolean add(@RequestBody AssetVO assetVO) {
        return asDeviceCommonService.add(assetVO);
    }

    @OperateLog(module = "资产模块", type = "编辑资产")
    @PostMapping("edit")
    public boolean edit(@RequestBody AssetVO assetVO) {
        return asDeviceCommonService.edit(assetVO);
    }
    /*
    下载标签数据  20230207这个上传/下载相关action的日志注解可能后续要去除：目前做法是已在日志切面类中通过过滤“含‘上传/下载’”的type将这些“触发”过滤
     打印标签列表todo屏敝“无密级”
    */
    @OperateLog(module = "设备模块", type = "标签数据下载")
    @GetMapping("download222")
    @SneakyThrows
    public void downloadBaomiLab222(HttpServletResponse response,int [] selectedKeys) {

        List<BaomiLabExcel> data0List = new ArrayList<>();
        WriteSheet sheet0 = EasyExcel.writerSheet(0, "保密标签").head(BaomiLabExcel.class).build();

        //20222315 测试选择列 可用
        //List<String> a  = new ArrayList<>();
        //a.add("no");
        // WriteSheet sheet4 = EasyExcel.writerSheet(4, "存储介质").includeColumnFiledNames(a).head(StorageExcel.class).build();
      //  WriteSheet sheet4 = EasyExcel.writerSheet(4, "存储介质").head(StorageExcel.class).build();
        //20221214 测试导出excel中加数据列：已，很容易
        if(selectedKeys.length>0){
            List<AsDeviceCommon> asDeviceCommonList = asDeviceCommonService.list(new QueryWrapper<AsDeviceCommon>().in("id",Arrays.stream(selectedKeys).boxed().toArray( Integer[]::new )));
            for(AsDeviceCommon asDeviceCommon : asDeviceCommonList){
                BaomiLabExcel tmp = new BaomiLabExcel();
                BeanUtils.copyProperties(asDeviceCommon,tmp);
                AsType asType = asTypeService.getById(asDeviceCommon.getTypeId());
                AsType asTypeLevel2 = asTypeService.getLevel2AsTypeById(asType.getId());
                String asTypeName = asType.getName();
                String title = "三十三所  ";
                if("桌面计算机".equals(asTypeName)){
                    title = title + "办公用计算机";
                } else{
                    if(asTypeLevel2.getId()==6){//代表“外设声像及自动化”
                        title = title + "办公设备";
                    } else if(asTypeLevel2.getId()==31){//存储介质
                        title = title + "存储介质/"+ asTypeName;

                    } else
                        title = title + asTypeName;
                }

                tmp.setTitle(title);
                tmp.setType(asDeviceCommon.getNetType());//类别改为联网类别
                tmp.setFormat("baomiLab.btw");
                tmp.setPrinter("Brother QL-570");//注意打印中心东院是9800，如果东西院打印机型号不一样，那就手动改成一个名如这个“9500”
                data0List.add(tmp);
            }
        }
        response.setContentType("text/csv;charset=gb2312");
        response.setCharacterEncoding("GB2312");
        String fileName = "保密标签（非密）";//20230206测试专用火狐设置的:经测这个方法火狐与谷歌都适用，本来想还通过 request来判断浏览器类型区分的，现在暂不用
        response.setHeader("Content-disposition", "attachment;filename=" +new String(fileName.getBytes("GB2312"),"ISO-8859-1")  + ".csv");
        ExcelWriter excelWriter = EasyExcel.write(response.getOutputStream()).useDefaultStyle(false).excelType(ExcelTypeEnum.CSV).build();
        //
        excelWriter.write(data0List, sheet0);
        //
        excelWriter.finish();
    }

    @OperateLog(module = "设备模块", type = "标签数据下载")
    @GetMapping("download2")
    @SneakyThrows
    public void downloadBaomiLab(HttpServletResponse response,int [] selectedKeys) {
        List<BaomiLabExcel>  data0List = new ArrayList<>();
        if(selectedKeys.length>0){
            List<AsDeviceCommon> asDeviceCommonList = asDeviceCommonService.list(new QueryWrapper<AsDeviceCommon>().in("id",Arrays.stream(selectedKeys).boxed().toArray( Integer[]::new )));
            for(AsDeviceCommon asDeviceCommon : asDeviceCommonList){
                BaomiLabExcel tmp = new BaomiLabExcel();
                BeanUtils.copyProperties(asDeviceCommon,tmp);
                AsType asType = asTypeService.getById(asDeviceCommon.getTypeId());
                AsType asTypeLevel2 = asTypeService.getLevel2AsTypeById(asType.getId());
                String asTypeName = asType.getName();
                String name =  asDeviceCommon.getName();//20230423
                String titlePrefix = "三十三所  ";
                String title = "";
                title = titlePrefix + asTypeName;//20230423
                if("办公计算机".equals(asTypeName)){
                    title = titlePrefix + "办公用计算机";
                } else if(asTypeName.contains("其他")){//20230423
                    title = titlePrefix + name;//20230423
                }  if(asTypeLevel2.getId()==31){//存储介质
                        title = titlePrefix + "存储介质/"+ asTypeName;
                }

                tmp.setTitle(title);
                tmp.setType(asDeviceCommon.getNetType());//类别改为联网类别tmp.setType(asTypeName);//类别|联网类别 “来回切换”
                tmp.setFormat("baomiLab.btw");
                tmp.setPrinter("Brother QL-570");//注意打印中心东院是9800，如果东西院打印机型号不一样，那就手动改成一个名如这个“9500”
                data0List.add(tmp);
            }
        }
        response.setCharacterEncoding("GB2312");
        response.setHeader("Content-Disposition", "attachment;filename=" + new String("保密标签（非密）.csv".getBytes("UTF-8"), "ISO8859-1"));
        response.setContentType("text/csv");
        CsvWriter csvWriter = CsvUtil.getWriter(response.getWriter());
        csvWriter.writeBeans(data0List);
        csvWriter.close();
    }
    //选中计算机下载
//    @OperateLog(module = "设备模块", type = "选中计算机下载")
//    @GetMapping("download3forCMP")
//    @SneakyThrows
//    public void downloadCMP(HttpServletRequest request, HttpServletResponse response,Integer [] selectedKeys) {
//        //response.setContentType("application/vnd.ms-excel");
//        response.setContentType("application/vnd.ms-excel");
//        //  response.setCharacterEncoding("utf-8");
//        //String fileName = URLEncoder.encode("设备模板（非密）", "UTF-8");//20230206d原来的谷歌可用的
//        String fileName = "设备信息（非密）";//20230206测试专用火狐设置的:经测这个方法火狐与谷歌都适用，本来想还通过 request来判断浏览器类型区分的，现在暂不用
//        response.setHeader("Content-disposition", "attachment;filename=" +new String(fileName.getBytes("GB2312"),"ISO-8859-1")  + ".xlsx");
//        //表头策略使用默认 设置字体大小
//        WriteCellStyle headWriteCellStyle = new WriteCellStyle();
//        WriteFont headWriteFont = new WriteFont();
//        headWriteFont.setFontHeightInPoints((short) 12);
//
//        headWriteCellStyle.setFillForegroundColor(IndexedColors.RED.getIndex());//没起作用
//        headWriteCellStyle.setWriteFont(headWriteFont);
//        //headWriteCellStyle.setShrinkToFit(true);
//        //主体内容样式策略
//        WriteCellStyle contentWriteCellStyle = new WriteCellStyle();
//        //垂直居中,水平居中
//       contentWriteCellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
////        contentWriteCellStyle.setHorizontalAlignment(HorizontalAlignment.CENTER);
////        contentWriteCellStyle.setBorderLeft(BorderStyle.THIN);
////        contentWriteCellStyle.setBorderTop(BorderStyle.THIN);
////        contentWriteCellStyle.setBorderRight(BorderStyle.THIN);
////        contentWriteCellStyle.setBorderBottom(BorderStyle.THIN);
//        //设置 自动换行
//        //contentWriteCellStyle.setWrapped(true);
//        // 字体策略
//        WriteFont contentWriteFont = new WriteFont();
//        // 字体大小
//        contentWriteFont.setFontHeightInPoints((short) 12);
//        contentWriteCellStyle.setWriteFont(contentWriteFont);
//       // contentWriteCellStyle.setShrinkToFit(true);
//      //20230228 .useDefaultStyle(false)会有一些表头背景色等样式，未细测
//        ExcelWriter excelWriter = EasyExcel.write(response.getOutputStream()).registerWriteHandler((new HorizontalCellStyleStrategy(headWriteCellStyle,contentWriteCellStyle))).useDefaultStyle(false).excelType(ExcelTypeEnum.XLSX).build();
//        WriteSheet sheet0 = EasyExcel.writerSheet(0, "计算机").head(AsComputerDownload.class).build();
//        List<AsComputerDownload> data0List = new ArrayList<>();
//       // List<AsComputerExcel> asComputerExcelList = new ArrayList<>();
//        if(ArrayUtil.isNotEmpty(selectedKeys)){
//            List<Integer> list = Arrays.asList(selectedKeys);
//            List<AsDeviceCommon> asDeviceCommonList = asDeviceCommonService.list(new QueryWrapper<AsDeviceCommon>().in("id",Arrays.asList(selectedKeys)));
//            Integer order = 0;
//            for(AsDeviceCommon a : asDeviceCommonList){
//                AsComputerDownload asComputerDownload = new  AsComputerDownload();
//                asComputerDownload.setOrder(++order);
//                BeanUtils.copyProperties(a, asComputerDownload);
//                List<AsDeviceCommon> ypList = asDeviceCommonService.list(new QueryWrapper<AsDeviceCommon>().eq("host_as_id",a.getId()));
//                if (CollUtil.isNotEmpty(ypList)){
//                    asComputerDownload.setDiskSn(ypList.stream().map(item->item.getSn()).collect(Collectors.joining(",")));
//                }
//                asComputerDownload.setTypeName(asTypeService.getById(a.getTypeId()).getName());
//                data0List.add(asComputerDownload);
//              //  BeanUtils.copyProperties(asComputerExcel, asComputerSpecial);
//            }
//        }
//        excelWriter.write(data0List, sheet0);
//        excelWriter.finish();
//    }
    //计算机台账下载
    @OperateLog(module = "设备模块", type = "计算机台账下载")
    @GetMapping("downloadForCMP")
    @SneakyThrows
    public void downloadCMP(HttpServletRequest request, HttpServletResponse response, String queryParam) {//,AssetVO[] comlumns
        //response.setContentType("application/vnd.ms-excel");
        response.setContentType("application/vnd.ms-excel");
        //  response.setCharacterEncoding("utf-8");
        //String fileName = URLEncoder.encode("设备模板（非密）", "UTF-8");//20230206d原来的谷歌可用的
        String fileName = "设备信息（秘密）";//20230206测试专用火狐设置的:经测这个方法火狐与谷歌都适用，本来想还通过 request来判断浏览器类型区分的，现在暂不用
        response.setHeader("Content-disposition", "attachment;filename=" +new String(fileName.getBytes("GB2312"),"ISO-8859-1")  + ".xlsx");
        //表头策略使用默认 设置字体大小
        WriteCellStyle headWriteCellStyle = new WriteCellStyle();
        WriteFont headWriteFont = new WriteFont();
        headWriteFont.setFontHeightInPoints((short) 12);
        headWriteCellStyle.setFillForegroundColor(IndexedColors.RED.getIndex());//没起作用
        headWriteCellStyle.setWriteFont(headWriteFont);
        //headWriteCellStyle.setShrinkToFit(true);
        //主体内容样式策略
        WriteCellStyle contentWriteCellStyle = new WriteCellStyle();
        //垂直居中,水平居中
        contentWriteCellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
//        contentWriteCellStyle.setHorizontalAlignment(HorizontalAlignment.CENTER);
//        contentWriteCellStyle.setBorderLeft(BorderStyle.THIN);
//        contentWriteCellStyle.setBorderTop(BorderStyle.THIN);
//        contentWriteCellStyle.setBorderRight(BorderStyle.THIN);
//        contentWriteCellStyle.setBorderBottom(BorderStyle.THIN);
        //设置 自动换行
        //contentWriteCellStyle.setWrapped(true);
        // 字体策略
        WriteFont contentWriteFont = new WriteFont();
        // 字体大小
        contentWriteFont.setFontHeightInPoints((short) 12);
        contentWriteCellStyle.setWriteFont(contentWriteFont);
        // contentWriteCellStyle.setShrinkToFit(true);
//20230228 减去了.useDefaultStyle(false)
        ExcelWriter excelWriter = EasyExcel.write(response.getOutputStream()).registerWriteHandler((new HorizontalCellStyleStrategy(headWriteCellStyle,contentWriteCellStyle))).useDefaultStyle(false).excelType(ExcelTypeEnum.XLSX).build();
        List<AsComputerDownload> data0List = new ArrayList<>();
        // List<AsComputerExcel> asComputerExcelList = new ArrayList<>();
        String t1;
        String t2;
        QueryWrapper queryWrapper = new QueryWrapper<AsDeviceCommon>();
        String selectedColumnsStr = "";
        if(ObjectUtil.isNotEmpty(queryParam)){
            t1 = queryParam.replace("*","{");
            t2 = t1.replace("@","}");
            JSONObject jsonObject = JSON.parseObject(t2);
            if (jsonObject.containsKey("typeId")){//json取值先判断有无KEY，否则报空指针
                List<Integer> typeIdList = asTypeService.getTypeIdList(jsonObject.getInteger("typeId"));//获取当前类型ID及子类型的IDList
                queryWrapper.in("type_id",typeIdList);
            }
            if (jsonObject.containsKey("no")){//json取值先判断有无KEY，否则报空指针
                queryWrapper.like("no",jsonObject.getString("no"));
            }
            if (jsonObject.containsKey("miji")){//json取值先判断有无KEY，否则报空指针
                queryWrapper.eq("miji",jsonObject.getString("miji"));
            }
            if (jsonObject.containsKey("state")){//json取值先判断有无KEY，否则报空指针
                queryWrapper.eq("state",jsonObject.getString("state"));
            }
            if (jsonObject.containsKey("userDept")){//json取值先判断有无KEY，否则报空指针
                queryWrapper.eq("user_dept", sysDeptService.getById(jsonObject.getString("userDept")).getName());
            }
            if (jsonObject.containsKey("userName")){//json取值先判断有无KEY，否则报空指针
                queryWrapper.eq("user_name",jsonObject.getString("userName"));
            }
            if (jsonObject.containsKey("selectedColumns")){//json取值先判断有无KEY，否则报空指针
                selectedColumnsStr = jsonObject.getString("selectedColumns");
            }
            if(jsonObject.containsKey("selectedRowKeys")){//ArrayUtil.isNotEmpty(selectedKeys)
                queryWrapper.in("id",Arrays.asList(jsonObject.getString("selectedRowKeys").split(",")));
            }

        }
        //20222315 测试选择列 可用
        List<String> tmpList;
        if(ObjUtil.isNotEmpty(selectedColumnsStr)){//选择了属性才下载;不选的话，这个action返回的数据“ 不完整”导致前端打开EXCEL时报错：暂不处理 && 前端对选项做控制

            tmpList = Arrays.asList(selectedColumnsStr.split(","));
            WriteSheet sheet0 = EasyExcel.writerSheet(0, "计算机").includeColumnFiledNames(tmpList).head(AsComputerDownload.class).build();
            List<AsDeviceCommon> asDeviceCommonList = asDeviceCommonService.list(queryWrapper);
            Integer order = 0;
            if(CollUtil.isEmpty(asDeviceCommonList)){
                AsComputerDownload asComputerDownload = new  AsComputerDownload();
                asComputerDownload.setOrder(++order);
                asComputerDownload.setNo("筛选结果为空！");
                data0List.add(asComputerDownload);
                //  throw new RuntimeException("列表为空，请重新选择筛选条件！");//这个没反应，可能是下载页面的特殊性：暂不研
            }

            for(AsDeviceCommon a : asDeviceCommonList){
                AsComputerDownload asComputerDownload = new  AsComputerDownload();
                asComputerDownload.setOrder(++order);
                BeanUtils.copyProperties(a, asComputerDownload);
                //todo参考导入资产时那个日期转化函数：写一个localdate转字符串
                //处理日期类型
                ExcelDateUtil.dateConverToString(a, asComputerDownload, AsDeviceCommon.class);
                List<AsComputerSpecial> asComputerSpecialList = asComputerSpecialService.list(new QueryWrapper<AsComputerSpecial>().eq("as_id",a.getId()));
                if(CollUtil.isNotEmpty(asComputerSpecialList)){
                    AsComputerSpecial asComputerSpecial = asComputerSpecialList.get(0);
                    BeanUtils.copyProperties(asComputerSpecial, asComputerDownload);
                    ExcelDateUtil.dateConverToString(asComputerSpecial, asComputerDownload, AsComputerSpecial.class);
                }
                //asComputerDownload.setUseDate("2022/01/10");
                if(tmpList.contains("diskSn")){
                    List<AsDeviceCommon> ypList = asDeviceCommonService.list(new QueryWrapper<AsDeviceCommon>().eq("host_as_id",a.getId()));
                    if (CollUtil.isNotEmpty(ypList)){
                        asComputerDownload.setDiskSn(ypList.stream().map(item->item.getSn()).collect(Collectors.joining(",")));
                    }
                }

                asComputerDownload.setTypeName(asTypeService.getById(a.getTypeId()).getName());
                data0List.add(asComputerDownload);
                //  BeanUtils.copyProperties(asComputerExcel, asComputerSpecial);
            }
            excelWriter.write(data0List, sheet0);
            excelWriter.finish();

        }

        //tmpList.add("no");
        // WriteSheet sheet4 = EasyExcel.writerSheet(4, "存储介质").includeColumnFiledNames(a).head(StorageExcel.class).build();

    }
    //选中外设下载
//    @OperateLog(module = "设备模块", type = "选中外设下载")
//    @GetMapping("download3forAff")
//    @SneakyThrows
//    public void downloadAff(HttpServletRequest request, HttpServletResponse response,Integer [] selectedKeys) {
//        //response.setContentType("application/vnd.ms-excel");
//        response.setContentType("application/vnd.ms-excel");
//        //  response.setCharacterEncoding("utf-8");
//        //String fileName = URLEncoder.encode("设备模板（非密）", "UTF-8");//20230206d原来的谷歌可用的
//        String fileName = "设备信息（非密）";//20230206测试专用火狐设置的:经测这个方法火狐与谷歌都适用，本来想还通过 request来判断浏览器类型区分的，现在暂不用
//        response.setHeader("Content-disposition", "attachment;filename=" +new String(fileName.getBytes("GB2312"),"ISO-8859-1")  + ".xlsx");
//        //表头策略使用默认 设置字体大小
//        WriteCellStyle headWriteCellStyle = new WriteCellStyle();
//        WriteFont headWriteFont = new WriteFont();
//        headWriteFont.setFontHeightInPoints((short) 12);
//
//        headWriteCellStyle.setFillForegroundColor(IndexedColors.RED.getIndex());//没起作用
//        headWriteCellStyle.setWriteFont(headWriteFont);
//        //headWriteCellStyle.setShrinkToFit(true);
//        //主体内容样式策略
//        WriteCellStyle contentWriteCellStyle = new WriteCellStyle();
//        //垂直居中,水平居中
//        contentWriteCellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
////        contentWriteCellStyle.setHorizontalAlignment(HorizontalAlignment.CENTER);
////        contentWriteCellStyle.setBorderLeft(BorderStyle.THIN);
////        contentWriteCellStyle.setBorderTop(BorderStyle.THIN);
////        contentWriteCellStyle.setBorderRight(BorderStyle.THIN);
////        contentWriteCellStyle.setBorderBottom(BorderStyle.THIN);
//        //设置 自动换行
//        //contentWriteCellStyle.setWrapped(true);
//        // 字体策略
//        WriteFont contentWriteFont = new WriteFont();
//        // 字体大小
//        contentWriteFont.setFontHeightInPoints((short) 12);
//        contentWriteCellStyle.setWriteFont(contentWriteFont);
//        // contentWriteCellStyle.setShrinkToFit(true);
//        //20230228 .useDefaultStyle(false)会有一些表头背景色等样式，未细测
//        ExcelWriter excelWriter = EasyExcel.write(response.getOutputStream()).registerWriteHandler((new HorizontalCellStyleStrategy(headWriteCellStyle,contentWriteCellStyle))).useDefaultStyle(false).excelType(ExcelTypeEnum.XLSX).build();
//        WriteSheet sheet0 = EasyExcel.writerSheet(0, "外设及办公自动化").head(AsAffDownload.class).build();
//        List<AsAffDownload> data0List = new ArrayList<>();
//        // List<AsComputerExcel> asComputerExcelList = new ArrayList<>();
//        if(ArrayUtil.isNotEmpty(selectedKeys)){
//            List<Integer> list = Arrays.asList(selectedKeys);
//            List<AsDeviceCommon> asDeviceCommonList = asDeviceCommonService.list(new QueryWrapper<AsDeviceCommon>().in("id",Arrays.asList(selectedKeys)));
//            Integer order = 0;
//            for(AsDeviceCommon a : asDeviceCommonList){
//                AsAffDownload asAffDownload = new  AsAffDownload();
//                asAffDownload.setOrder(++order);
//                BeanUtils.copyProperties(a, asAffDownload);
//                asAffDownload.setTypeName(asTypeService.getById(a.getTypeId()).getName());
//                List<AsIoSpecial> ioSpecialList = asIoSpecialService.list(new QueryWrapper<AsIoSpecial>().eq("as_id",(a.getId())));
//                if(CollUtil.isNotEmpty(ioSpecialList)){
//                    asAffDownload.setAccessHostNo(ioSpecialList.get(0).getAccessHostNo());
//                }
//                data0List.add(asAffDownload);
//                //  BeanUtils.copyProperties(asComputerExcel, asComputerSpecial);
//            }
//        }
//        excelWriter.write(data0List, sheet0);
//        excelWriter.finish();
//    }
    //外设台账下载
    @OperateLog(module = "设备模块", type = "外设台账下载")
    @GetMapping("downloadForAff")
    @SneakyThrows
    public void downloadAff(HttpServletRequest request, HttpServletResponse response, String queryParam) {
        //response.setContentType("application/vnd.ms-excel");
        response.setContentType("application/vnd.ms-excel");
        //  response.setCharacterEncoding("utf-8");
        //String fileName = URLEncoder.encode("设备模板（非密）", "UTF-8");//20230206d原来的谷歌可用的
        String fileName = "设备信息（非密）";//20230206测试专用火狐设置的:经测这个方法火狐与谷歌都适用，本来想还通过 request来判断浏览器类型区分的，现在暂不用
        response.setHeader("Content-disposition", "attachment;filename=" +new String(fileName.getBytes("GB2312"),"ISO-8859-1")  + ".xlsx");
        //表头策略使用默认 设置字体大小
        WriteCellStyle headWriteCellStyle = new WriteCellStyle();
        WriteFont headWriteFont = new WriteFont();
        headWriteFont.setFontHeightInPoints((short) 12);

        headWriteCellStyle.setFillForegroundColor(IndexedColors.RED.getIndex());//没起作用
        headWriteCellStyle.setWriteFont(headWriteFont);
        //headWriteCellStyle.setShrinkToFit(true);
        //主体内容样式策略
        WriteCellStyle contentWriteCellStyle = new WriteCellStyle();
        //垂直居中,水平居中
        contentWriteCellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        // 字体策略
        WriteFont contentWriteFont = new WriteFont();
        // 字体大小
        contentWriteFont.setFontHeightInPoints((short) 12);
        contentWriteCellStyle.setWriteFont(contentWriteFont);
        // contentWriteCellStyle.setShrinkToFit(true);


//20230228 减去了.useDefaultStyle(false)
        ExcelWriter excelWriter = EasyExcel.write(response.getOutputStream()).registerWriteHandler((new HorizontalCellStyleStrategy(headWriteCellStyle,contentWriteCellStyle))).useDefaultStyle(false).excelType(ExcelTypeEnum.XLSX).build();
        List<AsAffDownload> data0List = new ArrayList<>();
        // List<AsComputerExcel> asComputerExcelList = new ArrayList<>();
        String t1;
        String t2;
        QueryWrapper queryWrapper =new QueryWrapper<AsDeviceCommon>();
        String selectedColumnsStr = "";
        if(ObjectUtil.isNotEmpty(queryParam)){
            t1 = queryParam.replace("*","{");
            t2 = t1.replace("@","}");
            JSONObject jsonObject = JSON.parseObject(t2);
            if (jsonObject.containsKey("typeId")){//json取值先判断有无KEY，否则报空指针
                List<Integer> typeIdList = asTypeService.getTypeIdList(jsonObject.getInteger("typeId"));//获取当前类型ID及子类型的IDList
                queryWrapper.in("type_id",typeIdList);
            }
            if (jsonObject.containsKey("no")){//json取值先判断有无KEY，否则报空指针
                queryWrapper.eq("no",jsonObject.getString("no"));
            }
            if (jsonObject.containsKey("miji")){//json取值先判断有无KEY，否则报空指针
                queryWrapper.eq("miji",jsonObject.getString("miji"));
            }
            if (jsonObject.containsKey("state")){//json取值先判断有无KEY，否则报空指针
                queryWrapper.eq("state",jsonObject.getString("state"));
            }
            if (jsonObject.containsKey("userDept")){//json取值先判断有无KEY，否则报空指针
                queryWrapper.eq("user_dept", sysDeptService.getById(jsonObject.getString("userDept")).getName());
            }
            if (jsonObject.containsKey("userName")){//json取值先判断有无KEY，否则报空指针
                queryWrapper.eq("user_name",jsonObject.getString("userName"));
            }
            if (jsonObject.containsKey("selectedColumns")){//json取值先判断有无KEY，否则报空指针
                selectedColumnsStr = jsonObject.getString("selectedColumns");
            }
            if(jsonObject.containsKey("selectedRowKeys")){//ArrayUtil.isNotEmpty(selectedKeys)
                queryWrapper.in("id",Arrays.asList(jsonObject.getString("selectedRowKeys").split(",")));
            }


        }
        List<String> tmpList;
        if(ObjUtil.isNotEmpty(selectedColumnsStr)) {//选择了属性才下载;不选的话，这个action返回的数据“ 不完整”导致前端打开EXCEL时报错：暂不处理 && 前端对选项做控制

            tmpList = Arrays.asList(selectedColumnsStr.split(","));
            WriteSheet sheet0 = EasyExcel.writerSheet(0, "外设及办公自动化").includeColumnFiledNames(tmpList).head(AsAffDownload.class).build();
            List<AsDeviceCommon> asDeviceCommonList = asDeviceCommonService.list(queryWrapper);
            Integer order = 0;
            if (CollUtil.isEmpty(asDeviceCommonList)) {
                AsAffDownload asAffDownload = new AsAffDownload();
                asAffDownload.setOrder(++order);
                asAffDownload.setNo("筛选结果为空！");
                data0List.add(asAffDownload);
                //  throw new RuntimeException("列表为空，请重新选择筛选条件！");//这个没反应，可能是下载页面的特殊性：暂不研
            }

            for (AsDeviceCommon a : asDeviceCommonList) {
                AsAffDownload asAffDownload = new AsAffDownload();
                asAffDownload.setOrder(++order);
                BeanUtils.copyProperties(a, asAffDownload);
                //处理日期类型
                ExcelDateUtil.dateConverToString(a, asAffDownload,  AsAffDownload.class);
                asAffDownload.setTypeName(asTypeService.getById(a.getTypeId()).getName());
                List<AsIoSpecial> ioSpecialList = asIoSpecialService.list(new QueryWrapper<AsIoSpecial>().eq("as_id", (a.getId())));
                if (CollUtil.isNotEmpty(ioSpecialList)) {
                    AsIoSpecial asIoSpecial = ioSpecialList.get(0);
                    asAffDownload.setAccessHostNo(asIoSpecial.getAccessHostNo());
                    ExcelDateUtil.dateConverToString(asIoSpecial, asAffDownload, AsIoSpecial.class);
                }
                data0List.add(asAffDownload);
                //  BeanUtils.copyProperties(asComputerExcel, asComputerSpecial);
            }
            excelWriter.write(data0List, sheet0);
            excelWriter.finish();
        }
    }
    //选中资产下载
//    @OperateLog(module = "资产模块", type = "选中资产下载")
//    @GetMapping("download3forCommon")
//    @SneakyThrows
//    public void downloadCommon(HttpServletRequest request, HttpServletResponse response,Integer [] selectedKeys) {
//        //response.setContentType("application/vnd.ms-excel");
//        response.setContentType("application/vnd.ms-excel");
//        //  response.setCharacterEncoding("utf-8");
//        //String fileName = URLEncoder.encode("设备模板（非密）", "UTF-8");//20230206d原来的谷歌可用的
//        String fileName = "资产信息（非密）";//20230206测试专用火狐设置的:经测这个方法火狐与谷歌都适用，本来想还通过 request来判断浏览器类型区分的，现在暂不用
//        response.setHeader("Content-disposition", "attachment;filename=" +new String(fileName.getBytes("GB2312"),"ISO-8859-1")  + ".xlsx");
//        //表头策略使用默认 设置字体大小
//        WriteCellStyle headWriteCellStyle = new WriteCellStyle();
//        WriteFont headWriteFont = new WriteFont();
//        headWriteFont.setFontHeightInPoints((short) 12);
//
//        headWriteCellStyle.setFillForegroundColor(IndexedColors.RED.getIndex());//没起作用
//        headWriteCellStyle.setWriteFont(headWriteFont);
//        //headWriteCellStyle.setShrinkToFit(true);
//        //主体内容样式策略
//        WriteCellStyle contentWriteCellStyle = new WriteCellStyle();
//        //垂直居中,水平居中
//        contentWriteCellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
////        contentWriteCellStyle.setHorizontalAlignment(HorizontalAlignment.CENTER);
////        contentWriteCellStyle.setBorderLeft(BorderStyle.THIN);
////        contentWriteCellStyle.setBorderTop(BorderStyle.THIN);
////        contentWriteCellStyle.setBorderRight(BorderStyle.THIN);
////        contentWriteCellStyle.setBorderBottom(BorderStyle.THIN);
//        //设置 自动换行
//        //contentWriteCellStyle.setWrapped(true);
//        // 字体策略
//        WriteFont contentWriteFont = new WriteFont();
//        // 字体大小
//        contentWriteFont.setFontHeightInPoints((short) 12);
//        contentWriteCellStyle.setWriteFont(contentWriteFont);
//        // contentWriteCellStyle.setShrinkToFit(true);
//        //20230228 .useDefaultStyle(false)会有一些表头背景色等样式，未细测
//        ExcelWriter excelWriter = EasyExcel.write(response.getOutputStream()).registerWriteHandler((new HorizontalCellStyleStrategy(headWriteCellStyle,contentWriteCellStyle))).useDefaultStyle(false).excelType(ExcelTypeEnum.XLSX).build();
//        WriteSheet sheet0 = EasyExcel.writerSheet(0, "资产列表").head(AsCommonDownload.class).build();
//        List<AsCommonDownload> data0List = new ArrayList<>();
//        // List<AsComputerExcel> asComputerExcelList = new ArrayList<>();
//        if(ArrayUtil.isNotEmpty(selectedKeys)){
//            List<Integer> list = Arrays.asList(selectedKeys);
//            List<AsDeviceCommon> asDeviceCommonList = asDeviceCommonService.list(new QueryWrapper<AsDeviceCommon>().in("id",Arrays.asList(selectedKeys)));
//            Integer order = 0;
//            for(AsDeviceCommon a : asDeviceCommonList){
//                AsCommonDownload asCommonDownload = new  AsCommonDownload();
//                asCommonDownload.setOrder(++order);
//                BeanUtils.copyProperties(a, asCommonDownload);
//                asCommonDownload.setTypeName(asTypeService.getById(a.getTypeId()).getName());
//                data0List.add(asCommonDownload);
//                //  BeanUtils.copyProperties(asComputerExcel, asComputerSpecial);
//            }
//        }
//        excelWriter.write(data0List, sheet0);
//        excelWriter.finish();
//    }
    //资产台账表下载
    @OperateLog(module = "资产模块", type = "资产台账下载")
    @GetMapping("downloadForCommon")
    @SneakyThrows
    public void downloadCommon(HttpServletRequest request, HttpServletResponse response, String queryParam) {
        //response.setContentType("application/vnd.ms-excel");
        response.setContentType("application/vnd.ms-excel");
        //  response.setCharacterEncoding("utf-8");
        //String fileName = URLEncoder.encode("设备模板（非密）", "UTF-8");//20230206d原来的谷歌可用的
        String fileName = "资产信息（非密）";//20230206测试专用火狐设置的:经测这个方法火狐与谷歌都适用，本来想还通过 request来判断浏览器类型区分的，现在暂不用
        response.setHeader("Content-disposition", "attachment;filename=" +new String(fileName.getBytes("GB2312"),"ISO-8859-1")  + ".xlsx");
        //表头策略使用默认 设置字体大小
        WriteCellStyle headWriteCellStyle = new WriteCellStyle();
        WriteFont headWriteFont = new WriteFont();
        headWriteFont.setFontHeightInPoints((short) 12);

        headWriteCellStyle.setFillForegroundColor(IndexedColors.RED.getIndex());//没起作用
        headWriteCellStyle.setWriteFont(headWriteFont);
        //headWriteCellStyle.setShrinkToFit(true);
        //主体内容样式策略
        WriteCellStyle contentWriteCellStyle = new WriteCellStyle();
        //垂直居中,水平居中
        contentWriteCellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
//        contentWriteCellStyle.setHorizontalAlignment(HorizontalAlignment.CENTER);
//        contentWriteCellStyle.setBorderLeft(BorderStyle.THIN);
//        contentWriteCellStyle.setBorderTop(BorderStyle.THIN);
//        contentWriteCellStyle.setBorderRight(BorderStyle.THIN);
//        contentWriteCellStyle.setBorderBottom(BorderStyle.THIN);
        //设置 自动换行
        //contentWriteCellStyle.setWrapped(true);
        // 字体策略
        WriteFont contentWriteFont = new WriteFont();
        // 字体大小
        contentWriteFont.setFontHeightInPoints((short) 12);
        contentWriteCellStyle.setWriteFont(contentWriteFont);
        // contentWriteCellStyle.setShrinkToFit(true);


//20230228 减去了.useDefaultStyle(false)
        ExcelWriter excelWriter = EasyExcel.write(response.getOutputStream()).registerWriteHandler((new HorizontalCellStyleStrategy(headWriteCellStyle,contentWriteCellStyle))).useDefaultStyle(false).excelType(ExcelTypeEnum.XLSX).build();
        List<AsCommonDownload> data0List = new ArrayList<>();
        // List<AsComputerExcel> asComputerExcelList = new ArrayList<>();
        String t1;
        String t2;
        String selectedColumnsStr = "";
        QueryWrapper queryWrapper =new QueryWrapper<AsDeviceCommon>();
        if(ObjectUtil.isNotEmpty(queryParam)){
            t1 = queryParam.replace("*","{");
            t2 = t1.replace("@","}");
            JSONObject jsonObject = JSON.parseObject(t2);
            if (jsonObject.containsKey("typeId")){//json取值先判断有无KEY，否则报空指针
                List<Integer> typeIdList = asTypeService.getTypeIdList(jsonObject.getInteger("typeId"));//获取当前类型ID及子类型的IDList
                queryWrapper.in("type_id",typeIdList);
            }
            if (jsonObject.containsKey("no")){//json取值先判断有无KEY，否则报空指针
                queryWrapper.eq("no",jsonObject.getString("no"));
            }
            if (jsonObject.containsKey("miji")){//json取值先判断有无KEY，否则报空指针
                queryWrapper.eq("miji",jsonObject.getString("miji"));
            }
            if (jsonObject.containsKey("state")){//json取值先判断有无KEY，否则报空指针
                queryWrapper.eq("state",jsonObject.getString("state"));
            }
            if (jsonObject.containsKey("userDept")){//json取值先判断有无KEY，否则报空指针
                queryWrapper.eq("user_dept", sysDeptService.getById(jsonObject.getString("userDept")).getName());//20230425
            }
            if (jsonObject.containsKey("userName")){//json取值先判断有无KEY，否则报空指针
                queryWrapper.eq("user_name",jsonObject.getString("userName"));
            }
            if (jsonObject.containsKey("selectedColumns")){//json取值先判断有无KEY，否则报空指针
                selectedColumnsStr = jsonObject.getString("selectedColumns");
            }
            if(jsonObject.containsKey("selectedRowKeys")){//ArrayUtil.isNotEmpty(selectedKeys)
                queryWrapper.in("id",Arrays.asList(jsonObject.getString("selectedRowKeys").split(",")));
            }


        }
        List<String> tmpList;
        if(ObjUtil.isNotEmpty(selectedColumnsStr)) {
            tmpList = Arrays.asList(selectedColumnsStr.split(","));
            WriteSheet sheet0 = EasyExcel.writerSheet(0, "资产列表").includeColumnFiledNames(tmpList).head(AsCommonDownload.class).build();
            List<AsDeviceCommon> asDeviceCommonList = asDeviceCommonService.list(queryWrapper);
            Integer order = 0;
            if (CollUtil.isEmpty(asDeviceCommonList)) {
                AsCommonDownload asCommonDownload = new AsCommonDownload();
                asCommonDownload.setOrder(++order);
                asCommonDownload.setNo("筛选结果为空！");
                data0List.add(asCommonDownload);
                //  throw new RuntimeException("列表为空，请重新选择筛选条件！");//这个没反应，可能是下载页面的特殊性：暂不研
            }

            for (AsDeviceCommon a : asDeviceCommonList) {
                AsCommonDownload asCommonDownload = new AsCommonDownload();
                asCommonDownload.setOrder(++order);
                BeanUtils.copyProperties(a, asCommonDownload);
                ExcelDateUtil.dateConverToString(a, asCommonDownload, AsDeviceCommon.class);
                asCommonDownload.setTypeName(asTypeService.getById(a.getTypeId()).getName());
                data0List.add(asCommonDownload);
                //  BeanUtils.copyProperties(asComputerExcel, asComputerSpecial);
            }
            excelWriter.write(data0List, sheet0);
            excelWriter.finish();
        }
    }

    //下载设备模板
    @OperateLog(module = "设备模块", type = "设备模板下载")
    @GetMapping("download1")
    @SneakyThrows
    public void downloadTemplate(HttpServletRequest request, HttpServletResponse response) {
        //response.setContentType("application/vnd.ms-excel");
        response.setContentType("application/vnd.ms-excel");
      //  response.setCharacterEncoding("utf-8");
        //String fileName = URLEncoder.encode("设备模板（非密）", "UTF-8");//20230206d原来的谷歌可用的
        String fileName = "设备模板（非密）";//20230206测试专用火狐设置的:经测这个方法火狐与谷歌都适用，本来想还通过 request来判断浏览器类型区分的，现在暂不用
        response.setHeader("Content-disposition", "attachment;filename=" +new String(fileName.getBytes("GB2312"),"ISO-8859-1")  + ".xls");
        //
        ExcelWriter excelWriter = EasyExcel.write(response.getOutputStream()).useDefaultStyle(false).excelType(ExcelTypeEnum.XLS).build();
        //
        List<AsComputerExcel> data0List = new ArrayList<>();
        WriteSheet sheet0 = EasyExcel.writerSheet(0, "计算机").head(AsComputerExcel.class).build();
        List<AsNetworkDeviceExcel> data1List = new ArrayList<>();
        WriteSheet sheet1 = EasyExcel.writerSheet(1, "网络设备").head(AsNetworkDeviceExcel.class).build();
        List<AsSecurityProductExcel> data2List = new ArrayList<>();
        WriteSheet sheet2 = EasyExcel.writerSheet(2, "安全产品").head(AsSecurityProductExcel.class).build();
        List<AsIoExcel> data3List = new ArrayList<>();
        WriteSheet sheet3 = EasyExcel.writerSheet(3, "外部设备").head(AsIoExcel.class).build();
        List<StorageExcel> data4List = new ArrayList<>();
        //20222315 测试选择列 可用
        //List<String> a  = new ArrayList<>();
        //a.add("no");
       // WriteSheet sheet4 = EasyExcel.writerSheet(4, "存储介质").includeColumnFiledNames(a).head(StorageExcel.class).build();
        WriteSheet sheet4 = EasyExcel.writerSheet(4, "存储介质").head(StorageExcel.class).build();
        //20221214 测试导出excel中加数据列：已，很容易
//        AsComputerExcel a1 = new AsComputerExcel();
//        a1.setNo("AAAA");
//        data0List.add(a1);
        excelWriter.write(data0List, sheet0);
        excelWriter.write(data1List, sheet1);
        excelWriter.write(data2List, sheet2);
        excelWriter.write(data3List, sheet3);
        excelWriter.write(data4List, sheet4);
        //
        excelWriter.finish();
    }

    //上传设备
    @OperateLog(module = "资产模块", type = "上传资产")
    @PostMapping("upload1")
    @SneakyThrows
    public List<String> importAsset(MultipartFile[] files, String formValue) {
        MultipartFile file = files[0];
        //20230323 测试写入本地磁盘：竟然如此简单：就下面一句即可
//        file.transferTo(new File("D:/"+file.getOriginalFilename()));
//        return null;


        InputStream inputStream = file.getInputStream();
        //
        ExcelReader excelReader = EasyExcel.read(inputStream).build();
        //
        ExcelListener<AsComputerExcel> listener0 = new ExcelListener<>();
        ExcelListener<AsNetworkDeviceExcel> listener1 = new ExcelListener<>();
        ExcelListener<AsSecurityProductExcel> listener2 = new ExcelListener<>();
        ExcelListener<AsIoExcel> listener3 = new ExcelListener<>();
        ExcelListener<StorageExcel> listener4 = new ExcelListener<>();
        //获取sheet对象
        ReadSheet sheet0 = EasyExcel.readSheet("计算机").head(AsComputerExcel.class).registerReadListener(listener0).build();
        ReadSheet sheet1 = EasyExcel.readSheet("网络设备").head(AsNetworkDeviceExcel.class).registerReadListener(listener1).build();
        ReadSheet sheet2 = EasyExcel.readSheet("安全产品").head(AsSecurityProductExcel.class).registerReadListener(listener2).build();
        ReadSheet sheet3 = EasyExcel.readSheet("外部设备").head(AsIoExcel.class).registerReadListener(listener3).build();
        ReadSheet sheet4 = EasyExcel.readSheet("存储介质").head(StorageExcel.class).registerReadListener(listener4).build();
        //读取数据
        excelReader.read(sheet0, sheet1, sheet2, sheet3, sheet4);
        //获取数据
        List<AsComputerExcel> list0 = listener0.getData();
        List<AsNetworkDeviceExcel> list1 = listener1.getData();
        List<AsSecurityProductExcel> list2 = listener2.getData();
        List<AsIoExcel> list3 = listener3.getData();
        List<StorageExcel> list4 = listener4.getData();

        //表单
        JSONObject jsonObject = JSON.parseObject(formValue);
        String importMode = jsonObject.getString("importMode");

        return asDeviceCommonService.addExcel(list0, list1, list2, list3, list4, importMode);
    }
}
