package com.sss.yunweiadmin.model.vo;

import lombok.Data;

import java.util.List;

@Data
public class MenuGiveVO {
    private List<TreeSelectVO> menuList;
    private List<Integer> checkMenuIdList;
}

