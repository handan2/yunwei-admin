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
 * 流程实例结束时，变更字段表
 * </p>
 *
 * @author 任勇林
 * @since 2021-10-23
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class ProcessInstanceChange implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    private Integer asId;

    private Integer processInstanceDataId;
    /**
     * activiti的流程实例id
     */
    private String actProcessInstanceId;

    /**
     * 属性名称
     */
    private String name;

    /**
     * 旧值
     */
    private String oldValue;

    /**
     * 新值
     */
    private String newValue;

    /**
     * 修改人部门名称
     */
    private String deptName;

    /**
     * 修改人的显示名称
     */
    private String displayName;

    /**
     * 修改人的登录账号
     */
    private String loginName;

    /**
     * 修改时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime modifyDatetime;

    private String flag;

    private String zhTableName;
}
