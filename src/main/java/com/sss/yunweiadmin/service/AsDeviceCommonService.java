package com.sss.yunweiadmin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sss.yunweiadmin.model.entity.AsDeviceCommon;
import com.sss.yunweiadmin.model.entity.AsType;
import com.sss.yunweiadmin.model.excel.*;
import com.sss.yunweiadmin.model.vo.AssetVO;

import java.time.LocalDate;
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

    boolean addStatistics();
    boolean add(AssetVO assetVO);

    boolean edit(AssetVO assetVO);
    boolean delete(Integer[] idArr);

    List<String> addExcel(List<AsComputerExcel> list0, List<AsNetworkDeviceExcel> list1, List<AsSecurityProductExcel> list2, List<AsIoExcel> list3, List<StorageExcel> list4, String importMode);
    String makeBaomiNo(AsType asType, String miji, LocalDate useDate) ;
}
