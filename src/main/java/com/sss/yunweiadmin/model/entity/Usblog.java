package com.sss.yunweiadmin.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 
 * </p>
 *
 * @author 任勇林
 * @since 2025-04-02
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class Usblog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    private  String ebm;

    private String usbSn;

    private String usbMod;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate checkDate;

    private String validBj;


}
