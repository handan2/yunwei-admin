package com.sss.yunweiadmin.service;

import com.sss.yunweiadmin.model.entity.ProcessFormTemplate;
import com.baomidou.mybatisplus.extension.service.IService;
import com.sss.yunweiadmin.model.vo.FormTemplateVO;

import java.util.List;

/**
 * <p>
 * 自定义表单模板 服务类
 * </p>
 *
 * @author 任勇林
 * @since 2021-07-29
 */
public interface ProcessFormTemplateService extends IService<ProcessFormTemplate> {
    List<FormTemplateVO> getFormTemplateTree(List<ProcessFormTemplate> initList);
}
