package com.sss.yunweiadmin.common.result;


import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import javax.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class ResponseResultAdvice implements ResponseBodyAdvice<Object> {
    private static final String RESPONSE_RESULT_TAG = "response_result_tag";


    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        ServletRequestAttributes sra = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = sra.getRequest();
        ResponseResultWrapper responseResultWrapper = (ResponseResultWrapper) request.getAttribute(RESPONSE_RESULT_TAG);
        return responseResultWrapper != null;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType, Class<? extends HttpMessageConverter<?>> selectedConverterType, ServerHttpRequest request, ServerHttpResponse response) {
        if (body == null) {
            return ResponseResult.success("null");
        }
        if (body instanceof ResponseResult) {
            return body;
        }
        if (body.toString().toLowerCase().contains("error")) {
            return ResponseResult.fail();
        }
        return ResponseResult.success(body);
    }
}
