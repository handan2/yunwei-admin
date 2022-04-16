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
 * @since 2021-09-02
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class AsComputerSpecial implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    private Integer asId;

    private String netInterface;

    private Integer ram;

    private String cdrom;

    private String videoCard;

    private String macBackup;

    private String soundCard;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate osDate;

    private String osType;

    private Integer cpuTotal;

    private Integer diskTotal;

    private Integer diskSize;

    private String diskMode1;

    private String diskMode2;

    private String diskMode3;

    private String diskMode4;

    private String diskSn1;

    private String diskSn2;

    private String diskSn3;

    private String diskSn4;


}
