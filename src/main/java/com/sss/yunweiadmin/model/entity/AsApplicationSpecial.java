package com.sss.yunweiadmin.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 
 * </p>
 *
 * @author 任勇林
 * @since 2021-11-21
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class AsApplicationSpecial implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    private Integer asId;

    private Integer sysadminId;

    private Integer safeadminId;

    private Integer auditadminId;

    private String usernameTmp;

    private String operateTypeTmp;

    private String useroperateTypeTmp;

    private String roleoperateTypeTmp;

    private String rolenameTmp;

    private String sysadminTmp;

    private String safeadminTmp;

    private String auditadminTmp;


}
