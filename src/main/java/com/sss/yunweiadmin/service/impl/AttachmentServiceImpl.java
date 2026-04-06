package com.sss.yunweiadmin.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sss.yunweiadmin.common.config.GlobalParam;
import com.sss.yunweiadmin.model.entity.Attachment;
import com.sss.yunweiadmin.mapper.AttachmentMapper;
import com.sss.yunweiadmin.model.entity.ProcessInstanceData;
import com.sss.yunweiadmin.service.AttachmentService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 任勇林
 * @since 2024-01-12
 */
@Service
public class AttachmentServiceImpl extends ServiceImpl<AttachmentMapper, Attachment> implements AttachmentService {
    @Autowired
    ProcessInstanceDataServiceImpl processInstanceDataService;
    @Autowired
    private Environment environment;


    @Override
    @SneakyThrows
    @Transactional(rollbackFor = Exception.class)
    public boolean uploadAttach(MultipartFile[] files, String formValue) {
        JSONObject jsonObject = JSON.parseObject(formValue);
        String fujianMiji = jsonObject.getString("fujianMiji");
        String orderNum = jsonObject.getString("orderNum");
        if (ObjectUtil.isNotEmpty(files)) {
            List<String> attachmentNameList =  new ArrayList<>();
            String localPath_prefix = environment.getProperty("downloadRoot");
            String route = "";
            Integer sourceId = null;
            String localPath ="";
            String attachmentIds_s = "";
            List<String> attachmentIds_sList = new ArrayList<>();
            List<Integer> attachmentIdsList ;
            ProcessInstanceData processInstanceData = null;
            if (ObjUtil.isNotEmpty(orderNum)) {

                processInstanceData = processInstanceDataService.getOne(new QueryWrapper<ProcessInstanceData>().eq("org_id", GlobalParam.orgId).eq("order_num", orderNum));
                if (ObjUtil.isEmpty(processInstanceData))
                    throw new RuntimeException("该流程编号不存在！");
                route = processInstanceData.getOrderNum();
                sourceId = processInstanceData.getId();
                attachmentIds_s = processInstanceData.getAttachmentIds();//流程实例里原有的附件ids
                localPath = localPath_prefix + processInstanceData.getOrderNum() + "/";


                if(ObjUtil.isNotEmpty(attachmentIds_s)){//20241122 初始值为流程实例里的str解析成的LIST,后面会加入新加的附件ID(用于 update instance）
                    String[] sss = attachmentIds_s.split(",");
                    for(String s : sss){
                        attachmentIds_sList.add(s);
                    }
                    attachmentIdsList = attachmentIds_sList.stream().map(i->Integer.valueOf(i)).collect(Collectors.toList());;
                    List<Map<String, Object>> listMaps = this.listMaps(new  QueryWrapper<Attachment>().eq("org_id",GlobalParam.orgId).in("id",attachmentIdsList).select("name"));
                    attachmentNameList = listMaps.stream().map(item -> String.valueOf(item.get("name"))).collect(Collectors.toList());
                }
            } else {//非流程的“全局附件” 注意赋值： attachmentNameList
                localPath = localPath_prefix;
                List<Map<String, Object>> listMaps = this.listMaps(new  QueryWrapper<Attachment>().eq("org_id",GlobalParam.orgId).eq("route",""));
                attachmentNameList = listMaps.stream().map(item -> String.valueOf(item.get("name"))).collect(Collectors.toList());
            }
            for (MultipartFile file : files) {
                //MultipartFile file = files[0];
                Attachment attachment = new Attachment();
                attachment.setCreateDatetime(LocalDateTime.now());
                attachment.setMiji(fujianMiji);
                attachment.setRoute(route);
                //下面这两个processInstanceDataService的相关方法只是为了处理下字符串，有空还是要挪到专门的工具类
                String suffix = processInstanceDataService.subStringByLastPeriod(file.getOriginalFilename(), "right");
                attachment.setType(suffix);
                String fileNameSaved = processInstanceDataService.subStringByLastPeriod(file.getOriginalFilename(), "left") + "(" + fujianMiji + ")" + suffix;
                if(attachmentNameList.contains(fileNameSaved)){
                    if(ObjUtil.isNotEmpty(orderNum))
                        throw new RuntimeException("该流程已存在同名附件，请删除原文件或给新文件重命名！");
                    else
                        throw new RuntimeException("发现重名附件，若要替换请先删除原文件！");
                }
                attachment.setName(fileNameSaved);
                attachment.setSourceId(sourceId);
                if (!FileUtil.exist(localPath)) {
                    FileUtil.mkdir(localPath);
                    System.out.println("Directory created: " + localPath);
                } else {
                    System.out.println("Directory already exists.");
                }
                this.save(attachment);
                if (ObjUtil.isNotEmpty(orderNum))
                    attachmentIds_sList.add(String.valueOf(attachment.getId()));
                file.transferTo(new File(localPath + "/" + fileNameSaved));
            }
            if (ObjUtil.isNotEmpty(orderNum)) {
                attachmentIds_s = attachmentIds_sList.stream().collect(Collectors.joining(","));
                processInstanceData.setAttachmentIds(attachmentIds_s);
                return processInstanceDataService.updateById(processInstanceData);
            }
            return true;


        }





        return false;

    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean delete(Integer[] idArr) {
        List<Integer> idList = Stream.of(idArr).collect(Collectors.toList());


        //20241121 删除磁盘文件逻辑
      //  List<Integer> ids = Arrays.asList(idArr);
        //20241121 根据有没有route(流程编号) 来组装本地文件路径

        idList.stream().forEach(i->{
            Attachment attachment = this.getById(Integer.valueOf(i));
            if(ObjUtil.isNotEmpty(attachment.getRoute())){
                ProcessInstanceData processInstanceData = processInstanceDataService.getById(attachment.getSourceId());
                if(ObjUtil.isEmpty(processInstanceData))
                    throw new RuntimeException("流程(" + attachment.getRoute() + ")不存在！");

                String localPath = environment.getProperty("downloadRoot") + attachment.getRoute() + "/" + attachment.getName();
                if (FileUtil.exist(localPath)) {
                    // 删除目录及其所有内容
                    FileUtil.del(localPath);
                    System.out.println("Directory and all its contents deleted successfully.");
                }
                //更新processInstanceData中的attachment_ids字段
                //读取i所对应的流程实例中的附件Ids字段值
                String ids_str = processInstanceData.getAttachmentIds();
                List<String> list = Arrays.asList(ids_str.split("\\,"));
                List<String> list1 = list.stream().filter(item->!(item.equals(String.valueOf(i)))).collect(Collectors.toList());//删除值为i的成员

                String ids_str_new = String.join(",", list1);//测list是空时会不会报错：不会
                processInstanceData.setAttachmentIds(ids_str_new);
                processInstanceDataService.updateById(processInstanceData);


            }
        });


        return this.removeByIds(idList);
    }
}
