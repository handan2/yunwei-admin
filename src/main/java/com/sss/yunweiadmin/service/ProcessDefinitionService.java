package com.sss.yunweiadmin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sss.yunweiadmin.model.entity.ProcessDefinition;
import com.sss.yunweiadmin.model.vo.ProcessDefinitionVO;

/**
 * <p>
 * 流程定义时的基本表，用于保存 流程名称，自定义表单布局 服务类
 * </p>
 *
 * @author 任勇林
 * @since 2021-09-03
 */
public interface ProcessDefinitionService extends IService<ProcessDefinition> {
    boolean add(ProcessDefinitionVO processDefinitionVO);

    boolean edit(ProcessDefinitionVO processDefinitionVO);

    boolean delete(Integer processDefinitionId);

    boolean copy(Integer processDefinitionId);
}
