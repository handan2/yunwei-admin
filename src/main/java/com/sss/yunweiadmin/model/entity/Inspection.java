package com.sss.yunweiadmin.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import java.time.LocalDate;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 
 * </p>
 *
 * @author 任勇林
 * @since 2024-06-23
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class Inspection implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    private String inspector;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate inspectDate;

    private String no;

    private String type;

    private String mode;

    private String netType;

    private String miji;

    private String userDept;

    private String userName;

    private String label;

    private String system;

    private String safeSoft;

    private String illegalAccess;

    private String files;

    private String others;

    private String creator;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createDatetime;

    private Integer orgId;
}
