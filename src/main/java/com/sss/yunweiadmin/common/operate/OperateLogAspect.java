package com.sss.yunweiadmin.common.operate;

import cn.hutool.core.util.ObjectUtil;
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
   //20230214 切入点为：使用注解“com.sss.yunweiadmin.common.operate.OperateLog”的类/方法
    @Pointcut("@annotation(com.sss.yunweiadmin.common.operate.OperateLog)")
    public void logPointCut() {//注：在之前的（aspect注解驱动）日志/demo中：直接“绑定”切入点的这个方法也是内容为空，具体的操作是放在“通知方法”中（如下面那个方法）
    }
   //环绕通知：暂不深研
    @Around("logPointCut()")//“通知”方法：具体的操作过程；参数为“上面那个方法”
    //PS:之前日志中demo的通知方法里无参；（ProceedingJoinPoint类型）参数是AOP本身定义的，在本例中用于执行“被监控”的用户方法+（在saveOperateLog方法中）读取（被监控方法相关的）参数
    public Object around(ProceedingJoinPoint point) throws Throwable {
        long beginTime = System.currentTimeMillis();
        //执行方法
        Object result = point.proceed();//调用“被监控”的用户方法
        //执行时长(毫秒)
        int time = (int) (System.currentTimeMillis() - beginTime);
        //保存日志
        saveOperateLog(point, time);

        return result;
    }

    private void saveOperateLog(ProceedingJoinPoint joinPoint, int time) {

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
        if ("编辑资产".equals(operateLog.type())) {
            JSONArray arr = JSON.parseArray((JSON.toJSONString(args)));
            JSONObject object = (JSONObject) arr.get(0);//只有一个元素
            AsDeviceCommon asDeviceCommon = JSON.parseObject(object.getString("asDeviceCommon"), AsDeviceCommon.class);
            //  AsDeviceCommon asDeviceCommon = (AsDeviceCommon)object.get("asDeviceCommon");
            operateeLog.setParam(asDeviceCommon.toString());
        } else if (operateLog.type().contains("上传") || operateLog.type().contains("下载")) {//20230207 添加对“下载”的过滤：不过感觉在这里过滤和在action里直接不用那个日志注解效果可能一样：暂不研

        } else
            operateeLog.setParam(JSON.toJSONString(args).replaceAll("\"", ""));


        //20211116


        //System.out.println(arr.get(0).);
        // System.out.println(JSON.toJSONString(args).replaceAll("\"", ""));
        //IP地址
        operateeLog.setIp(ServletUtil.getClientIP(httpServletRequest));
        //用户名
        SysUser user = (SysUser) httpSession.getAttribute("user");
        if (user == null) {
            throw new RuntimeException("用户未登录或登陆超时，请关闭本页面，重新从登陆入口进入");
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
