package com.sss.yunweiadmin.model.entity;

import ch.qos.logback.core.net.SyslogOutputStream;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;

import com.sss.yunweiadmin.common.config.GlobalParam;
import lombok.Data;
import lombok.EqualsAndHashCode;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;


/**
 * <p>
 * 
 * </p>
 *
 * @author 任勇林
 * @since 2021-11-21
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class AsApplicationSpecial implements Serializable {

    private static final long serialVersionUID = 1L;

//    @Autowired //20241111在办公电脑上编译报错 && 运行时没事
//    private Environment environment;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    private Integer asId;

    private Integer sysadminId;

    private Integer safeadminId;

    private Integer auditadminId;

    private String usernameTmp;

    private String operateTypeTmp;

    private String useroperateTypeTmp;

    private String roleoperateTypeTmp;

    private String rolenameTmp;

    private String sysadminTmp;

    private String safeadminTmp;

    private String auditadminTmp;

    private String rolenameStr;


   // private Integer orgId = Integer.valueOf(environment.getProperty("orgId")) ;
    private Integer orgId = GlobalParam.orgId;



}
