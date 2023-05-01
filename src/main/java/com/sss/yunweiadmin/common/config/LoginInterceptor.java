package com.sss.yunweiadmin.common.config;
//https://blog.csdn.net/leeta521/article/details/119532691

import com.sss.yunweiadmin.model.entity.SysUser;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class LoginInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        SysUser user = (SysUser) request.getSession().getAttribute("user");
        if (user == null) {
            //用户未登录1,重定向到登录页面；不起作用：no,20211128
            // response.sendRedirect(request.getContextPath() + "/login");
            throw new RuntimeException("用户未登录或登陆超时，请关闭本页面，重新从登陆入口进入");
        }
        return true;
    }
}
