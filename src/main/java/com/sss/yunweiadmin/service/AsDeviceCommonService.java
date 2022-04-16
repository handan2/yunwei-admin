package com.sss.yunweiadmin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sss.yunweiadmin.model.entity.AsDeviceCommon;
import com.sss.yunweiadmin.model.excel.AsComputerExcel;
import com.sss.yunweiadmin.model.excel.AsIoSpecialExcel;
import com.sss.yunweiadmin.model.excel.AsNetworkDeviceSpecialExcel;
import com.sss.yunweiadmin.model.excel.AsSecurityProductsSpecialExcel;
import com.sss.yunweiadmin.model.vo.AssetVO;

import java.util.List;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author 任勇林
 * @since 2021-10-08
 */
public interface AsDeviceCommonService extends IService<AsDeviceCommon> {
    boolean add(AssetVO assetVO);

    boolean edit(AssetVO assetVO);
    boolean delete(Integer[] idArr);

    List<String> addExcel(List<AsComputerExcel> list0, List<AsNetworkDeviceSpecialExcel> list1, List<AsSecurityProductsSpecialExcel> list2, List<AsIoSpecialExcel> list3, String haveCover);
}
