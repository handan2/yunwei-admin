package com.sss.yunweiadmin.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * <p>
 * 自定义表单模板
 * </p>
 *
 * @author 任勇林
 * @since 2021-07-29
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class ProcessFormTemplate implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * label显示名称
     */
    private String label;

    /**
     * 字段名字及类型，"16.计算机信息表.as_device_common.miji,字符串"；字段组及表类型时为空
     */
    private String name;

    /**
     * 控件的类型，如“下拉可编辑”；当是表类型时，值格式为“16.计算机信息表”
     */
    private String type;

    /**
     * 真正的字段类型：基本类型(字符串，数字)/自定义表类型/字段变更类型/字段组类型
     */
    private String flag;

    /**
     * 对应sys_dic中的自定义表单布局，用于字段组里的布局
     */
    private Integer groupLayout;

    /**
     * 用于子分组，关联父分组的label
     */
    private String groupParentLabel;

    /**
     * 发起流程时，是否允许进行字段组选择
     */
    private String haveGroupSelect;

    /**
     * 是否必填，值为是和否
     */
    private String required;

    /**
     * 提示
     */
    private String tooltip;

    /**
     * 下拉或者点击的单选或者复选的值，使用英文逗号隔开
     */
    private String value;

    /**
     * 默认值
     */
    private String defaultValue;

    /**
     * 关联process_definition.id
     */
    private Integer processDefinitionId;

    /**
     * visible
     */
    private String visible;

    private String editable;

    private String datasource;

}
