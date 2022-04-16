package com.sss.yunweiadmin.service.impl;

import com.sss.yunweiadmin.model.entity.ProcessInstanceChange;
import com.sss.yunweiadmin.mapper.ProcessInstanceChangeMapper;
import com.sss.yunweiadmin.service.ProcessInstanceChangeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 流程实例结束时，变更字段表 服务实现类
 * </p>
 *
 * @author 任勇林
 * @since 2021-10-23
 */
@Service
public class ProcessInstanceChangeServiceImpl extends ServiceImpl<ProcessInstanceChangeMapper, ProcessInstanceChange> implements ProcessInstanceChangeService {

}
