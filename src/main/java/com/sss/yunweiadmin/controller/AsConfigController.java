package com.sss.yunweiadmin.controller;


import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sss.yunweiadmin.common.result.ResponseResultWrapper;
import com.sss.yunweiadmin.mapper.AnySqlMapper;
import com.sss.yunweiadmin.model.entity.AsConfig;
import com.sss.yunweiadmin.model.vo.ValueLabelVO;
import com.sss.yunweiadmin.service.AsConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * 表名，表字段的中英文对应表 前端控制器
 * </p>
 *
 * @author 任勇林
 * @since 2021-03-19
 */
@RestController
@RequestMapping("/asConfig")
@ResponseResultWrapper
public class AsConfigController {
    @Autowired
    private AsConfigService asConfigService;
    @Autowired
    AnySqlMapper anySqlMapper;

    @GetMapping("getTableList")
    public List<ValueLabelVO> getTablelist() {
        //select distinct en_table_name,zh_table_name from as_config where en_table_name not in('as_device_common')
        List<AsConfig> list = asConfigService.list(new QueryWrapper<AsConfig>().select("distinct en_table_name,zh_table_name").notIn("en_table_name", "as_device_common").orderByAsc("sort"));

        List<ValueLabelVO> list2 = list.stream().map(tmp -> new ValueLabelVO(tmp.getEnTableName(), tmp.getZhTableName())).collect(Collectors.toList());
        return list2;
    }

    @GetMapping("getColumnList")
    public List<ValueLabelVO> list(String enTableName) {
        List<AsConfig> list = asConfigService.list(new QueryWrapper<AsConfig>().eq("en_table_name", enTableName).orderByAsc("sort"));
        List<ValueLabelVO> list2 = list.stream().map(tmp -> new ValueLabelVO(tmp.getId(), tmp.getZhColumnName())).collect(Collectors.toList());
        return list2;
    }

    @GetMapping("getTableData")
    public Map<String, List<ValueLabelVO>> getTableData() {
        Map<String, List<ValueLabelVO>> map = new LinkedHashMap<>();
        List<AsConfig> AsConfigList = asConfigService.list(new QueryWrapper<AsConfig>().orderByAsc("sort"));
        String key = null;
        for (AsConfig asConfig : AsConfigList) {
            key = asConfig.getEnTableName() + "," + asConfig.getZhTableName();
            if (map.get(key) == null) {
                List<ValueLabelVO> list = new ArrayList<>();
                list.add(new ValueLabelVO(asConfig.getId(), asConfig.getZhColumnName()));
                map.put(key, list);
            } else {
                map.get(key).add(new ValueLabelVO(asConfig.getId(), asConfig.getZhColumnName()));
            }
        }
        return map;
    }

    @GetMapping("getExcelEntity")
    public boolean getExcelEntity(String enTableName) {
        List<String> resultList = new ArrayList<>();
        List<AsConfig> list = asConfigService.list(new QueryWrapper<AsConfig>().eq("en_table_name", enTableName).orderByAsc("sort"));
        for (AsConfig asConfig : list) {
            resultList.add("@ExcelProperty(\"" + asConfig.getZhColumnName() + "\")");
            if (asConfig.getType().equals("字符串")) {
                resultList.add("private String " + StrUtil.toCamelCase(asConfig.getEnColumnName()) + ";");
            } else if (asConfig.getType().equals("数字")) {
                resultList.add("private Integer " + StrUtil.toCamelCase(asConfig.getEnColumnName()) + ";");
            } else if (asConfig.getType().equals("日期")) {
                resultList.add("private LocalDate " + StrUtil.toCamelCase(asConfig.getEnColumnName()) + ";");
            }else if (asConfig.getType().equals("日期时间")) {
                resultList.add("private LocalDateTime " + StrUtil.toCamelCase(asConfig.getEnColumnName()) + ";");
            }
        }
        System.out.println(resultList.stream().collect(Collectors.joining(System.getProperty("line.separator"))));
        return true;
    }
}
