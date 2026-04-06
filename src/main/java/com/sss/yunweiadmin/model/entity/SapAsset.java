package com.sss.yunweiadmin.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.time.LocalDateTime;
import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 
 * </p>
 *
 * @author 任勇林
 * @since 2025-03-25
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class SapAsset implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;


    private String equnr; // 设备唯一编号
    private String invnr; // 设备统一编码
    private String eqart; // 设备类型或型号
    private String eqktx; // 设备名称
    private String zzzjly; // 资源来源
    private String zjlydsc; // 资源来源描述
    private String tplnr; // 位置编码
    private String pltxt; //楼宇楼层
    private String stat; // 设备状态，中文
    private String zzytlb; // 用途
    private String herst; // 制造商
    private String typbz; // 型号
    private String serge; // 序列号
    private String inbdt; // 入库日期  20200101这种格式
    private String zzlwzl; // 联网类别；可能为空串
    private String beber; // 部门编码
    private String fing; // 部门中文
    private String zzrp; // 负责人
    private String zzdh; // 电话
    private String ansdt; // 购置日期，实际测的都是00000000
    private String zzszr; // 上账人
    private String zzszbm; // 上账部门代码
    private String zzsbmj;//密级 10/20/30(秘密)/40(机密)/50（非密）/60(普通商密)/70(未定密)
    private String msgrp;//房间号
    private String zzsfsm;//是否涉密
    private String erdat;//创建日期
    private String aedat; // 更新日期//

    private String zzxnzb;//性能描述
    private LocalDateTime createTime;

    private Integer asId;






}
