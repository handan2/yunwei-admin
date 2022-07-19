package com.sss.yunweiadmin.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 流程定义时的基本表，用于保存 流程名称，自定义表单布局
 * </p>
 *
 * @author 任勇林
 * @since 2021-09-03
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class ProcessDefinition implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 修改流程定义时，最开始的id
     */
    private Integer baseId;

    /**
     * 上一次修改的id
     */
    private Integer beforeId;

    /**
     * 页面流程定义列表，是否显示出来
     */
    private String haveDisplay;

    /**
     * 流程定义名称
     */
    private String processName;

    /**
     * 流程分类，来自sys_dic.流程分类
     */
    private String processType;

    /**
     * 流程分类2，来自sys_dic
     */
    private String processType2;
    /**
     * asTypeId
     */
    private Integer asTypeId;



    /**
     * 流程实例名称分类
     */
    private String processNameType;

    /**
     * 是否允许打分
     */
    private String haveRate;

    /**
     * 对应sys_dic中的自定义表单布局
     */
    private Integer formLayout;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 备注
     */
    private String remark;

    private String bpmnXml;

    private String deployId;

    private String roleId;

    private Double sort;
    private String description;
    private String status;
    private String startLimitedByCheck;
    private String integrationMode;
}
