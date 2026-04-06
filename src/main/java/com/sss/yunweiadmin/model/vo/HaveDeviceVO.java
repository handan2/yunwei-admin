package com.sss.yunweiadmin.model.vo;

import com.sss.yunweiadmin.model.entity.*;
import lombok.Data;

import java.util.List;

@Data
public class HaveDeviceVO {
    //该字段是标志位
    private String haveDevice ;//前端form相关的values有与这个标志位等值的“name/值”成员时，前端的formRule/formItemNameHandle方法才会进行序列化/反序列化
    private String deviceStr;

}
