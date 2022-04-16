package com.sss.yunweiadmin.service.impl;

import com.sss.yunweiadmin.model.entity.ProcessDefinitionTask;
import com.sss.yunweiadmin.mapper.ProcessDefinitionTaskMapper;
import com.sss.yunweiadmin.service.ProcessDefinitionTaskService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 流程定义条件表，用于保存流程图中的Task条件 服务实现类
 * </p>
 *
 * @author 任勇林
 * @since 2021-09-22
 */
@Service
public class ProcessDefinitionTaskServiceImpl extends ServiceImpl<ProcessDefinitionTaskMapper, ProcessDefinitionTask> implements ProcessDefinitionTaskService {

}
