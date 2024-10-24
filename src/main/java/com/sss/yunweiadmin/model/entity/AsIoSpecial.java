package com.sss.yunweiadmin.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.sss.yunweiadmin.common.config.GlobalParam;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * <p>
 * 
 * </p>
 *
 * @author 任勇林
 * @since 2021-10-08
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class AsIoSpecial implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    private Integer asId;

    private String accessHostNo;

    private String accessPort;

    private Integer orgId = GlobalParam.orgId;
}
