package com.sss.yunweiadmin.common.result;

import java.lang.annotation.*;

//用来标记方法的返回值,可以在类和方法 上面
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ResponseResultWrapper {
}
