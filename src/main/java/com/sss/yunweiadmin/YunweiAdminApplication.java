package com.sss.yunweiadmin;


import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(exclude = {
        org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
        org.activiti.spring.boot.SecurityAutoConfiguration.class}
)
@MapperScan("com.sss.yunweiadmin.mapper")
public class YunweiAdminApplication {
    public static void main(String[] args) {
        SpringApplication.run(YunweiAdminApplication.class, args);
    }

}
