package com.sss.yunweiadmin.model.vo;

import com.sss.yunweiadmin.model.entity.AsDeviceCommon;
import com.sss.yunweiadmin.model.entity.SysPermission;
import com.sss.yunweiadmin.model.entity.SysUser;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class RepeaterForAssetListVO {

    //导航菜单
    private List<AsDeviceCommon> dataSource;

}

