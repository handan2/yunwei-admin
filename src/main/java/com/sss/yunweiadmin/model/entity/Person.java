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
 * @since 2025-03-26
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class Person implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    private String a0000;

    private String uniqueId;

    private String nbase;

    private String nbase_0;

    private String a0100;

    private String a0101;

    private String b0110_0;

    private String b0110;

    private String e0122_0;

    private String a011i;

    private String e01a1_0;

    private String e01a1;

    private String b0110Code;

    private String e0122Code;

    private String e01a1Code;

    private String sysFlag;

    private LocalDateTime sdate;

    private String flag;

    private String a0183;

    private String a0184;

    private String a010n;

    private String a011m;

    private String a0177;

    private String status;

    private String yunwei;

    private String yunweiSdate;

    private LocalDateTime createTime;


}
