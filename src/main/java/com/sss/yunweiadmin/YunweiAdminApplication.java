package com.sss.yunweiadmin;


import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(exclude = {
        org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
        org.activiti.spring.boot.SecurityAutoConfiguration.class}
)
@MapperScan("com.sss.yunweiadmin.mapper")
@EnableScheduling // 启用定时任务
public class YunweiAdminApplication {
    public static void main(String[] args) {
        SpringApplication.run(YunweiAdminApplication.class, args);
    }

}
