package com.sss.yunweiadmin.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import java.time.LocalDate;
import com.baomidou.mybatisplus.annotation.TableId;
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
 * @since 2022-06-09
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class Disk implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    private Integer hostAsId;

    private String sn;

    private String mode;

    private String status;

    private String miji;

    private Integer capacity;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate createTime;
    private String  flag;


}
