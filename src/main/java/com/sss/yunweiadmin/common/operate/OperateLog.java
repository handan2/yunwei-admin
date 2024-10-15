package com.sss.yunweiadmin.common.operate;

import java.lang.annotation.*;

//示例：@OperationLog(module = "用户模块-用户列表",type = "查询",description = "查询所有用户")

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperateLog {
    // 操作模块
    String module() default "";

    // 操作类型
    String type() default "";

    // 操作说明
    String description() default "";
}
