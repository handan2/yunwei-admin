package com.sss.yunweiadmin.common.result;

import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;

//拦截所有的请求，解析@ResponseResultWrapper注解，设置一个属性标记
public class ResponseResultInterceptor implements HandlerInterceptor {
    private static final String RESPONSE_RESULT_TAG = "response_result_tag";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (handler instanceof HandlerMethod) {
            final HandlerMethod handlerMethod = (HandlerMethod) handler;
            final Class<?> clazz = handlerMethod.getBeanType();
            final Method method = handlerMethod.getMethod();
            //判断类、方法上是否有ResponseResult注解，有，就设置一个属性标记
            if (clazz.isAnnotationPresent(ResponseResultWrapper.class)) {
                //先设置，然后往下传递，在ResponseBodyAdvice的实现中进行判断
                request.setAttribute(RESPONSE_RESULT_TAG, clazz.getAnnotation(ResponseResultWrapper.class));
            } else if (method.isAnnotationPresent(ResponseResultWrapper.class)) {
                request.setAttribute(RESPONSE_RESULT_TAG, method.getAnnotation(ResponseResultWrapper.class));
            }
        }
        return true;
    }
}
