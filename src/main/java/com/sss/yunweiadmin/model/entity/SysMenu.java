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
 * @since 2021-08-24
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class SysMenu implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    private Integer pid;

    /**
     * 导航菜单名称，操作按钮名称，查询表单
     */
    private String name;

    private String path;

    /**
     * icon图标名称
     */
    private String icon;

    /**
     * add,edit,...
     */
    private String type;

    /**
     * 导航菜单，操作按钮，查询表单
     */
    private String flag;

    private Double sort;

    private String remark;

    @TableField(exist = false)
    private List<SysMenu> children;
}
