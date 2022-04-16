package com.sss.yunweiadmin.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
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
 * @since 2021-04-14
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class AsComputerGranted implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    private Integer asId;

    private String usb;

    private String serial;

    private String parallel;

    private String hongwai;

    private String bluetooth;

    private String screenShot;

    private String dev1394;

    private String connection;

    private String ipBind;

    private String vm;

    private String docShare;

    private String pcmcia;

    private String devImage;

    private String devJuanying;

    private String portShemi;

    private String portCommon;


}
