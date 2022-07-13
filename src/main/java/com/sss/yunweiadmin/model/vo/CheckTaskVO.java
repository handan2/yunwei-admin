package com.sss.yunweiadmin.model.vo;

import com.sss.yunweiadmin.model.entity.ProcessDefinitionTask;
import lombok.Data;

import java.util.List;

//审批流程时，各种条件数据
@Data
public class CheckTaskVO extends ProcessDefinitionTask {
    //当前节点有多条连线
    private List<String> buttonNameList;
    //是否允许填写审批意见
  //  private String haveComment;
    private String commentTitle;
    // 是否允许修改表单
//    private String haveEditForm;
//    //是否允许指定下一步处理人
//    private String haveNextUser;
//    //是否显示操作记录
//    private String haveOperate;
    //隐藏字段组ID
//    private String hideGroupIds;
//    //隐藏字段组名称
//    private String hideGroupLabel;
//    //
//    private String haveSelectAsset;
//    //
//    private String haveSelectProcess;
}
