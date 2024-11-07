package com.sss.yunweiadmin.controller;


import cn.hutool.core.util.ObjectUtil;
import cn.hutool.extra.servlet.ServletUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.support.ExcelTypeEnum;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.alibaba.excel.write.metadata.style.WriteCellStyle;
import com.alibaba.excel.write.metadata.style.WriteFont;
import com.alibaba.excel.write.style.HorizontalCellStyleStrategy;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sss.yunweiadmin.common.config.GlobalParam;
import com.sss.yunweiadmin.common.operate.OperateLog;
import com.sss.yunweiadmin.common.result.ResponseResultWrapper;
import com.sss.yunweiadmin.common.utils.ExcelDateUtil;
import com.sss.yunweiadmin.model.entity.OperateeLog;
import com.sss.yunweiadmin.model.entity.SysUser;
import com.sss.yunweiadmin.model.excel.OperateLogDownload;
import com.sss.yunweiadmin.service.OperateeLogService;
import lombok.SneakyThrows;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * <p>
 * 用户操作记录日志 前端控制器
 * </p>
 *
 * @author 任勇林
 * @since 2021-10-18
 */
@RestController
@ResponseResultWrapper
@RequestMapping("/operateeLog")
public class OperateeLogController {
    @Autowired
    OperateeLogService operateeLogService;
    @Autowired
    HttpSession httpSession;
    @GetMapping("list")
    public IPage<OperateeLog> list(int currentPage, int pageSize, String displayName,String loginName,String dateRange){
        QueryWrapper<OperateeLog> queryWrapper = new  QueryWrapper<OperateeLog>().eq("org_id", GlobalParam.orgId).orderByDesc("id");
        if (ObjectUtil.isNotEmpty(displayName)) {
            queryWrapper.eq("display_name", displayName);
        }
        if (ObjectUtil.isNotEmpty(loginName)) {
            queryWrapper.eq("login_name",loginName);
        }
        if (ObjectUtil.isNotEmpty(dateRange)) {
            String[] dateArr = dateRange.split(",");
            queryWrapper.ge("create_datetime", dateArr[0] + " 00:00:00");
            queryWrapper.le("create_datetime", dateArr[1] + " 00:00:00");

        }
        //20230712过滤三员可见日志
        SysUser user = (SysUser) httpSession.getAttribute("user");
        if(user.getRoleIdList().contains(2))//安全员
            queryWrapper.notIn("login_name", Arrays.asList(new String[]{"admin_yw", "system_yw"}));
        else if(user.getRoleIdList().contains(3))//审计员
            queryWrapper.in("login_name", Arrays.asList(new String[]{"admin_yw", "system_yw"}));
        IPage<OperateeLog> page = operateeLogService.page(new Page<>(currentPage, pageSize), queryWrapper);
        return page;
    }

    //日志下载;20231205 可能因为这是导出：无法记录日志
    @OperateLog(module = "日志模块", type = "导出日志")
    @GetMapping("download1")
    @SneakyThrows
    public void download1(HttpServletRequest request, HttpServletResponse response, String selectedKeys, String displayName,String loginName,String dateRange) {//,AssetVO[] comlumns
        //response.setContentType("application/vnd.ms-excel");
        response.setContentType("application/vnd.ms-excel");
        //  response.setCharacterEncoding("utf-8");
        //String fileName = URLEncoder.encode("设备模板（非密）", "UTF-8");//20230206d原来的谷歌可用的
        String fileName = "审计日志（秘密）";//20230206测试专用火狐设置的:经测这个方法火狐与谷歌都适用，本来想还通过 request来判断浏览器类型区分的，现在暂不用
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
        ExcelWriter excelWriter = EasyExcel.write(response.getOutputStream()).registerWriteHandler((new HorizontalCellStyleStrategy(headWriteCellStyle, contentWriteCellStyle))).useDefaultStyle(false).excelType(ExcelTypeEnum.XLSX).build();
        List<OperateLogDownload> data0List = new ArrayList<>();

        QueryWrapper<OperateeLog> queryWrapper = new  QueryWrapper<OperateeLog>().eq("org_id",GlobalParam.orgId).orderByDesc("id");
        if (ObjectUtil.isNotEmpty(displayName)) {
            queryWrapper.eq("display_name", displayName);
        }
        if (ObjectUtil.isNotEmpty(loginName)) {
            queryWrapper.eq("login_name",loginName);
        }
        if (ObjectUtil.isNotEmpty(selectedKeys)) {
            queryWrapper.in("id", Arrays.asList(selectedKeys.split(",")));
        }
        if (ObjectUtil.isNotEmpty(dateRange)) {
            String[] dateArr = dateRange.split(",");
            queryWrapper.ge("create_datetime", dateArr[0] + " 00:00:00");
            queryWrapper.le("create_datetime", dateArr[1] + " 00:00:00");

        }
        //20230826过滤三员可见日志
        SysUser user = (SysUser) httpSession.getAttribute("user");
        //20231205 手工写日志
        OperateeLog operateeLog = new OperateeLog();
        String paramStr = "日志范围：未限制";
        if (ObjectUtil.isNotEmpty(dateRange)) {
            String[] dateArr = dateRange.split(",");
            paramStr = "日志范围从 " + dateArr[0] + " 00:00:00" + "到 " + dateArr[1] + " 00:00:00";
        }
        operateeLog.setParam(paramStr);
        operateeLog.setOperateModule("日志模块");
        operateeLog.setOperateType("日志导出");
        operateeLog.setLoginName(user.getLoginName());
        operateeLog.setDisplayName(user.getDisplayName());
        //operateeLog.setIp(ServletUtil.getClientIP(request));
        String ip = (String) httpSession.getAttribute("IP");
        if(ObjectUtil.isEmpty(ip))
            ip = ServletUtil.getClientIP(request);
        operateeLog.setIp(ip);
        operateeLog.setMethod("com.sss.yunweiadmin.controller.download1()");
        operateeLog.setCreateDatetime(LocalDateTime.now());
        operateeLogService.save(operateeLog);

        if(user.getRoleIdList().contains(2))//安全员
            queryWrapper.notIn("login_name", Arrays.asList(new String[]{"admin_yw", "system_yw"}));
        else if(user.getRoleIdList().contains(3))//审计员
            queryWrapper.in("login_name", Arrays.asList(new String[]{"admin_yw", "system_yw"}));
        WriteSheet sheet0 = EasyExcel.writerSheet(0, "日志").head(OperateLogDownload.class).build();
        List<OperateeLog> operateeLogList = operateeLogService.list(queryWrapper);
        Integer order = 0;
        for (OperateeLog a : operateeLogList) {
            OperateLogDownload operateLogDownload = new OperateLogDownload();
            operateLogDownload.setOrder(++order);
            BeanUtils.copyProperties(a, operateLogDownload);
            //todo参考导入资产时那个日期转化函数：写一个localdate转字符串
            //处理日期类型
            ExcelDateUtil.dateConverToString(a, operateLogDownload, OperateeLog.class);
            data0List.add(operateLogDownload);
        }
        excelWriter.write(data0List, sheet0);
        excelWriter.finish();

    }
}
