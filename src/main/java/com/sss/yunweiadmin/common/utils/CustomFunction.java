package com.sss.yunweiadmin.common.utils;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.StrUtil;

import java.util.List;

public class CustomFunction {
    public static boolean isSubstringInListBigData(List<String> list1, String a) {
        if (CollUtil.isEmpty(list1) || StrUtil.isEmpty(a)) {
            return false;
        }
        String joinStr = StrUtil.join("###", list1); // 自定义分隔符，避免与a重合
        return StrUtil.contains(joinStr, a);
    }
}
