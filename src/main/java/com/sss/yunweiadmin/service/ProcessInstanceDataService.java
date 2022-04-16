package com.sss.yunweiadmin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sss.yunweiadmin.model.entity.ProcessInstanceData;
import com.sss.yunweiadmin.model.vo.CheckProcessVO;
import com.sss.yunweiadmin.model.vo.ModifyProcessFormVO;
import com.sss.yunweiadmin.model.vo.StartProcessVO;

/**
 * <p>
 * 流程实例数据 服务类
 * </p>
 *
 * @author 任勇林
 * @since 2021-04-17
 */
public interface ProcessInstanceDataService extends IService<ProcessInstanceData> {
    boolean start(StartProcessVO startProcessVO);

    boolean handle(CheckProcessVO checkProcessVO);

    boolean modifyProcessForm(ModifyProcessFormVO modifyProcessFormVO);

    boolean delete(ProcessInstanceData processInstanceData);
}
