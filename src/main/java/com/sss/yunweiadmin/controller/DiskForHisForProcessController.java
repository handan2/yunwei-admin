package com.sss.yunweiadmin.controller;


import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sss.yunweiadmin.common.config.GlobalParam;
import com.sss.yunweiadmin.common.result.ResponseResultWrapper;
import com.sss.yunweiadmin.model.entity.DiskForHisForProcess;
import com.sss.yunweiadmin.service.DiskForHisForProcessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author 任勇林
 * @since 2022-06-18
 */
@RestController
@RequestMapping("/diskForHisForProcess")
@ResponseResultWrapper
public class DiskForHisForProcessController {
    @Autowired
    DiskForHisForProcessService diskForHisForProcessService;

    @GetMapping("getDiskForHisForProcess")
    public List<DiskForHisForProcess> getDiskForHisForProcess( Integer processInstanceDataId, String orgIdS){
        int orgId = GlobalParam.orgId;
        if(ObjectUtil.isNotEmpty(orgIdS)) {//20241106 穿透流程的前端参数标记
            orgId = Integer.valueOf(orgIdS);

        }
        List<DiskForHisForProcess> list = diskForHisForProcessService.list(new  QueryWrapper<DiskForHisForProcess>().eq("org_id", orgId).eq("process_instance_data_id",processInstanceDataId));
        return list;


    }
}
