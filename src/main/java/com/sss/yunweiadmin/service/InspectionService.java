package com.sss.yunweiadmin.service;

import com.sss.yunweiadmin.model.entity.Inspection;
import com.baomidou.mybatisplus.extension.service.IService;
import com.sss.yunweiadmin.model.excel.InfoNoExcel;
import com.sss.yunweiadmin.model.excel.InspectionExcel;

import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 任勇林
 * @since 2024-06-23
 */
public interface InspectionService extends IService<Inspection> {
    String addExcel(List<InspectionExcel> list, String importMode);
}
