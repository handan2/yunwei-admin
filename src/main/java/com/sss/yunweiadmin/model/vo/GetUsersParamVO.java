package com.sss.yunweiadmin.model.vo;

import lombok.Data;

@Data
public class GetUsersParamVO {
    private String token;
    private String updateDateTime;
    private String appNo;
    private Integer currentPage;
    private Integer pageSize;
}
