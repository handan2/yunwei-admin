package com.sss.yunweiadmin.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import java.time.LocalDate;
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
 * @since 2022-06-18
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("diskForHisForProcess")
public class DiskForHisForProcess implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    private Integer asId;
    private String no;
    private Integer hostAsId;
    private String hostAsNo;

    private Integer processInstanceDataId;

    private String sn;

    private String model;

    private String state;

    private String miji;

    private Integer price;//capacity

    private LocalDate madeDate;

    /**
     * 修改类型的标志字段
     */
    private String flag;


}
