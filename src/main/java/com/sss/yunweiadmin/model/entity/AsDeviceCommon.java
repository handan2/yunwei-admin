package com.sss.yunweiadmin.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * <p>
 * 
 * </p>
 *
 * @author 任勇林
 * @since 2021-10-08
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class AsDeviceCommon implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    private Integer typeId;

    private Integer hostAsId;

    private String no;

    private String name;

    private String baomiNo;

    private String fundSrc;

    private String netType;

    private String portNo;

    private String hostName;

    private String location;

    private String shared;

    private String nameShared;

    private String state;

    private String usagee;

    private String administrator;

    private String adminTel;

    private String userName;

    private String userDept;

    private String userMiji;

    private String userTel;

    private String manufacturer;

    private String model;

    private String sn;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate madeDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate buyDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate useDate;

    private Integer price;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate discardDate;

    private String ip;

    private String mac;

    private String miji;

    private String remark;
    private String temp;


}
