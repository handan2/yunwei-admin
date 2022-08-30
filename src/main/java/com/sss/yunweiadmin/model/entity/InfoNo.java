package com.sss.yunweiadmin.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import java.io.Serializable;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 
 * </p>
 *
 * @author 任勇林
 * @since 2022-08-24
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class InfoNo implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 联网类别
     */
    private String netType;

    private String value;

    /**
     * 状态：占用，空闲
     */
    private String status;

    /**
     * 保留字段：关联资产号，可能是多个，这样要用逗分开了，
     */
    private String assetNoStr;

    private String location;

    private String remark;
//    @JsonFormat(pattern = "yyyy-MM-dd")
//    private LocalDate useDate;
    private String d;


}
