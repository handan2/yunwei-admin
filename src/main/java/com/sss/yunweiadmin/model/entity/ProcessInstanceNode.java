package com.sss.yunweiadmin.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 流程实例节点
 * </p>
 *
 * @author 任勇林
 * @since 2021-07-27
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class ProcessInstanceNode implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    private Integer processInstanceDataId;

    /**
     * 节点id
     */
    private String taskId;

    /**
     * 节点名称
     */
    private String taskName;


    /**
     * 处理人的登录账号
     */
    private String loginName;

    /**
     * 处理人的真实姓名
     */
    private String displayName;

    private String deptName;

    /**
     * 处理意见
     */
    private String comment;

    /**
     * 对应have_operate的复选框的选中值
     */
    private String operate;

    /**
     * 选择处理类型,值为角色和用户
     */
    private String type;

    /**
     * 角色id集合，或者 用户id集合
     */
    private String typeValue;

    /**
     * 角色名称集合，或者 用户名称集合
     */
    private String typeLabel;

    /**
     * 按钮名称
     */
    private String buttonName;

    /**
     * 提交人部门
     */
    private String haveStarterDept;


    /**
     * 到达时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startDatetime;

    /**
     * 完成时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endDatetime;


}
