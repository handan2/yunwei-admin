package com.sss.yunweiadmin.controller;


import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelReader;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.read.metadata.ReadSheet;
import com.alibaba.excel.support.ExcelTypeEnum;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.alibaba.excel.write.metadata.style.WriteCellStyle;
import com.alibaba.excel.write.metadata.style.WriteFont;
import com.alibaba.excel.write.style.HorizontalCellStyleStrategy;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.common.base.Strings;
import com.sss.yunweiadmin.common.config.GlobalParam;
import com.sss.yunweiadmin.common.operate.OperateLog;
import com.sss.yunweiadmin.common.result.ResponseResultWrapper;
import com.sss.yunweiadmin.common.utils.ExcelDateUtil;
import com.sss.yunweiadmin.model.entity.Inspection;
import com.sss.yunweiadmin.model.entity.InfoNo;
import com.sss.yunweiadmin.model.entity.Inspection;
import com.sss.yunweiadmin.model.excel.AsAffDownload;
import com.sss.yunweiadmin.model.excel.InspectionDownload;
import com.sss.yunweiadmin.model.excel.ExcelListener;
import com.sss.yunweiadmin.model.excel.InspectionExcel;
import com.sss.yunweiadmin.model.vo.ValueLabelVO;
import com.sss.yunweiadmin.service.InfoNoService;
import com.sss.yunweiadmin.service.InspectionService;
import com.sss.yunweiadmin.service.SysDeptService;
import lombok.SneakyThrows;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author 任勇林
 * @since 2024-06-23
 */
@RestController
@ResponseResultWrapper
@RequestMapping("/inspection")
public class InspectionController {
    @Autowired
    private InfoNoService infoNoService;
    @Autowired
    private InspectionService inspectionService;
    @Autowired
    SysDeptService sysDeptService;

    @GetMapping("list")
    public IPage<Inspection> list(int currentPage, int pageSize, String netType, String inspector, String inspectDate, String mode, String userDept, String userName, String no) {
        QueryWrapper<Inspection> queryWrapper = new  QueryWrapper<Inspection>().eq("org_id",GlobalParam.orgId).orderByDesc("id");
        if (!Strings.isNullOrEmpty(netType)) {
            queryWrapper.eq("net_type", netType);
        }
        if (!Strings.isNullOrEmpty(inspector)) {
            queryWrapper.like("inspector", inspector);
        }
        if (!Strings.isNullOrEmpty(mode)) {
            queryWrapper.eq("mode", mode);
        }
        if (!Strings.isNullOrEmpty(userDept)) {
            String deptName = sysDeptService.getById(userDept).getName();
            queryWrapper.like("user_dept", deptName);
        }
        if (!Strings.isNullOrEmpty(userName)) {
            queryWrapper.like("user_name", userName);
        }
        if (!Strings.isNullOrEmpty(no)) {
            queryWrapper.like("no", no);
        }
        if (ObjectUtil.isNotEmpty(inspectDate)) {
            String[] dateArr = inspectDate.split(",");
            queryWrapper.ge("inspect_date", dateArr[0] + " 00:00:00");
            queryWrapper.le("inspect_date", dateArr[1] + " 00:00:00");

        }
        return inspectionService.page(new Page<>(currentPage, pageSize), queryWrapper);
    }
    @PostMapping("add")
    public boolean add(@RequestBody InfoNo InfoNo) {
        return infoNoService.save(InfoNo);
    }


//    @GetMapping("add1")
//    public boolean add1() {
//        InfoNo a =new InfoNo();
//        a.setValue("aaaa");
//        a.setNetType("国密网");
//        a.setD("2022/08/25");
//        return infoNoService.save(a);
//    }

    @GetMapping("get")
    public Inspection getById(String id) {
        return inspectionService.getById(id);
    }

    @PostMapping("edit")
    public boolean edit(@RequestBody Inspection insp) {
        return inspectionService.updateById(insp);
    }

    @GetMapping("delete")
    public boolean delete(Integer[] idArr) {
        List<Integer> idList = Stream.of(idArr).collect(Collectors.toList());
        return inspectionService.removeByIds(idList);

    }


    //    @ResponseBody
    @GetMapping("getInfoNoVL")
    @ResponseResultWrapper
    //是那种选择流程发起责任人时的提示框组件内容
    //20211203完善：限定了查询本部门的人（查询条件可从前台传也可直接读session）;value里把人员密级也带了进去
    public List<ValueLabelVO> getInfoNoVL(String netType, String miji) {
        List<ValueLabelVO> list = new ArrayList<>();
        QueryWrapper<InfoNo> queryWrapper = new  QueryWrapper<InfoNo>().eq("org_id",GlobalParam.orgId).eq("org_id", GlobalParam.orgId);
        if(StrUtil.isNotEmpty(netType))//20221007目前前端申领流程中没有传这个参数
            queryWrapper.eq("net_type", netType);
        if(StrUtil.isNotEmpty(miji)) {
            if(miji.equals("秘密")||miji.equals("机密")) {
                queryWrapper.eq("net_type", "国密网");
                queryWrapper.eq("status","空闲");
            }
            else if(miji.equals("普通商密"))//试验网可以多个机器用一个信息点号
                queryWrapper.eq("net_type", "试验网");
            else if(miji.equals("非密")) {
                queryWrapper.eq("net_type", "商密网");
                queryWrapper.eq("status","空闲");
            }
        }

        List<InfoNo> infoNoList = infoNoService.list(queryWrapper);
        return infoNoList.stream().map(infoNo -> new ValueLabelVO(infoNo.getValue(),infoNo.getValue())).collect(Collectors.toList());
        // return infoNoList.stream().map(infoNo -> new ValueLabelVO(infoNo.getValue()+"."+infoNo.getNetType(),infoNo.getValue()+"."+infoNo.getNetType())).collect(Collectors.toList());
    }

    //上传信息点号
//    @OperateLog(module = "系统维护模块", type = "上传信息点号")
    @PostMapping("upload1")
    @SneakyThrows
    public List<String> importInsp(MultipartFile[] files, String formValue) {
        MultipartFile file = files[0];
        InputStream inputStream = file.getInputStream();
        //
        ExcelReader excelReader = EasyExcel.read(inputStream).build();
        //
        ExcelListener<InspectionExcel> listener = new ExcelListener<>();

        //获取sheet对象
        ReadSheet sheet = EasyExcel.readSheet("检查结果").head(InspectionExcel.class).registerReadListener(listener).build();

        //读取数据
        excelReader.read(sheet);
        //获取数据
        List<InspectionExcel> list = listener.getData();
        //表单
        JSONObject jsonObject = JSON.parseObject(formValue);
        String importMode = jsonObject.getString("importMode");
        List<String> resultList = new ArrayList<>();
        resultList.add(inspectionService.addExcel(list, importMode));
        return resultList;
    }

    //下载检查结果模板
    @OperateLog(module = "设备管理模块", type = "检查结果模板下载")
    @GetMapping("download1")
    @SneakyThrows
    public void downloadTemplate(HttpServletResponse response) {
        response.setContentType("application/vnd.ms-excel");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("检查问题模板（非密）", "UTF-8");
        response.setHeader("Content-disposition", "attachment;filename=" + fileName + ".xls");
        //
        ExcelWriter excelWriter = EasyExcel.write(response.getOutputStream()).useDefaultStyle(false).excelType(ExcelTypeEnum.XLS).build();
        //
        List<InspectionExcel> dataList = new ArrayList<>();
        WriteSheet sheet = EasyExcel.writerSheet(0, "检查结果").head(InspectionExcel.class).build();

        //
        excelWriter.write(dataList, sheet);

        //
        excelWriter.finish();
    }

    //自查结果下载
    @OperateLog(module = "设备模块", type = "自查结果下载")
    @GetMapping("downloadForInsp")
    @SneakyThrows
    public void downloadInspection(HttpServletRequest request, HttpServletResponse response, String queryParam) {
        //response.setContentType("application/vnd.ms-excel");
        response.setContentType("application/vnd.ms-excel");
        //  response.setCharacterEncoding("utf-8");
        //String fileName = URLEncoder.encode("设备模板（非密）", "UTF-8");//20230206d原来的谷歌可用的
        String fileName = "自查结果（非密）";//20230206测试专用火狐设置的:经测这个方法火狐与谷歌都适用，本来想还通过 request来判断浏览器类型区分的，现在暂不用
        response.setHeader("Content-disposition", "attachment;filename=" + new String(fileName.getBytes("GB2312"), "ISO-8859-1") + ".xlsx");
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
        ExcelWriter excelWriter = EasyExcel.write(response.getOutputStream()).registerWriteHandler((new HorizontalCellStyleStrategy(headWriteCellStyle, contentWriteCellStyle))).useDefaultStyle(false).excelType(ExcelTypeEnum.XLSX).build();
        List<InspectionDownload> data0List = new ArrayList<>();
        // List<AsComputerExcel> asComputerExcelList = new ArrayList<>();
        String t1;
        String t2;
        QueryWrapper queryWrapper = new  QueryWrapper<Inspection>().eq("org_id",GlobalParam.orgId);
        String selectedColumnsStr = "";
        if (ObjectUtil.isNotEmpty(queryParam)) {
            t1 = queryParam.replace("*", "{");
            t2 = t1.replace("@", "}");
            JSONObject jsonObject = JSON.parseObject(t2);
//            if (jsonObject.containsKey("typeId")) {//json取值先判断有无KEY，否则报空指针
//                List<Integer> typeIdList = asTypeService.getTypeIdList(jsonObject.getInteger("typeId"));//获取当前类型ID及子类型的IDList
//                queryWrapper.in("type_id", typeIdList);
//            }
//            if (jsonObject.containsKey("no")) {//json取值先判断有无KEY，否则报空指针
//                queryWrapper.eq("no", jsonObject.getString("no"));
//            }
//            if (jsonObject.containsKey("miji")) {//json取值先判断有无KEY，否则报空指针
//                queryWrapper.eq("miji", jsonObject.getString("miji"));
//            }
//            if (jsonObject.containsKey("state")) {//json取值先判断有无KEY，否则报空指针
//                queryWrapper.eq("state", jsonObject.getString("state"));
//            }
//            if (jsonObject.containsKey("userDept")) {//json取值先判断有无KEY，否则报空指针
//                queryWrapper.eq("user_dept", sysDeptService.getById(jsonObject.getString("userDept")).getName());
//            }
//            if (jsonObject.containsKey("userName")) {//json取值先判断有无KEY，否则报空指针
//                queryWrapper.eq("user_name", jsonObject.getString("userName"));
//            }
//            if (jsonObject.containsKey("netType")) {//json取值先判断有无KEY，否则报空指针
//                queryWrapper.eq("net_type", jsonObject.getString("netType"));
//            }
//            if (jsonObject.containsKey("selectedColumns")) {//json取值先判断有无KEY，否则报空指针
//                selectedColumnsStr = jsonObject.getString("selectedColumns");
//            }
//            if (jsonObject.containsKey("selectedRowKeys")) {//ArrayUtil.isNotEmpty(selectedKeys)
//                queryWrapper.in("id", Arrays.asList(jsonObject.getString("selectedRowKeys").split(",")));
//            }
        }
        List<String> tmpList;
      //  if (ObjUtil.isNotEmpty(selectedColumnsStr)) {//选择了属性才下载;不选的话，这个action返回的数据“ 不完整”导致前端打开EXCEL时报错：暂不处理 && 前端对选项做控制

          // tmpList = Arrays.asList(selectedColumnsStr.split(","));
            WriteSheet sheet0 = EasyExcel.writerSheet(0, "检查结果").head(InspectionDownload.class).build();//.includeColumnFiledNames(tmpList)
            List<Inspection> inspectionList = inspectionService.list(queryWrapper);
            Integer order = 0;
            if (CollUtil.isEmpty(inspectionList)) {
                InspectionDownload InspectionDownload = new InspectionDownload();
                InspectionDownload.setOrder(++order);
                InspectionDownload.setNo("筛选结果为空！");
                data0List.add(InspectionDownload);
                //  throw new RuntimeException("列表为空，请重新选择筛选条件！");//这个没反应，可能是下载页面的特殊性：暂不研
            }

            for (Inspection a : inspectionList) {
                InspectionDownload InspectionDownload = new InspectionDownload();
                InspectionDownload.setOrder(++order);
                BeanUtils.copyProperties(a, InspectionDownload);
                //处理日期类型
                ExcelDateUtil.dateConverToString(a, InspectionDownload, InspectionDownload.class);
               // InspectionDownload.setTypeName(asTypeService.getById(a.getTypeId()).getName());
//                List<AsIoSpecial> ioSpecialList = asIoSpecialService.list(new  QueryWrapper<AsIoSpecial>().eq("org_id",GlobalParam.orgId).eq("as_id", (a.getId())));
//                if (CollUtil.isNotEmpty(ioSpecialList)) {
//                    AsIoSpecial asIoSpecial = ioSpecialList.get(0);
//                    InspectionDownload.setAccessHostNo(asIoSpecial.getAccessHostNo());
//                    ExcelDateUtil.dateConverToString(asIoSpecial, InspectionDownload, AsIoSpecial.class);
//                }
                data0List.add(InspectionDownload);
                //  BeanUtils.copyProperties(asComputerExcel, asComputerSpecial);
            }
            excelWriter.write(data0List, sheet0);
            excelWriter.finish();
      //  }
    }

}
