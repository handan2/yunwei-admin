package com.sss.yunweiadmin.mapper;

import java.util.LinkedHashMap;
import java.util.List;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author 任勇林
 * @since 2021-04-14
 */
public interface AnySqlMapper {
    /*
        sql的返回值必须为字符串，如下列格式：
        select concat_ws(',',column_name,data_type) as result from information_schema.columns where table_schema='yunwei' and table_name = 'as_device_common'
     */
    public List<LinkedHashMap<String, String>> execute(String sql);
}
