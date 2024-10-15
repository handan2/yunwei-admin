package com.sss.yunweiadmin.model.vo;

import lombok.Data;

import java.util.List;

@Data
public class ProcessRoleGiveVO {
    private List<ValueLabelVO> roleList;
    private List<Integer> checkRoleIdList;
}
