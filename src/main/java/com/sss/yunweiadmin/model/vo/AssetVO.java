package com.sss.yunweiadmin.model.vo;

import com.sss.yunweiadmin.model.entity.*;
import lombok.Data;

import java.util.List;

@Data
public class AssetVO {
    //该字段是标志位
    private String formItemNameFlag = "formItemNameFlag";//前端form相关的values有与这个标志位等值的“name/值”成员时，前端的formRule/formItemNameHandle方法才会进行序列化/反序列化
    private AsDeviceCommon asDeviceCommon;
    private AsComputerSpecial asComputerSpecial;
    private AsComputerGranted asComputerGranted;
    private AsNetworkDeviceSpecial asNetworkDeviceSpecial;
    private AsSecurityProductsSpecial asSecurityProductsSpecial;
    private AsIoSpecial asIoSpecial;
    private AsApplicationSpecial asApplicationSpecial;
    //20220612
    private List<AsDeviceCommon> diskList;
    private List<AsDeviceCommon> diskListForHis;//用于前端向后端传：增加了“删除/新增”的flag标志位
}
