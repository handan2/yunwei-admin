package com.sss.yunweiadmin.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;

import com.sss.yunweiadmin.common.config.GlobalParam;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 
 * </p>
 *
 * @author 任勇林
 * @since 2025-08-07
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class PermissionList implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    private String type;

    private String interfaceType;

    private String model;

    private String desc;

    private Integer orgId = GlobalParam.orgId;


}
