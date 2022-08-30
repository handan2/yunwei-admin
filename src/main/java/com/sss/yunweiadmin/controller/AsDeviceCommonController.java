package com.sss.yunweiadmin.controller;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.ObjectUtil;
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
import com.sss.yunweiadmin.common.operate.OperateLog;
import com.sss.yunweiadmin.common.result.ResponseResultWrapper;
import com.sss.yunweiadmin.common.utils.TreeUtil;
import com.sss.yunweiadmin.model.entity.*;
import com.sss.yunweiadmin.model.excel.*;
import com.sss.yunweiadmin.model.vo.AssetVO;
import com.sss.yunweiadmin.model.vo.TreeSelectVO;
import com.sss.yunweiadmin.model.vo.ValueLabelVO;
import com.sss.yunweiadmin.service.*;
import com.sss.yunweiadmin.service.impl.AsTypeServiceImpl;
import lombok.SneakyThrows;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
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

    @GetMapping("test")
    public List<Integer> test(int typeId) {
        return asTypeService.getTypeIdList(typeId);
    }

    //20211115
    @OperateLog(module = "资产模块", type = "删除资产")
    @GetMapping("delete")
    public boolean delete(Integer[] idArr) {
        System.out.println(idArr[0]);
        return asDeviceCommonService.delete(idArr);
    }

    @GetMapping("list")
    public IPage<AsDeviceCommon> list(int currentPage, int pageSize, String no, Integer typeId, String name, String netType, String state, Integer userDeptId, String userName, Integer customTableId, String stateForExcludeForJsonStr) {
        QueryWrapper<AsDeviceCommon> queryWrapper = new QueryWrapper<>();
        if (ObjectUtil.isNotEmpty(no)) {
            queryWrapper.like("no", no);
        }
        if (ObjectUtil.isNotEmpty(typeId)) {
            List<Integer> typeIdList = asTypeService.getTypeIdList(typeId);
            queryWrapper.in("type_id", typeIdList);
        }
        if (ObjectUtil.isNotEmpty(customTableId)) {//20220716
            String str = processFormCustomTypeService.getById(customTableId).getProps();
            JSONObject jsonObject = JSON.parseObject(str);
            int asTypeId = jsonObject.getInteger("asTypeId");
            List<Integer> typeIdList = asTypeService.getTypeIdList(asTypeId);
            queryWrapper.in("type_id", typeIdList);
        }
        if (ObjectUtil.isNotEmpty(name)) {
            queryWrapper.like("name", name);
        }
        if (ObjectUtil.isNotEmpty(netType)) {
            queryWrapper.eq("net_type", netType);
        }
        if (ObjectUtil.isNotEmpty(state)) {
            queryWrapper.eq("state", state);
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
        if (ObjectUtil.isNotEmpty(userName)) {
            queryWrapper.eq("user_name", userName);
        }
        IPage<AsDeviceCommon> page = asDeviceCommonService.page(new Page<>(currentPage, pageSize), queryWrapper);
        //20211115临时借用temp字段存储设备类型
        page.getRecords().stream().forEach(item -> item.setTemp(asTypeService.getById(item.getTypeId()).getName()));
        return page;
    }

    @GetMapping("getAsDeviceCommonNoVL")
    public List<ValueLabelVO> getAsDeviceCommonNoVL() {
        List<AsDeviceCommon> list = asDeviceCommonService.list();
        return list.stream().map(item -> new ValueLabelVO(item.getNo(), item.getNo())).collect(Collectors.toList());
    }



    @GetMapping("getLevelTwoAsTypeById")
    public AsType getLevel2AsTypeById(Integer typeId) {
        return asTypeService.getLevel2AsTypeById(typeId);
    }

    //与前端path.js里路径对应，所有的页面都是用的这个，先不改了
    @GetMapping("get")
    public AssetVO getById(Integer id) {
        AssetVO assetVO = new AssetVO();
        AsDeviceCommon asDeviceCommon = asDeviceCommonService.getById(id);
        assetVO.setAsDeviceCommon(asDeviceCommon);
        //资产类型：去查了相应的第二层typeID,但未处理typeID是第一级的情况 ，这里还有问题：应用系统分类肯定是第一级，这里未处理，其实放在eles里就行，那个资产签怎么展示呢？
        AsType asType = getLevel2AsTypeById(asDeviceCommon.getTypeId());
        Integer asTypeId = asType.getId();
        if (asTypeId == 4) {
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

    //下载设备模板
    @OperateLog(module = "资产模块", type = "资产模板下载")
    @GetMapping("download1")
    @SneakyThrows
    public void downloadTemplate(HttpServletResponse response) {
        response.setContentType("application/vnd.ms-excel");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("设备模板（非密）", "UTF-8");
        response.setHeader("Content-disposition", "attachment;filename=" + fileName + ".xls");
        //
        ExcelWriter excelWriter = EasyExcel.write(response.getOutputStream()).useDefaultStyle(false).excelType(ExcelTypeEnum.XLS).build();
        //
        List<AsComputerExcel> data0List = new ArrayList<>();
        WriteSheet sheet0 = EasyExcel.writerSheet(0, "计算机").head(AsComputerExcel.class).build();
        List<AsNetworkDeviceSpecialExcel> data1List = new ArrayList<>();
        WriteSheet sheet1 = EasyExcel.writerSheet(1, "网络设备").head(AsNetworkDeviceSpecialExcel.class).build();
        List<AsSecurityProductsSpecialExcel> data2List = new ArrayList<>();
        WriteSheet sheet2 = EasyExcel.writerSheet(2, "安全产品").head(AsSecurityProductsSpecialExcel.class).build();
        List<AsIoSpecialExcel> data3List = new ArrayList<>();
        WriteSheet sheet3 = EasyExcel.writerSheet(3, "外部设备").head(AsIoSpecialExcel.class).build();
        //
        excelWriter.write(data0List, sheet0);
        excelWriter.write(data1List, sheet1);
        excelWriter.write(data2List, sheet2);
        excelWriter.write(data3List, sheet3);
        //
        excelWriter.finish();
    }

    //上传设备
    @OperateLog(module = "资产模块", type = "上传资产")
    @PostMapping("upload1")
    @SneakyThrows
    public List<String> importAsset(MultipartFile[] files, String formValue) {
        MultipartFile file = files[0];
        InputStream inputStream = file.getInputStream();
        //
        ExcelReader excelReader = EasyExcel.read(inputStream).build();
        //
        ExcelListener<AsComputerExcel> listener0 = new ExcelListener<>();
        ExcelListener<AsNetworkDeviceSpecialExcel> listener1 = new ExcelListener<>();
        ExcelListener<AsSecurityProductsSpecialExcel> listener2 = new ExcelListener<>();
        ExcelListener<AsIoSpecialExcel> listener3 = new ExcelListener<>();
        //获取sheet对象
        ReadSheet sheet0 = EasyExcel.readSheet("计算机").head(AsComputerExcel.class).registerReadListener(listener0).build();
        ReadSheet sheet1 = EasyExcel.readSheet("网络设备").head(AsNetworkDeviceSpecialExcel.class).registerReadListener(listener1).build();
        ReadSheet sheet2 = EasyExcel.readSheet("安全产品").head(AsSecurityProductsSpecialExcel.class).registerReadListener(listener2).build();
        ReadSheet sheet3 = EasyExcel.readSheet("外部设备").head(AsIoSpecialExcel.class).registerReadListener(listener3).build();
        //读取数据
        excelReader.read(sheet0, sheet1, sheet2, sheet3);
        //获取数据
        List<AsComputerExcel> list0 = listener0.getData();
        List<AsNetworkDeviceSpecialExcel> list1 = listener1.getData();
        List<AsSecurityProductsSpecialExcel> list2 = listener2.getData();
        List<AsIoSpecialExcel> list3 = listener3.getData();

        //表单
        JSONObject jsonObject = JSON.parseObject(formValue);
        String haveCover = jsonObject.getString("haveCover");

        return asDeviceCommonService.addExcel(list0, list1, list2, list3, haveCover);
    }
}
