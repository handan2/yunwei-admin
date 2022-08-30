package com.sss.yunweiadmin.controller;


import cn.hutool.core.util.StrUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelReader;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.read.metadata.ReadSheet;
import com.alibaba.excel.support.ExcelTypeEnum;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.common.base.Strings;
import com.sss.yunweiadmin.common.operate.OperateLog;
import com.sss.yunweiadmin.common.result.ResponseResultWrapper;
import com.sss.yunweiadmin.model.entity.*;
import com.sss.yunweiadmin.model.entity.InfoNo;
import com.sss.yunweiadmin.model.excel.*;
import com.sss.yunweiadmin.model.vo.ValueLabelVO;
import com.sss.yunweiadmin.service.InfoNoService;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.net.URLEncoder;
import java.sql.Struct;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author 任勇林
 * @since 2022-08-24
 */
@RestController
@RequestMapping("/infoNo")
@ResponseResultWrapper
public class InfoNoController {
    @Autowired
    private InfoNoService infoNoService;

    @GetMapping("list")
    public IPage<InfoNo> list(int currentPage, int pageSize, String netType, String value, String status, String location) {
        QueryWrapper<InfoNo> queryWrapper = new QueryWrapper<>();
        if (!Strings.isNullOrEmpty(netType)) {
            queryWrapper.eq("net_type", netType);
        }
        if (!Strings.isNullOrEmpty(value)) {
            queryWrapper.like("value", value);
        }
        if (!Strings.isNullOrEmpty(value)) {
            queryWrapper.eq("status", status);
        }
        if (!Strings.isNullOrEmpty(location)) {
            queryWrapper.like("location", location);
        }
        return infoNoService.page(new Page<>(currentPage, pageSize), queryWrapper);
    }
    @PostMapping("add")
    public boolean add(@RequestBody InfoNo InfoNo) {
        return infoNoService.save(InfoNo);
    }


    @GetMapping("add1")
    public boolean add1() {
        InfoNo a =new InfoNo();
        a.setValue("aaaa");
        a.setNetType("内网");
        a.setD("2022/08/25");
        return infoNoService.save(a);
    }

    @GetMapping("get")
    public InfoNo getById(String id) {
        return infoNoService.getById(id);
    }

    @PostMapping("edit")
    public boolean edit(@RequestBody InfoNo InfoNo) {
        return infoNoService.updateById(InfoNo);
    }

    @GetMapping("delete")
    public boolean delete(Integer[] idArr) {
        List<Integer> idList = Stream.of(idArr).collect(Collectors.toList());
        return infoNoService.removeByIds(idList);

    }


//    @ResponseBody
    @GetMapping("getInfoNoVL")
    @ResponseResultWrapper
    //是那种选择流程发起责任人时的提示框组件内容
    //20211203完善：限定了查询本部门的人（查询条件可从前台传也可直接读session）;value里把人员密级也带了进去
    public List<ValueLabelVO> getInfoNoVL(String netType,String miji) {
        List<ValueLabelVO> list = new ArrayList<>();
        QueryWrapper<InfoNo> queryWrapper = new QueryWrapper<>();
        if(StrUtil.isNotEmpty(netType))
            queryWrapper.eq("net_type", netType);
        queryWrapper.eq("status","空闲");
        if(StrUtil.isNotEmpty(miji)) {
            if(miji.equals("秘密")||miji.equals("机密"))
                queryWrapper.eq("net_type", "内网");
            else if(miji.equals("普通商密"))
                queryWrapper.eq("net_type", "试验网");
            else if(miji.equals("非密"))
                queryWrapper.eq("net_type", "商密网");
        }
        queryWrapper.eq("status","空闲");
        List<InfoNo> infoNoList = infoNoService.list(queryWrapper);
        return infoNoList.stream().map(infoNo -> new ValueLabelVO(infoNo.getValue(),infoNo.getValue())).collect(Collectors.toList());
       // return infoNoList.stream().map(infoNo -> new ValueLabelVO(infoNo.getValue()+"."+infoNo.getNetType(),infoNo.getValue()+"."+infoNo.getNetType())).collect(Collectors.toList());
    }

    //上传信息点号
//    @OperateLog(module = "系统维护模块", type = "上传信息点号")
    @PostMapping("upload1")
    @SneakyThrows
    public List<String> importInfoNo(MultipartFile[] files, String formValue) {
        MultipartFile file = files[0];
        InputStream inputStream = file.getInputStream();
        //
        ExcelReader excelReader = EasyExcel.read(inputStream).build();
        //
        ExcelListener<InfoNoExcel> listener = new ExcelListener<>();

        //获取sheet对象
        ReadSheet sheet = EasyExcel.readSheet("信息点号").head(InfoNoExcel.class).registerReadListener(listener).build();

        //读取数据
        excelReader.read(sheet);
        //获取数据
        List<InfoNoExcel> list = listener.getData();
        //表单
        JSONObject jsonObject = JSON.parseObject(formValue);
        String haveCover = jsonObject.getString("haveCover");
        List<String> resultList = new ArrayList<>();
        resultList.add(infoNoService.addExcel(list, haveCover));
        return resultList;
    }

    //下载信息点号模板
    @OperateLog(module = "系统维护模块", type = "信息点号模板下载")
    @GetMapping("download1")
    @SneakyThrows
    public void downloadTemplate(HttpServletResponse response) {
        response.setContentType("application/vnd.ms-excel");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("信息点号模板（非密）", "UTF-8");
        response.setHeader("Content-disposition", "attachment;filename=" + fileName + ".xls");
        //
        ExcelWriter excelWriter = EasyExcel.write(response.getOutputStream()).useDefaultStyle(false).excelType(ExcelTypeEnum.XLS).build();
        //
        List<InfoNoExcel> dataList = new ArrayList<>();
        WriteSheet sheet = EasyExcel.writerSheet(0, "信息点号").head(InfoNoExcel.class).build();

        //
        excelWriter.write(dataList, sheet);

        //
        excelWriter.finish();
    }
}
