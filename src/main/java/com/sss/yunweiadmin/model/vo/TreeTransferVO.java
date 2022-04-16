package com.sss.yunweiadmin.model.vo;

import lombok.Data;

import java.util.List;
@Data
public class TreeTransferVO {
    private String title;
    private Object key;
    private boolean checkable = true;
    //临时信息，比如 部门名称
    private String tmp;
    private List<TreeTransferVO> children;
}
