package com.sss.yunweiadmin.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * <p>
 * 表名，表字段的中英文对应表
 * </p>
 *
 * @author 任勇林
 * @since 2021-09-02
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class AsConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 英文表名称
     */
    private String enTableName;

    /**
     * 中文表名称
     */
    private String zhTableName;

    /**
     * 英文字段名称
     */
    private String enColumnName;

    /**
     * 中文字段名称
     */
    private String zhColumnName;

    /**
     * 值为字符串，数字，日期，日期区间，普通附件，拖拽附件，下拉单选，点击单选，下拉复选，点击复选
     */
    private String type;

    /**
     * 排序
     */
    private Double sort;

}
