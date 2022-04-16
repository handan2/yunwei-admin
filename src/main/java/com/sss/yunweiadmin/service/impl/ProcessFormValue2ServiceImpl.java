package com.sss.yunweiadmin.service.impl;

import com.sss.yunweiadmin.model.entity.ProcessFormValue2;
import com.sss.yunweiadmin.mapper.ProcessFormValue2Mapper;
import com.sss.yunweiadmin.service.ProcessFormValue2Service;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 自定义表单，走流程时，保存对应的自定义表的as_id，辅助process_form_value1 服务实现类
 * </p>
 *
 * @author 任勇林
 * @since 2021-10-02
 */
@Service
public class ProcessFormValue2ServiceImpl extends ServiceImpl<ProcessFormValue2Mapper, ProcessFormValue2> implements ProcessFormValue2Service {

}
