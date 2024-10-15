package com.sss.yunweiadmin.common.result;

import lombok.AllArgsConstructor;
import lombok.Data;

//自定义的分页数据
@Data
@AllArgsConstructor
public class PaginationData {
    //当前页
    private Integer currentPage;
    //每页显示几条
    private Integer pageSize;
    //总记录数
    private Integer total;
    //总分页数
    private Integer totalPage;
    //具体数据
    Object dataList;
}
