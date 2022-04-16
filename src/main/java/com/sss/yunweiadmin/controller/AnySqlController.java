package com.sss.yunweiadmin.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sss.yunweiadmin.mapper.AnySqlMapper;
import com.sss.yunweiadmin.model.entity.AsConfig;
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

@RestController
@RequestMapping("/anySqlController")
public class AnySqlController {
    @Autowired
    AnySqlMapper anySqlMapper;
    @Autowired
    AsConfigService asConfigService;

    private String getZh(String columnType) {
        if (columnType.equals("int") || columnType.equals("double")) {
            return "数字";
        } else if (columnType.equals("varchar")) {
            return "字符串";
        } else if (columnType.equals("date")) {
            return "日期";
        } else if (columnType.equals("datetime")) {
            return "日期时间";
        } else {
            return "不知道";
        }
    }


    @GetMapping("getTableInfo")
    public boolean getTableInfo(String tableName) {
        String table_schema = "yunwei";
        String sql = "select concat_ws(',',column_name,data_type) from information_schema.columns where table_schema='" + table_schema + "' and table_name = '" + tableName + "'";
        List<LinkedHashMap<String, String>> resultList = anySqlMapper.execute(sql);
        for (LinkedHashMap<String, String> resultMap : resultList) {
            for (Map.Entry<String, String> entry : resultMap.entrySet()) {
                String[] arr = entry.getValue().split(",");
                String columnName = arr[0];
                String columnType = arr[1];
            }
        }

        return true;
    }

    @GetMapping("generateFormItem")
    public boolean generateFormItem(String enTableName) {
        List<String> resultList = new ArrayList<>();
        List<AsConfig> list = asConfigService.list(new QueryWrapper<AsConfig>().eq("en_table_name", enTableName));
        for (AsConfig asConfig : list) {
            String name = StrUtil.toCamelCase(asConfig.getEnTableName() + "." + asConfig.getEnColumnName());
            String label = asConfig.getZhColumnName();
            if (asConfig.getType().equals("数字")) {
                resultList.add("<Col span={span}>");
                resultList.add("<FormItem name='" + name + "' label='" + label + "'><InputNumber style={{ width: width }}/></FormItem>");
                resultList.add("</Col>");
            } else if (asConfig.getType().equals("日期")) {
                resultList.add("<Col span={span}>");
                resultList.add("<div style={{ display: 'none' }}><FormItem name='" + name + "'><Input/></FormItem></div>");
                resultList.add("<FormItem name='" + name + "Tmp' label='" + label + "' width={width}");
                resultList.add("onChange={date => {");
                resultList.add(" if (date) {");
                resultList.add("core.setValue('" + name + "', date.format('YYYY-MM-DD'))");
                resultList.add(" } else {");
                resultList.add("core.setValue('" + name + "', '')");
                resultList.add("}");
                resultList.add(" }}>");
                resultList.add("<DatePicker locale={locale} format=\"YYYY-MM-DD\" style={{ width: width }}/>");
                resultList.add("</FormItem>");
                resultList.add("</Col>");
            } else if (asConfig.getType().equals("日期时间")) {
                resultList.add("<Col span={span}>");
                resultList.add("<div style={{ display: 'none' }}><FormItem name='" + name + "'><Input/></FormItem></div>");
                resultList.add("<FormItem name='" + name + "Tmp' label='" + label + "' width={width}");
                resultList.add("onChange={date => {");
                resultList.add(" if (date) {");
                resultList.add("core.setValue('" + name + "', date.format('YYYY-MM-DD'))");
                resultList.add(" } else {");
                resultList.add("core.setValue('" + name + "', '')");
                resultList.add("}");
                resultList.add(" }}>");
                resultList.add("<DatePicker locale={locale} format=\"YYYY-MM-DD HH:mm:ss\" style={{ width: width }}/>");
                resultList.add("</FormItem>");
                resultList.add("</Col>");
            } else {
                resultList.add("<Col span={span}>");
                resultList.add("<FormItem name='" + name + "' label='" + label + "'><Input style={{ width: width }}/></FormItem>");
                resultList.add("</Col>");
            }
        }
        System.out.println(resultList.stream().collect(Collectors.joining(System.getProperty("line.separator"))));

        return true;
    }
}
