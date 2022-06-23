package com.sss.yunweiadmin.common.config;


import com.sss.yunweiadmin.common.result.ResponseResultInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;

import java.util.ArrayList;
import java.util.List;


@Configuration
public class WebMvcConfig extends WebMvcConfigurationSupport {
    @Override
    protected void addInterceptors(InterceptorRegistry registry) {
        //配置ResponseResultInterceptor
        registry.addInterceptor(new ResponseResultInterceptor()).addPathPatterns("/**");
        //配置登录拦截器， excludeList搭配下面的拦截器使用
        List<String> excludeList = new ArrayList<>();
        excludeList.add("/sysUser/login");
        excludeList.add("/sysUser/ssoLogin");//20220324
      //  excludeList.add("/back");//20211127
        excludeList.add("/login");
        excludeList.add("/ssologin");
        excludeList.add("/redirect");
        excludeList.add("/sysUser/ssoLoginForPost");
        excludeList.add("/*.js");
        excludeList.add("/*.css");
       //registry.addInterceptor(new LoginInterceptor()).addPathPatterns("/**").excludePathPatterns(excludeList);
        super.addInterceptors(registry);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        ///static本是默认静态资源路径，但因为extends WebMvcConfigurationSupport 这个路径就自动失效了&即使没有重写这个方法：感觉不太合逻辑，但不研
        registry.addResourceHandler("/**", "/static/**").addResourceLocations("classpath:/static/");
        super.addResourceHandlers(registry);
    }
}
