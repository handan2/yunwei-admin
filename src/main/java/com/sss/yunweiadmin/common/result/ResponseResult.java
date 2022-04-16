package com.sss.yunweiadmin.common.result;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResponseResult {
    private Integer code;
    private String msg;
    private Object data;

    public static ResponseResult success() {
        return new ResponseResult(200, "操作成功", "");
    }

    public static ResponseResult success(Object data) {
        ResponseResult responseResult = success();
        if (data instanceof IPage) {
            IPage page = (IPage) data;
            int pageSize = (int) page.getSize();
            int total = (int) page.getTotal();
            //计算总页数
            int totalPage = total / pageSize + ((total % pageSize == 0) ? 0 : 1);
            PaginationData paginationVO = new PaginationData((int) page.getCurrent(), pageSize, total, totalPage, page.getRecords());
            responseResult.setData(paginationVO);
        } else {
            responseResult.setData(data);
        }

        return responseResult;
    }

    public static ResponseResult fail() {
        return new ResponseResult(0, "操作失败", "");
    }

    public static ResponseResult fail(String msg) {
        return new ResponseResult(0, msg, msg);
    }
}
