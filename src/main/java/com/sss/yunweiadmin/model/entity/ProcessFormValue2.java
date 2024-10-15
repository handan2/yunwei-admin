package com.sss.yunweiadmin.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * <p>
 * 自定义表单，走流程时，保存对应的自定义表的as_id，辅助process_form_value1
 * </p>
 *
 * @author 任勇林
 * @since 2021-10-02
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class ProcessFormValue2 implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 关联process_definition.id
     */
    private Integer processDefinitionId;

    /**
     * activiti的流程实例id
     */
    private String actProcessInstanceId;

    /**
     * 关联proce_form_value1.id
     */
    private Integer formValue1Id;

    /**
     * 自定义表名称
     */
    private Integer customTableId;

    private Integer asId;


}
