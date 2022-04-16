package com.sss.yunweiadmin.model.vo;

import lombok.Data;

import java.util.List;

@Data
public class PermissionGiveVO {
    private List<TreeSelectVO> permissionList;
    private List<Integer> checkPermissionIdList;
}

