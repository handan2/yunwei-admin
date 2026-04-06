package com.sss.yunweiadmin.model.vo;

import com.sss.yunweiadmin.model.entity.SysPermission;
import com.sss.yunweiadmin.model.entity.SysUser;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class UserForAppVO {
    /**
     * 中文名
     */
    private String displayName;

    /**
     * 登陆名
     */
    private String loginName;

    /**
     * 性别
     */
    private String gender;

    /**
     * 应用系统代号
     */
    private String appNo;

    /**
     * 身份证ID
     */
    private String idNumber;

    /**
     * 人员编码
     */
    private String userCode;

    /**
     * 机构编码
     */
    private String orgCode;

    /**
     * 密级
     */
    private String secretDegree;

    /**
     * 聘用身份
     */
    private String identity;

    /**
     * 职务；多个用“|”分隔
     */
    private String position;

    /**
     * 状态：在职、离职、退休
     */
    private String status;

    /**
     * 角色名称：多个用“|”分隔
     */
    private String roleName;

    /**
     * 更新时间
     */
    private String updateDateTime;

    /**
     * 更新类型：新增、删除、修改
     */
    private String updateDateType;
}
