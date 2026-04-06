package com.sss.yunweiadmin.model.vo;

import lombok.Data;

import java.util.List;

@Data
public class GetUsersVO {
    /**
     * 当前页码
     */
    private Integer currentPage;

    /**
     * 每页条数
     */
    private Integer pageSize;

    /**
     * 总记录数
     */
    private Integer total; // 建议使用 Long，防止记录数过多导致 Integer 溢出

    /**
     * 总页数
     */
    private Integer totalPage;

    /**
     * 数据列表
     */
    private List<UserForAppVO> dataList;
}
