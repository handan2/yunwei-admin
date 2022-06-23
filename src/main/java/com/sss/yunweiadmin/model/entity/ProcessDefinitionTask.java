package com.sss.yunweiadmin.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 流程定义条件表，用于保存流程图中的Task条件
 * </p>
 *
 * @author 任勇林
 * @since 2021-09-22
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class ProcessDefinitionTask implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * startTask,approvalTask,handleTask,archiveTask
     */
    private String taskType;

    /**
     * 节点id
     */
    private String taskDefKey;

    /**
     * 节点名称
     */
    private String taskName;

    /**
     * 选择处理类型,值为角色和用户
     */
    private String operatorType;

    /**
     * 角色id集合，或者 用户id集合
     */
    private String operatorTypeValue;

    /**
     * 角色名称集合，或者 用户名称集合
     */
    private String operatorTypeLabel;

    /**
     * 是否勾选提交人部门
     */
    private String haveStarterDept;

    /**
     * 是否允许填写审批意见
     */
    private String haveComment;

    /**
     * 是否允许指定下一步处理人
     */
    private String haveNextUser;

    /**
     * 是否允许修改表单
     */
    private String haveEditForm;

    /**
     * 是否显示操作记录的checkbox，sysDic中的操作类型
     */
    private String haveOperate;

    /**
     * 关联process_definition.id
     */
    private Integer processDefinitionId;
    /**
     * 关联process_definition. hide_group_ids
     */
    private String  hideGroupIds;
    /**
     * 关联process_definition.hide_group_label
     */
    private String hideGroupLabel;


    private String haveSelectAsset;

}
