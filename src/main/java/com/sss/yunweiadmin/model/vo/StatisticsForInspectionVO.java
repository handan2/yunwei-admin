package com.sss.yunweiadmin.model.vo;

import com.sss.yunweiadmin.model.entity.SysPermission;
import com.sss.yunweiadmin.model.entity.SysUser;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class StatisticsForInspectionVO {
    private int cmpTotals;
    private int cmpTotalsHaveInspeted;
    private int cmpTotalsHaveNotInspeted;
    private int cmpTotalsToInspetThisMonth;
    private int cmpTotalsHaveNotImproved;
    private String cmpNoStrHaveNotImproved;
    private RepeaterForAssetListVO repeaterForAssetListVO;

}

