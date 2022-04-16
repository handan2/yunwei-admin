package com.sss.yunweiadmin.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sss.yunweiadmin.mapper.AsTypeMapper;
import com.sss.yunweiadmin.model.entity.AsType;
import com.sss.yunweiadmin.service.AsTypeService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 任勇林
 * @since 2021-10-08
 */
@Service
public class AsTypeServiceImpl extends ServiceImpl<AsTypeMapper, AsType> implements AsTypeService {
    @Override
    //获取当前类型ID及子类型的IDList
    public List<Integer> getTypeIdList(Integer typeId) {
        List<Integer> typeIdList = new ArrayList<>();
        typeIdList.add(typeId);
       // int Typelevel = this.getById(typeId).getLevel();
//        if (Typelevel == 3) {//3是叶子结点
//            return typeIdList;
//        } else{
            List<AsType> asTypeList = this.list(new QueryWrapper<AsType>().eq("pid",typeId));
            if(ObjectUtil.isNotEmpty(asTypeList)){
                List<Integer> asTypeIdList = asTypeList.stream().map(item->item.getId()).collect(Collectors.toList());
                asTypeIdList.forEach(item->typeIdList.addAll(this.getTypeIdList(item)));
            }
            return  typeIdList;
        //}

    }

    @Override
    //20211115获取第二层分类的asType ; 这里未处理level=1的情况
    public AsType  getLevel2AsTypeById(Integer typeId) {
        AsType asType;
        List<AsType> list = this.list();
        Map<Integer, AsType> map = list.stream().collect(Collectors.toMap(AsType::getId, AsType -> AsType));
        while (true) {
            AsType asTypeTmp = map.get(typeId);
            if (asTypeTmp.getLevel() == 2) {
                asType = asTypeTmp;
                break;
            } else {
                typeId = asTypeTmp.getPid();
            }
        }
        return asType;
    }

    @Override
    public AsType getAsType(String typeName) {
        return this.getOne(new QueryWrapper<AsType>().eq("name", typeName));
    }
}
