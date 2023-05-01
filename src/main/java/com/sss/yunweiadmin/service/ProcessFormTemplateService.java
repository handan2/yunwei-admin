package com.sss.yunweiadmin.service;

import com.sss.yunweiadmin.model.entity.ProcessFormTemplate;
import com.baomidou.mybatisplus.extension.service.IService;
import com.sss.yunweiadmin.model.vo.FormTemplateVO;
import com.sss.yunweiadmin.model.vo.TableTypeVO;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 自定义表单模板 服务类
 * </p>
 *
 * @author 任勇林
 * @since 2021-07-29
 */
public interface ProcessFormTemplateService extends IService<ProcessFormTemplate> {
    List<FormTemplateVO> getFormTemplateTree(List<ProcessFormTemplate> initList,Integer processDefinitionId,String actProcessInstanceId);
    Map<Integer, List<TableTypeVO>> getTableTypeVO(Integer processDefinitionId) ;
    Map<String, String> getLabelIdMapForItemByAjax(Integer processDefinitionId, String hideGroupIds);
}
