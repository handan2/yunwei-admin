package com.sss.yunweiadmin.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sss.yunweiadmin.common.config.GlobalParam;
import com.sss.yunweiadmin.mapper.AsTypeMapper;
import com.sss.yunweiadmin.model.entity.AsType;
import com.sss.yunweiadmin.model.entity.SysDept;
import com.sss.yunweiadmin.service.AsTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.HashMap;
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
    @Autowired
    HttpSession httpSession;

    @Override
    //20230721 用于闭环|特殊事项|外出|存储介质(报废)等流程的允许设备类型IDList(含硬盘)
    public List<Integer> getTypeIdListForSpecial(String processName) {
        List<Integer> list = new ArrayList<>();


        if( processName.contains("特殊事项") ||  processName.contains("三合一")){//进一步限制
            list.addAll(getTypeIdList(GlobalParam.typeIDForCMP));//计算机//限定计算机与服务器
            list.addAll(getTypeIdList(GlobalParam.typeIDForFWQ));
            //queryWrapper.in("type_id", typeIdList);
        } else if( processName.contains("外出")){
            list.addAll(getTypeIdList(GlobalParam.typeIDForCMP));//计算机
            list.addAll(getTypeIdList(GlobalParam.typeIDForStor));//存储介质
            list.addAll(getTypeIdList(GlobalParam.typeIDForAff));//外设
        } else {
            list.addAll(getTypeIdList(GlobalParam.typeIDForStor));//存储介质
        }

        return list;
    }

    //20230721 用于闭环|特殊事项|外出等流程的map<允许设备类型ID：自定义表ID> ；20250118这个仅用于“设备列表”界面里对已经选择的设备 组装成value2Map时的“二次筛选”
    public Map<Integer,Integer> getTypeIdCustomIdMapForSpecial() {
        List<Integer> cmpList = getTypeIdList(GlobalParam.typeIDForCMP);//计算机
        List<Integer> fwqList = getTypeIdList(GlobalParam.typeIDForFWQ);//服务器
        List<Integer> affList = getTypeIdList(GlobalParam.typeIDForAff);//外部设备
        List<Integer> netList = getTypeIdList(GlobalParam.typeIDForAff);//网络设备
        List<Integer> storList = getTypeIdList(GlobalParam.typeIDForStor);//存储介质
        List<Integer> driveList = getTypeIdList(GlobalParam.typeIDForDrive);//光驱
        Map<Integer,Integer> map = new HashMap<>();
        cmpList.forEach(i->{
            map.put(i,GlobalParam.cusTblIDForCMP);//16代表自定义“计算机信息表”ID
        });
        fwqList.forEach(i->{
            map.put(i,GlobalParam.cusTblIDForCMP);//暂也用“计算机信息表”ID
        });
        storList.forEach(i->{
            if(i != GlobalParam.typeIDForDisk)//排除硬盘
                map.put(i,GlobalParam.cusTblIDForStor);//30代表自定义“存储介质信息表”ID
        });
        netList.forEach(i->{
            map.put(i,GlobalParam.cusTblIDForStor);//暂时也用“存储介质信息表”ID
        });
        affList.forEach(i->{
            map.put(i,GlobalParam.cusTblIDForStor);//暂时也用“存储介质信息表”ID
        });
        driveList.forEach(i->{
            map.put(i,GlobalParam.cusTblIDForStor);//暂时也用“存储介质信息表”ID
        });
        return map;
    }
    //获取当前类型ID及子类型的IDList
    public List<Integer> getTypeIdList(Integer typeId) {
        List<Integer> typeIdList = new ArrayList<>();
        typeIdList.add(typeId);
       // int Typelevel = this.getById(typeId).getLevel();
//        if (Typelevel == 3) {//3是叶子结点
//            return typeIdList;
//        } else{
            List<AsType> asTypeList = this.list(new  QueryWrapper<AsType>().eq("org_id", GlobalParam.orgId).eq("pid",typeId));
            if(ObjectUtil.isNotEmpty(asTypeList)){
                List<Integer> asTypeIdList = asTypeList.stream().map(item->item.getId()).collect(Collectors.toList());
                asTypeIdList.forEach(item->typeIdList.addAll(this.getTypeIdList(item)));
            }
            return  typeIdList;
        //}

    }
    //对比上个：获取当前类型的子类型的IDList（不含本身）
    @Override
    public List<Integer> getTypeIdListNotIncludeSelf(Integer typeId) {
        List<Integer> typeIdList = new ArrayList<>();
        //typeIdList.add(typeId);
        // int Typelevel = this.getById(typeId).getLevel();
//        if (Typelevel == 3) {//3是叶子结点
//            return typeIdList;
//        } else{
        List<AsType> asTypeList = this.list(new  QueryWrapper<AsType>().eq("org_id",GlobalParam.orgId).eq("pid",typeId));
        if(ObjectUtil.isNotEmpty(asTypeList)){
            List<Integer> asTypeIdList = asTypeList.stream().map(item->item.getId()).collect(Collectors.toList());
            asTypeIdList.forEach(item->typeIdList.addAll(this.getTypeIdList(item)));
        }
        return  typeIdList;

    }
    @Override
    //20211115获取第二层分类的asType ; 这里未处理level=1的情况
    public AsType  getLevel2AsTypeById(Integer typeId) {
        AsType asType;
        List<AsType> list = this.list(new  QueryWrapper<>());//20241107 为了避免前端传(跨)部门ID参数；取消环境部门ID约束 &&  假定typeID唯一 ；.eq("org_id",orgId));
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
        return this.getOne(new  QueryWrapper<AsType>().eq("org_id",GlobalParam.orgId).eq("name", typeName));
    }
}
