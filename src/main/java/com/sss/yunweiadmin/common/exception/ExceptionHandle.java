package com.sss.yunweiadmin.common.exception;

import com.alibaba.fastjson.JSON;
import com.google.common.base.Throwables;
import com.sss.yunweiadmin.common.result.ResponseResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.http.HttpServletRequest;

@RestControllerAdvice
@Slf4j
public class ExceptionHandle {

    //前端提示异常
    @ExceptionHandler(PageTipException.class)
    public ResponseResult pageTipException(HttpServletRequest request, Exception e) {
        return ResponseResult.fail(e.getMessage().length() > 50 ? "java代码错误" : e.getMessage());
    }

    //未知异常
    @ExceptionHandler(Exception.class)
    public ResponseResult exception(HttpServletRequest request, Exception e) {
        //记录异常信息到日志
        log.error("url={}", request.getRequestURL());
        log.error("method={}", request.getMethod());
        //目前只能获取get请求的参数，post获取不到！！！
        log.error("params={}", JSON.toJSONString(request.getParameterMap()));
        log.error("error={}", Throwables.getStackTraceAsString(e));
        return ResponseResult.fail(e.getMessage().length() > 50 ? "java代码错误" : e.getMessage());
    }
}
