package com.sss.yunweiadmin.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sss.yunweiadmin.common.utils.ExcelDateUtil;
import com.sss.yunweiadmin.mapper.AsDeviceCommonMapper;
import com.sss.yunweiadmin.model.entity.*;
import com.sss.yunweiadmin.model.excel.AsComputerExcel;
import com.sss.yunweiadmin.model.excel.AsIoSpecialExcel;
import com.sss.yunweiadmin.model.excel.AsNetworkDeviceSpecialExcel;
import com.sss.yunweiadmin.model.excel.AsSecurityProductsSpecialExcel;
import com.sss.yunweiadmin.model.vo.AssetVO;
import com.sss.yunweiadmin.service.*;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
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
    //20211115 不管啥类型，把所有专有表清清
    @Override
    public boolean delete(Integer[] idArr1){
        List<Integer> idArr = Stream.of(idArr1).collect(Collectors.toList());
        asComputerSpecialService.remove(new QueryWrapper<AsComputerSpecial>().in("as_id",idArr));
        asNetworkDeviceSpecialService.remove(new QueryWrapper<AsNetworkDeviceSpecial>().in("as_id",idArr));
        asComputerGrantedService.remove(new QueryWrapper<AsComputerGranted>().in("as_id",idArr));
        asIoSpecialService.remove(new QueryWrapper<AsIoSpecial>().in("as_id",idArr));
        asSecurityProductsSpecialService.remove(new QueryWrapper<AsSecurityProductsSpecial>().in("as_id",idArr));
        asApplicationSpecialService.remove(new QueryWrapper<AsApplicationSpecial>().in("as_id",idArr));
        this.removeByIds(idArr);
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
        if (asTypeId == 4) {
            //计算机
            AsComputerSpecial asComputerSpecial = assetVO.getAsComputerSpecial();
            if (asComputerSpecial != null) {
                asComputerSpecial.setAsId(asDeviceCommon.getId());
                flag = flag && asComputerSpecialService.save(asComputerSpecial);
            }
            AsComputerGranted asComputerGranted = assetVO.getAsComputerGranted();
            if (asComputerGranted != null) {
                asComputerGranted.setAsId(asDeviceCommon.getId());
                flag = flag && asComputerGrantedService.save(asComputerGranted);
            }
        } else if (asTypeId == 5) {
            //网络设备
            AsNetworkDeviceSpecial asNetworkDeviceSpecial = assetVO.getAsNetworkDeviceSpecial();
            if (asNetworkDeviceSpecial != null) {
                asNetworkDeviceSpecial.setAsId(asDeviceCommon.getId());
                flag = flag && asNetworkDeviceSpecialService.save(asNetworkDeviceSpecial);
            }
        } else if (asTypeId == 6) {
            //外设
            AsIoSpecial asIoSpecial = assetVO.getAsIoSpecial();
            if (asIoSpecial != null) {
                asIoSpecial.setAsId(asDeviceCommon.getId());
                flag = flag && asIoSpecialService.save(asIoSpecial);
            }
        } else if (asTypeId == 7) {
            //安全防护产品
            AsSecurityProductsSpecial asSecurityProductsSpecial = assetVO.getAsSecurityProductsSpecial();
            if (asSecurityProductsSpecial != null) {
                asSecurityProductsSpecial.setAsId(asDeviceCommon.getId());
                flag = flag && asSecurityProductsSpecialService.save(asSecurityProductsSpecial);
            }
        }else if (asTypeId == 19) {
            //应用系统
            AsApplicationSpecial asApplicationSpecial = assetVO.getAsApplicationSpecial();
            if (asApplicationSpecial != null) {
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
        flag = this.updateById(asDeviceCommon);
        //资产类型
        AsType asType = asTypeService.getLevel2AsTypeById(asDeviceCommon.getTypeId());
        Integer asTypeId = asType.getId();
        if (asTypeId == 4) {
            //计算机
            AsComputerSpecial asComputerSpecial = assetVO.getAsComputerSpecial();
            if (asComputerSpecial != null) {
                asComputerSpecial.setAsId(asDeviceCommon.getId());
                flag = flag && asComputerSpecialService.saveOrUpdate(asComputerSpecial);
            }
            AsComputerGranted asComputerGranted = assetVO.getAsComputerGranted();
            if (asComputerGranted != null) {
                asComputerGranted.setAsId(asDeviceCommon.getId());
                flag = flag && asComputerGrantedService.saveOrUpdate(asComputerGranted);
            }
        } else if (asTypeId == 5) {
            //网络设备
            AsNetworkDeviceSpecial asNetworkDeviceSpecial = assetVO.getAsNetworkDeviceSpecial();
            if (asNetworkDeviceSpecial != null) {
                asNetworkDeviceSpecial.setAsId(asDeviceCommon.getId());
                flag = flag && asNetworkDeviceSpecialService.saveOrUpdate(asNetworkDeviceSpecial);
            }
        } else if (asTypeId == 6) {
            //外设
            AsIoSpecial asIoSpecial = assetVO.getAsIoSpecial();
            if (asIoSpecial != null) {
                asIoSpecial.setAsId(asDeviceCommon.getId());
                flag = flag && asIoSpecialService.saveOrUpdate(asIoSpecial);
            }
        } else if (asTypeId == 7) {
            //安全防护产品
            AsSecurityProductsSpecial asSecurityProductsSpecial = assetVO.getAsSecurityProductsSpecial();
            if (asSecurityProductsSpecial != null) {
                asSecurityProductsSpecial.setAsId(asDeviceCommon.getId());
                flag = flag && asSecurityProductsSpecialService.saveOrUpdate(asSecurityProductsSpecial);
            }
        }else if (asTypeId == 19) {
            //应用系统
            AsApplicationSpecial asApplicationSpecial = assetVO.getAsApplicationSpecial();
            if (asApplicationSpecial != null) {
                asApplicationSpecial.setAsId(asDeviceCommon.getId());
                flag = flag && asApplicationSpecialService.saveOrUpdate(asApplicationSpecial);
            }
        }

        return flag;
    }

    private String addAsComputerExcel(List<AsComputerExcel> excelList, String haveCover) {
        //去掉db中存在的设备，剩下页面上需要导入的设备
        List<AsComputerExcel> pageList;
        //db中存在的设备
        List<AsDeviceCommon> dbList = new ArrayList<>();

        //处理日期类型
        ExcelDateUtil.converToDate(excelList, AsComputerExcel.class);
        /*
            haveCover=是，先删除，后全部插入设备
            haveCover=否，插入不在db中的设备
         */
        if (haveCover.equals("是")) {//可重复&覆盖，把重复的从DB删除
            List<AsDeviceCommon> list = this.list(new QueryWrapper<AsDeviceCommon>().in("no", excelList.stream().map(AsComputerExcel::getNo).collect(Collectors.toList())));
            if (ObjectUtil.isNotEmpty(list)) {
                dbList = list;
                List<Integer> asIdList = list.stream().map(AsDeviceCommon::getId).collect(Collectors.toList());
                this.removeByIds(asIdList);
                asComputerSpecialService.remove(new QueryWrapper<AsComputerSpecial>().in("as_id", asIdList));
                asComputerGrantedService.remove(new QueryWrapper<AsComputerGranted>().in("as_id", asIdList));
            }
            pageList = excelList;
        } else {//不可重复&覆盖
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
        if(ObjectUtil.isNotEmpty(pageList)) {
            List<AsDeviceCommon> asDeviceCommonList = new ArrayList<>();
            List<AsComputerSpecial> asComputerSpecialList = new ArrayList<>();
            List<AsComputerGranted> asComputerGrantedList = new ArrayList<>();

            for (AsComputerExcel asComputerExcel : pageList) {
                if (ObjectUtil.isEmpty(asComputerExcel.getNo())) {
                    throw new RuntimeException( "资产编号不能为空");
                }
                AsType asType = asTypeService.getAsType(asComputerExcel.getTypeName());
                if (asType == null) {
                    throw new RuntimeException("资产类别不存在");
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
                //20211116save之后会把主键ID写入asDeviceCommon
                this.save(asDeviceCommon);
                asComputerSpecial.setAsId(asDeviceCommon.getId());
                asComputerGranted.setAsId(asDeviceCommon.getId());
                //
                asDeviceCommonList.add(asDeviceCommon);
                asComputerSpecialList.add(asComputerSpecial);
                asComputerGrantedList.add(asComputerGranted);
            }
            //
            this.saveBatch(asDeviceCommonList);
            asComputerSpecialService.saveBatch(asComputerSpecialList);
            asComputerGrantedService.saveBatch(asComputerGrantedList);
        }
        if (haveCover.equals("是")) {
            int pageCount = pageList.size() - dbList.size();
            return "导入" + pageCount + "条资产;资产编号：" + dbList.stream().map(AsDeviceCommon::getNo).collect(Collectors.joining(",")) + ",已经被覆盖";
        } else {
            int pageCount = pageList.size();
            if (ObjectUtil.isEmpty(dbList)) {
                return "导入" + pageCount + "条资产";
            } else {
                return "导入" + pageCount + "条资产;资产编号：" + dbList.stream().map(AsDeviceCommon::getNo).collect(Collectors.joining(",")) + "已经存在，未导入";
            }
        }
    }

    private String addAsNetworkDeviceSpecialExcel(List<AsNetworkDeviceSpecialExcel> excelList, String haveCover) {
        //去掉db中存在的设备，剩下页面上需要导入的设备
        List<AsNetworkDeviceSpecialExcel> pageList;
        //db中存在的设备
        List<AsDeviceCommon> dbList = new ArrayList<>();

        //处理日期类型
        ExcelDateUtil.converToDate(excelList, AsNetworkDeviceSpecialExcel.class);
        /*
            haveCover=是，先删除，后全部插入设备
            haveCover=否，插入不在db中的设备
         */
        if (haveCover.equals("是")) {
            List<AsDeviceCommon> list = this.list(new QueryWrapper<AsDeviceCommon>().in("no", excelList.stream().map(AsNetworkDeviceSpecialExcel::getNo).collect(Collectors.toList())));
            if (ObjectUtil.isNotEmpty(list)) {
                dbList = list;
                List<Integer> asIdList = list.stream().map(AsDeviceCommon::getId).collect(Collectors.toList());
                this.removeByIds(asIdList);
                asNetworkDeviceSpecialService.remove(new QueryWrapper<AsNetworkDeviceSpecial>().in("as_id", asIdList));
            }
            pageList = excelList;
        } else {
            List<AsDeviceCommon> list = this.list(new QueryWrapper<AsDeviceCommon>().in("no", excelList.stream().map(AsNetworkDeviceSpecialExcel::getNo).collect(Collectors.toList())));
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
        for (AsNetworkDeviceSpecialExcel asNetworkDeviceSpecialExcel : pageList) {
            if (ObjectUtil.isEmpty(asNetworkDeviceSpecialExcel.getNo())) {
                throw new RuntimeException(asNetworkDeviceSpecialExcel.getName() + "的资产编号不能为空");
            }
            AsType asType = asTypeService.getAsType(asNetworkDeviceSpecialExcel.getTypeName());
            if (asType == null) {
                throw new RuntimeException("资产类别不存在");
            }
            AsDeviceCommon asDeviceCommon = new AsDeviceCommon();
            AsNetworkDeviceSpecial asNetworkDeviceSpecial = new AsNetworkDeviceSpecial();
            //
            BeanUtils.copyProperties(asNetworkDeviceSpecialExcel, asDeviceCommon);
            BeanUtils.copyProperties(asNetworkDeviceSpecialExcel, asNetworkDeviceSpecial);
            //
            asDeviceCommon.setTypeId(asType.getId());
            this.save(asDeviceCommon);
            asNetworkDeviceSpecial.setAsId(asDeviceCommon.getId());
            //
            asDeviceCommonList.add(asDeviceCommon);
            asNetworkDeviceSpecialList.add(asNetworkDeviceSpecial);
        }
        //
        this.updateBatchById(asDeviceCommonList);
        asNetworkDeviceSpecialService.saveBatch(asNetworkDeviceSpecialList);
        //
        if (haveCover.equals("是")) {
            int pageCount = pageList.size() - dbList.size();
            return "导入" + pageCount + "条资产;资产编号：" + dbList.stream().map(AsDeviceCommon::getNo).collect(Collectors.joining(",")) + ",已经被覆盖";
        } else {
            int pageCount = pageList.size();
            if (ObjectUtil.isEmpty(dbList)) {
                return "导入" + pageCount + "条资产";
            } else {
                return "导入" + pageCount + "条资产;资产编号：" + dbList.stream().map(AsDeviceCommon::getNo).collect(Collectors.joining(",")) + "已经存在，未导入";
            }
        }
    }

    private String addAsSecurityProductsSpecialExcel(List<AsSecurityProductsSpecialExcel> excelList, String haveCover) {
        //去掉db中存在的设备，剩下页面上需要导入的设备
        List<AsSecurityProductsSpecialExcel> pageList;
        //db中存在的设备
        List<AsDeviceCommon> dbList = new ArrayList<>();

        //处理日期类型
        ExcelDateUtil.converToDate(excelList, AsSecurityProductsSpecialExcel.class);
        /*
            haveCover=是，先删除，后全部插入设备
            haveCover=否，插入不在db中的设备
         */
        if (haveCover.equals("是")) {
            List<AsDeviceCommon> list = this.list(new QueryWrapper<AsDeviceCommon>().in("no", excelList.stream().map(AsSecurityProductsSpecialExcel::getNo).collect(Collectors.toList())));
            if (ObjectUtil.isNotEmpty(list)) {
                dbList = list;
                List<Integer> asIdList = list.stream().map(AsDeviceCommon::getId).collect(Collectors.toList());
                this.removeByIds(asIdList);
                asNetworkDeviceSpecialService.remove(new QueryWrapper<AsNetworkDeviceSpecial>().in("as_id", asIdList));
            }
            pageList = excelList;
        } else {
            List<AsDeviceCommon> list = this.list(new QueryWrapper<AsDeviceCommon>().in("no", excelList.stream().map(AsSecurityProductsSpecialExcel::getNo).collect(Collectors.toList())));
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
        for (AsSecurityProductsSpecialExcel asSecurityProductsSpecialExcel : pageList) {
            if (ObjectUtil.isEmpty(asSecurityProductsSpecialExcel.getNo())) {
                throw new RuntimeException(asSecurityProductsSpecialExcel.getName() + "的资产编号不能为空");
            }
            AsType asType = asTypeService.getAsType(asSecurityProductsSpecialExcel.getTypeName());
            if (asType == null) {
                throw new RuntimeException("资产类别不存在");
            }
            AsDeviceCommon asDeviceCommon = new AsDeviceCommon();
            AsSecurityProductsSpecial asSecurityProductsSpecial = new AsSecurityProductsSpecial();
            //
            BeanUtils.copyProperties(asSecurityProductsSpecialExcel, asDeviceCommon);
            BeanUtils.copyProperties(asSecurityProductsSpecialExcel, asSecurityProductsSpecial);
            //
            asDeviceCommon.setTypeId(asType.getId());
            this.save(asDeviceCommon);
            asSecurityProductsSpecial.setAsId(asDeviceCommon.getId());
            //
            asDeviceCommonList.add(asDeviceCommon);
            asSecurityProductsSpecialList.add(asSecurityProductsSpecial);
        }
        //
        this.updateBatchById(asDeviceCommonList);
        asSecurityProductsSpecialService.saveBatch(asSecurityProductsSpecialList);
        //
        if (haveCover.equals("是")) {
            int pageCount = pageList.size() - dbList.size();
            return "导入" + pageCount + "条资产;资产编号：" + dbList.stream().map(AsDeviceCommon::getNo).collect(Collectors.joining(",")) + ",已经被覆盖";
        } else {
            int pageCount = pageList.size();
            if (ObjectUtil.isEmpty(dbList)) {
                return "导入" + pageCount + "条资产";
            } else {
                return "导入" + pageCount + "条资产;资产编号：" + dbList.stream().map(AsDeviceCommon::getNo).collect(Collectors.joining(",")) + "已经存在，未导入";
            }
        }
    }

    private String addAsIoSpecialExcel(List<AsIoSpecialExcel> excelList, String haveCover) {
        //去掉db中存在的设备，剩下页面上需要导入的设备
        List<AsIoSpecialExcel> pageList;
        //db中存在的设备
        List<AsDeviceCommon> dbList = new ArrayList<>();

        //处理日期类型
        ExcelDateUtil.converToDate(excelList, AsIoSpecialExcel.class);
        /*
            haveCover=是，先删除，后全部插入设备
            haveCover=否，插入不在db中的设备
         */
        if (haveCover.equals("是")) {
            List<AsDeviceCommon> list = this.list(new QueryWrapper<AsDeviceCommon>().in("no", excelList.stream().map(AsIoSpecialExcel::getNo).collect(Collectors.toList())));
            if (ObjectUtil.isNotEmpty(list)) {
                dbList = list;
                List<Integer> asIdList = list.stream().map(AsDeviceCommon::getId).collect(Collectors.toList());
                this.removeByIds(asIdList);
                asIoSpecialService.remove(new QueryWrapper<AsIoSpecial>().in("as_id", asIdList));
            }
            pageList = excelList;
        } else {
            List<AsDeviceCommon> list = this.list(new QueryWrapper<AsDeviceCommon>().in("no", excelList.stream().map(AsIoSpecialExcel::getNo).collect(Collectors.toList())));
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
        for (AsIoSpecialExcel asIoSpecialExcel : pageList) {
            if (ObjectUtil.isEmpty(asIoSpecialExcel.getNo())) {
                throw new RuntimeException(asIoSpecialExcel.getName() + "的资产编号不能为空");
            }
            AsType asType = asTypeService.getAsType(asIoSpecialExcel.getTypeName());
            if (asType == null) {
                throw new RuntimeException("资产类别不存在");
            }
            AsDeviceCommon asDeviceCommon = new AsDeviceCommon();
            AsIoSpecial asIoSpecial = new AsIoSpecial();
            //
            BeanUtils.copyProperties(asIoSpecialExcel, asDeviceCommon);
            BeanUtils.copyProperties(asIoSpecialExcel, asIoSpecial);
            //
            asDeviceCommon.setTypeId(asType.getId());
            this.save(asDeviceCommon);
            asIoSpecial.setAsId(asDeviceCommon.getId());
            //
            asDeviceCommonList.add(asDeviceCommon);
            asIoSpecialList.add(asIoSpecial);
        }
        //
        this.updateBatchById(asDeviceCommonList);
        asIoSpecialService.saveBatch(asIoSpecialList);
        //
        if (haveCover.equals("是")) {
            int pageCount = pageList.size() - dbList.size();
            return "导入" + pageCount + "条资产;资产编号：" + dbList.stream().map(AsDeviceCommon::getNo).collect(Collectors.joining(",")) + ",已经被覆盖";
        } else {
            int pageCount = pageList.size();
            if (ObjectUtil.isEmpty(dbList)) {
                return "导入" + pageCount + "条资产";
            } else {
                return "导入" + pageCount + "条资产;资产编号：" + dbList.stream().map(AsDeviceCommon::getNo).collect(Collectors.joining(",")) + "已经存在，未导入";
            }
        }
    }

    //是否覆盖原资产信息，是=不判重，只提示导入多少个资产，否=判重，提示导入多少个资产;资产编号xx、yy已经存在，未导入。
    @Override
    public List<String> addExcel(List<AsComputerExcel> list0, List<AsNetworkDeviceSpecialExcel> list1, List<AsSecurityProductsSpecialExcel> list2, List<AsIoSpecialExcel> list3, String haveCover) {
        List<String> resultList = new ArrayList<>();
        //
        if (ObjectUtil.isNotEmpty(list0)) {
            resultList.add("计算机:" + addAsComputerExcel(list0, haveCover));
        }
        if (ObjectUtil.isNotEmpty(list1)) {
            resultList.add("网络设备:" + addAsNetworkDeviceSpecialExcel(list1, haveCover));
        }
        if (ObjectUtil.isNotEmpty(list2)) {
            resultList.add("安全产品:" + addAsSecurityProductsSpecialExcel(list2, haveCover));
        }
        if (ObjectUtil.isNotEmpty(list3)) {
            resultList.add("外部设备:" + addAsIoSpecialExcel(list3, haveCover));
        }
        //
        if (ObjectUtil.isEmpty(resultList)) {
            resultList.add("无设备数据被导入");
        }
        return resultList;
    }
}
