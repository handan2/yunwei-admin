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
 * @since 2022-04-14
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class ProcessFormCustomInst implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    private String tableName;

    private String columnName;

    private String columnType;

    private  Integer assetTypeId;

    private  Integer asId;

    private String columnValue;

    private Integer processInstanceDataId;


}
