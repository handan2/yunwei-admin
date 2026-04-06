package com.sss.yunweiadmin.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.sss.yunweiadmin.common.config.GlobalParam;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * 用户表
 * </p>
 *
 * @author 任勇林
 * @since 2021-04-09
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class SysUserForInterFace implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 登录名称
     */
    private String loginName;

    /**
     * 显示名称
     */
    private String displayName;


    /**
     * 密级程度,值为机密，秘密，普通商密，核心商密，非密
     */
    private String secretDegree;

    /**
     * 手机号码
     */
    private String mobile;



    private String idNumber;

    /**
     * 使用状态，正常和禁用
     */
    private String status;

    private String deptName;

    private Integer orgId = GlobalParam.orgId;
}
