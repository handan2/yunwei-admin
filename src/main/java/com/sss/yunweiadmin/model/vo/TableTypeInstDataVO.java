package com.sss.yunweiadmin.model.vo;

import com.sss.yunweiadmin.model.entity.*;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class TableTypeInstDataVO {
    private Map<String, String> map;//基本的自定义表实例数据
    private List<DiskForHisForProcess> diskList;//硬盘数据

}
