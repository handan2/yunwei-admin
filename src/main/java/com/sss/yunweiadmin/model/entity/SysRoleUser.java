package com.sss.yunweiadmin.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * <p>
 * 角色-用户
 * </p>
 *
 * @author 任勇林
 * @since 2021-04-09
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class SysRoleUser implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 对应sys_user表的id
     */
    private Integer userId;

    /**
     * 对应sys_role表的id
     */
    private Integer roleId;


}
