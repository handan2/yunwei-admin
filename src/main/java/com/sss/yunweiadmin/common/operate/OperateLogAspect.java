package com.sss.yunweiadmin.common.operate;

import cn.hutool.extra.servlet.ServletUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.sss.yunweiadmin.model.entity.AsDeviceCommon;
import com.sss.yunweiadmin.model.entity.OperateeLog;
import com.sss.yunweiadmin.model.entity.SysUser;
import com.sss.yunweiadmin.service.OperateeLogService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.List;

@Aspect
@Component
public class OperateLogAspect {
    @Autowired
    OperateeLogService operateeLogService;
    @Autowired
    HttpSession httpSession;
    @Autowired
    HttpServletRequest httpServletRequest;

    @Pointcut("@annotation(com.sss.yunweiadmin.common.operate.OperateLog)")
    public void logPointCut() {
    }

    @Around("logPointCut()")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        long beginTime = System.currentTimeMillis();
        //执行方法
        Object result = point.proceed();
        //执行时长(毫秒)
        int time = (int) (System.currentTimeMillis() - beginTime);
        //保存日志
        saveOperateLog(point, time);

        return result;
    }

    private void saveOperateLog(ProceedingJoinPoint joinPoint, int time) {
        //
        OperateeLog operateeLog = new OperateeLog();
        //
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        OperateLog operateLog = method.getAnnotation(OperateLog.class);
        //注解
        operateeLog.setOperateModule(operateLog.module());
        operateeLog.setOperateType(operateLog.type());
        operateeLog.setOperateDescription(operateLog.description());
        //请求的方法名
        String className = joinPoint.getTarget().getClass().getName();
        String methodName = signature.getName();
        operateeLog.setMethod(className + "." + methodName + "()");
        //请求的参数
        Object[] args = joinPoint.getArgs();
        if("编辑资产".equals(operateLog.type())){
            JSONArray arr =JSON.parseArray((JSON.toJSONString(args)));
            JSONObject object = (JSONObject) arr.get(0);//只有一个元素
            AsDeviceCommon asDeviceCommon=JSON.parseObject(object.getString("asDeviceCommon"),AsDeviceCommon.class);
            //  AsDeviceCommon asDeviceCommon = (AsDeviceCommon)object.get("asDeviceCommon");
            operateeLog.setParam(asDeviceCommon.toString());
        }else if("上传资产".equals(operateLog.type())){

        }else
        operateeLog.setParam(JSON.toJSONString(args).replaceAll("\"", ""));
        //20211116




        //System.out.println(arr.get(0).);
        // System.out.println(JSON.toJSONString(args).replaceAll("\"", ""));
        //IP地址
        operateeLog.setIp(ServletUtil.getClientIP(httpServletRequest));
        //用户名
        SysUser user = (SysUser) httpSession.getAttribute("user");
        if (user == null) {
            throw new RuntimeException("用户未登录");
        }
        operateeLog.setLoginName(user.getLoginName());
        operateeLog.setDisplayName(user.getDisplayName());
        //时间
        operateeLog.setTime(time);
        operateeLog.setCreateDatetime(LocalDateTime.now());
        //保存系统日志
        operateeLogService.save(operateeLog);
    }

}
