package com.sss.yunweiadmin.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 流程实例数据
 * </p>
 *
 * @author 任勇林
 * @since 2021-07-29
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class ProcessInstanceData implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 关联process_definition.id
     */
    private Integer processDefinitionId;

    /**
     * 流程名称
     */
    private String processName;

    /**
     * 业务id，自定义表单实例id，process_form_value1.id
     */
    private Integer businessId;

    /**
     * activiti的流程实例id
     */
    private String actProcessInstanceId;

    /**
     * 流程状态
     */
    private String processStatus;

    /**
     * 当前步骤,用于页面显示
     */
    private String displayCurrentStep;

    /**
     * 当前步骤,用于查询
     */
    private String loginCurrentStep;

    /**
     * 提交人部门
     */
    private Integer deptId;
    /**
     * 提交人部门
     */
    private String deptName;

    /**
     * 提交人的真实姓名
     */
    private String displayName;

    /**
     * 提交人的登录账号
     */
    private String loginName;

    /**
     * 提交时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startDatetime;

    /**
     * 结束时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endDatetime;

    /**
     * 流程评分
     */
    @TableField(exist = false)
    private int score;

    private int preProcessInstanceId;
    private int postProcessInstanceId;
    /**
     * 工单编号
     */
    private String orderNum;
    /**
     * 锁定时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lockDatetime;

    private String lockUsername;

}
