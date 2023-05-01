package com.sss.yunweiadmin.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sss.yunweiadmin.common.utils.ExcelDateUtil;
import com.sss.yunweiadmin.model.entity.*;
import com.sss.yunweiadmin.mapper.InfoNoMapper;
import com.sss.yunweiadmin.model.excel.InfoNoExcel;
import com.sss.yunweiadmin.service.InfoNoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 任勇林
 * @since 2022-08-24
 */
@Service
public class InfoNoServiceImpl extends ServiceImpl<InfoNoMapper, InfoNo> implements InfoNoService {
    @Override
    public String addExcel(List<InfoNoExcel> excelList, String importMode){
        //去掉db中存在的信息点号，剩下页面上需要导入的信息点号
        List<InfoNoExcel> pageList;
        //db中存在的信息点号
        List<InfoNo> dbList = new ArrayList<>();

        //处理日期类型
       // ExcelDateUtil.converToDate(excelList, InfoExcel.class);
        /*
            importMode=是，先删除，后全部插入信息点号
            importMode=否，插入不在db中的信息点号
         */
        //20211116读出DB与EXCEL资产号重复的记录; 20220825 有时间根据下面调整的结果把张强的资产导入代码改了
        List<InfoNo> dupList = this.list(new QueryWrapper<InfoNo>().in("value", excelList.stream().map(InfoNoExcel::getValue).collect(Collectors.toList())));
        if (importMode.equals("是")) {//可 重复&覆盖，把重复的从DB删除
            if (ObjectUtil.isNotEmpty(dupList)) {
                dbList = dupList;
                List<Integer> idList = dupList.stream().map(InfoNo::getId).collect(Collectors.toList());
                this.removeByIds(idList);
            }
            pageList = excelList;
        } else {//不可 重复&覆盖
            if (ObjectUtil.isNotEmpty(dupList)) {//有与DB重复的情况，注意：如果数据均是重复的，这里处理完后pageList里没数据了
                dbList = dupList;
                Set<String> valueSet = dupList.stream().map(InfoNo::getValue).collect(Collectors.toSet());//value字段是信息点号的值字段
                pageList = excelList.stream().filter(item -> !valueSet.contains(item.getValue())).collect(Collectors.toList());
            } else {//此时导入的value均不存在DB中
                pageList = excelList;
            }
        }
        if (ObjectUtil.isNotEmpty(pageList)) {
            for (InfoNoExcel infoNoExcel : pageList) {
                if (ObjectUtil.isEmpty(infoNoExcel.getValue())) {
                    throw new RuntimeException("信息点号不能为空");
                }
                if (ObjectUtil.isEmpty(infoNoExcel.getNetType())) {
                    throw new RuntimeException("联网类别不能为空");
                }
                InfoNo InfoNo = new InfoNo();BeanUtils.copyProperties(infoNoExcel, InfoNo);
                this.save(InfoNo);
            }
        }
        if (importMode.equals("是")) {
            int pageCount = pageList.size() - dbList.size();
            return "导入" + pageCount + "条资产;信息点号：" + dbList.stream().map(InfoNo::getValue).collect(Collectors.joining(",")) + ",已经被覆盖";
        } else {
            int pageCount = pageList.size();
            if (ObjectUtil.isEmpty(dbList)) {
                return "导入" + pageCount + "条资产";
            } else {
                return "导入" + pageCount + "条资产;信息点号：" + dbList.stream().map(InfoNo::getValue).collect(Collectors.joining(",")) + "已经存在，未导入";
            }
        }


    }


}
