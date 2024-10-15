package com.sss.yunweiadmin.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.TableField;
import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 
 * </p>
 *
 * @author 任勇林
 * @since 2022-09-11
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class Statistics implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    private String period;;

    private Integer asTypeId;

    private Integer amount;

    private String rate;

    @TableField("reInstall_amount")
    private Integer reinstallAmount;

    private LocalDateTime createTime;

    private String miji;


}
