package com.sss.yunweiadmin.common.utils;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.sss.yunweiadmin.model.entity.AsConfig;
import com.sss.yunweiadmin.service.AsConfigService;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


public class ProcessFormCustomTypeUtil {
    //取出 props 中的表名+属性id;map格式：<"as_device_common",List<AsConfig>>
    public static Map<String, List<AsConfig>> parseProps(String props) {
        Map<String, List<AsConfig>> map = Maps.newLinkedHashMap();
        //字符串转map
        JSONObject jsonObject = JSONObject.parseObject(props);
        //遍历 取出表名
        List<String> tableNameList = Lists.newArrayList();
        for (Map.Entry<String, Object> entry : ((Map<String, Object>) jsonObject).entrySet()) {
            if (entry.getValue() instanceof JSONArray) {
                tableNameList.add(entry.getKey());
            }
        }
        AsConfigService asConfigService = (AsConfigService) SpringUtil.getBean("asConfigServiceImpl");
        //给tableNameList排个序
        tableNameList=asConfigService.list(new QueryWrapper<AsConfig>().in("en_table_name", tableNameList).select("distinct en_table_name").orderByAsc("sort"))
                .stream().map(AsConfig::getEnTableName).collect(Collectors.toList());

        for (String tableName : tableNameList) {
            //根据tableName，获取id
            List<Integer> idList = jsonObject.getJSONArray(tableName).stream().map(item -> (Integer) item).collect(Collectors.toList());
         if(CollUtil.isNotEmpty(idList)) {//20220718 idList size不能为0，否则会报错
             QueryWrapper queryWrapper = new QueryWrapper<AsConfig>()
                     .eq("en_table_name", tableName)
                     .in("id", idList)
                     .orderByAsc("sort");
             List<AsConfig> AsConfigList = asConfigService.list(queryWrapper);
             map.put(tableName, AsConfigList);
         }
        }
        return map;
    }
    //取出 props 中的表名+所有属性
    public static Map<String, List<AsConfig>> parseProps2(String props) {
        Map<String, List<AsConfig>> map = Maps.newLinkedHashMap();
        //字符串转map
        JSONObject jsonObject = JSONObject.parseObject(props);
        //遍历 取出表名
        List<String> tableNameList = Lists.newArrayList();
        for (Map.Entry<String, Object> entry : ((Map<String, Object>) jsonObject).entrySet()) {
//            if (entry.getValue() instanceof JSONArray) {//20220718逻辑有问题：这里限制了只能选自定义表实例中已选字段对应的基本表：实际应该是所有‘flag=是’的基本表（不管有没有选过当自定义表字段）
//                tableNameList.add(entry.getKey());
//            }
              if (entry.getValue().equals("是")) {//20220718逻辑有问题：这里限制了只能选自定义表实例中已选字段对应的基本表：实际应该是所有‘flag=是’的基本表（不管有没有选过当自定义表字段）
                tableNameList.add(entry.getKey().replace("_flag",""));
              }

        }
        AsConfigService asConfigService = (AsConfigService) SpringUtil.getBean("asConfigServiceImpl");
        //给tableNameList排个序
        tableNameList=asConfigService.list(new QueryWrapper<AsConfig>().in("en_table_name", tableNameList).select("distinct en_table_name").orderByAsc("sort"))
                .stream().map(AsConfig::getEnTableName).collect(Collectors.toList());

        for (String tableName : tableNameList) {
            QueryWrapper queryWrapper = new QueryWrapper<AsConfig>()
                    .eq("en_table_name", tableName)
                    .orderByAsc("sort");
            List<AsConfig> AsConfigList = asConfigService.list(queryWrapper);
            map.put(tableName, AsConfigList);
        }
        return map;
    }
}
