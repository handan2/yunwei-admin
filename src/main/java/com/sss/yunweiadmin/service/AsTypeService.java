package com.sss.yunweiadmin.service;

import com.sss.yunweiadmin.model.entity.AsType;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author 任勇林
 * @since 2021-10-08
 */
public interface AsTypeService extends IService<AsType> {
    AsType getLevel2AsTypeById(Integer typeId);
    AsType getAsType(String typeName);
    List<Integer> getTypeIdList(Integer typeId);
}
