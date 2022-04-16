package com.sss.yunweiadmin.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 自定义表单，走流程时，保存表单中数据
 * </p>
 *
 * @author 任勇林
 * @since 2021-10-02
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class ProcessFormValue1 implements Serializable {

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
     * 包含process_form_template中flag为基本类型、字段变更类型和组类型的值
     */
    private String value;

    private String userType;

    private String userIdStr;

    private String userName;

    private String selectGroupId;


}
