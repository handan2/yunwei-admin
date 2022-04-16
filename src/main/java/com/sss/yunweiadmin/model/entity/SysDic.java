package com.sss.yunweiadmin.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 数据字典，用于固定的下拉选项
 * </p>
 *
 * @author 任勇林
 * @since 2021-03-17
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class SysDic implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 标识符，比如性别
     */
    private String flag;

    /**
     * 名称
     */
    private String name;

    /**
     * 状态，值为正常和禁用
     */
    private String status;

    /**
     * 排序
     */
    private Double sort;


}
