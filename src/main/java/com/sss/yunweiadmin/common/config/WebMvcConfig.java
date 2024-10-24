package com.sss.yunweiadmin.common.config;


import com.sss.yunweiadmin.common.result.ResponseResultInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;

import java.util.ArrayList;
import java.util.List;


@Configuration
public class WebMvcConfig extends WebMvcConfigurationSupport {
    @Override
    protected void addInterceptors(InterceptorRegistry registry) {
        //配置ResponseResultInterceptor;
        registry.addInterceptor(new ResponseResultInterceptor()).addPathPatterns("/**");
        //配置登录拦截器， excludeList搭配下面的拦截器使用
        List<String> excludeList = new ArrayList<>();
        excludeList.add("/sysUser/login1");//后端部署时前端发的静态登陆页面请求
        excludeList.add("/sysUser/login");//登陆时前端页面发来的ajax
        excludeList.add("/sysUser/ssoLogin");//20220324
        excludeList.add("/sysUser/todoForSSO");//20230503
        excludeList.add("/sysUser/netgateLogin");//20230805
        excludeList.add("/sysUser/todoListForSSO");//20230503
        excludeList.add("/sysUser/ssoLoginForUserVO");//20230513
        excludeList.add("/processInstanceData/addCandidate");//20230714
        excludeList.add("/asDeviceCommon/listForSoftware");//
        excludeList.add("/asDeviceCommon/getAsDeviceCommonBySap");//20241023
      //  excludeList.add("/back");//20211127
        excludeList.add("/login1");
        excludeList.add("/ssologin");
        excludeList.add("/netgateLogin");
        excludeList.add("/redirect");
        excludeList.add("/sysUser/printLogin");
        excludeList.add("/*.js");
        excludeList.add("/*.css");
        excludeList.add("/sysUser/download");
        excludeList.add("/download/*.*");
        excludeList.add("/static/*.*");
        registry.addInterceptor(new LoginInterceptor()).addPathPatterns("/**").excludePathPatterns(excludeList);
        super.addInterceptors(registry);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        ///static本是默认静态资源路径，但因为extends WebMvcConfigurationSupport 这个路径就自动失效了&即使没有重写这个方法：感觉不太合逻辑，但不研
        //20231227 登陆拦截器的优先级高于这里; 这里的配置要清缓存||修改“index.html里的静态资源路径参数”才起作用
        registry.addResourceHandler("/**", "/static/**").addResourceLocations("classpath:/static/");
        //registry.addResourceHandler( "/static/**").addResourceLocations("classpath:/static/");
        registry.addResourceHandler( "/download/**").addResourceLocations("file:///E:/");
        super.addResourceHandlers(registry);
    }



}


