package com.sss.yunweiadmin.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.List;

/**
 * <p>
 * 菜单表，别名权限表、资源表
 * </p>
 *
 * @author 任勇林
 * @since 2021-10-31
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class SysPermission implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    private Integer pid;

    /**
     * 菜单，叶子菜单，权限
     */
    private String type;

    private String name;

    private String path;

    private String permissionType;

    private String position;
    /**
     * icon图标名称
     */
    private String icon;

    private Double sort;

    private String remark;

    @TableField(exist = false)
    List<SysPermission> children;

}
