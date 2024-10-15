package com.sss.yunweiadmin.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 流程中的自定义表单时，下拉类型
 * </p>
 *
 * @author 任勇林
 * @since 2021-03-19
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class ProcessFormType implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 名称
     */
    private String name;

    /**
     * 排序
     */
    private Double sort;

    /**
     * 状态，值为正常和禁用
     */
    private String status;


}
