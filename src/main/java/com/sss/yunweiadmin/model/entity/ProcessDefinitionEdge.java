package com.sss.yunweiadmin.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * <p>
 * 流程定义条件表，用于保存流程图中的连线条件
 * </p>
 *
 * @author 任勇林
 * @since 2021-09-22
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class ProcessDefinitionEdge implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 连线id
     */
    private String edgeId;

    /**
     * 源节点id
     */
    private String sourceId;

    /**
     * 目标节点id
     */
    private String targetId;

    /**
     * 流程图上的连线名称
     */
    private String edgeName;

    /**
     * 流程启动中的按钮名称
     */
    private String buttonName;

    /**
     * 连线方向
     */
    private String edgeDirection;

    /**
     * 变量名称，中英文逗号分隔，在排他网关时使用
     */
    private String varName;

    /**
     * 判断条件
     */
    private String conditionn;

    /**
     * 关联process_definition.id
     */
    private Integer processDefinitionId;


}
