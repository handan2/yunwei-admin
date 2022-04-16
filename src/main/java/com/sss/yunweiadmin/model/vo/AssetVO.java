package com.sss.yunweiadmin.model.vo;

import com.sss.yunweiadmin.model.entity.*;
import lombok.Data;

@Data
public class AssetVO {
    //该字段是标志位
    private String formItemNameFlag = "formItemNameFlag";
    private AsDeviceCommon asDeviceCommon;
    private AsComputerSpecial asComputerSpecial;
    private AsComputerGranted asComputerGranted;
    private AsNetworkDeviceSpecial asNetworkDeviceSpecial;
    private AsSecurityProductsSpecial asSecurityProductsSpecial;
    private AsIoSpecial asIoSpecial;
    private AsApplicationSpecial asApplicationSpecial;
}
