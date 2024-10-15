package com.sss.yunweiadmin.service;

import com.sss.yunweiadmin.model.entity.InfoNo;
import com.baomidou.mybatisplus.extension.service.IService;
import com.sss.yunweiadmin.model.excel.*;

import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 任勇林
 * @since 2022-08-24
 */
public interface InfoNoService extends IService<InfoNo> {
    String addExcel(List<InfoNoExcel> list, String importMode);

}
