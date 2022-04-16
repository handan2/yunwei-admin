package com.sss.yunweiadmin;
import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;

import org.activiti.engine.HistoryService;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.repository.Deployment;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Map;

/**
 *
 */
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
@EnableAutoConfiguration(exclude = SecurityAutoConfiguration.class)
public class testProcess {

//    @Autowired
//    RuntimeService runtimeService;
//    @Autowired
//    RepositoryService repositoryService;
//    @Autowired
//    TaskService taskService;
//    @Autowired
//    HistoryService historyService;
//    @Autowired
//    ProcessRuntime processRuntime;

    @Autowired
    RuntimeService runtimeService;
    @Autowired
    RepositoryService repositoryService;
    @Autowired
    TaskService taskService;
    @Autowired
    HistoryService historyService;

    //    @Autowired
//    SysRoleUserService sysRoleUserService;


    public Integer bbb(){
        return null;//null不能返回给int,但可以是Integer
    }
    @Test//@Test注解修饰的方法只能是void
    public void abc(){
        System.out.println("111");

        JSONObject jsonObject = JSONObject.parseObject("{\"1550\":\"秘密\",\"1551\":\"内网\",\"1552\":\"未分配\",\"asset\":[{\"customTableId\":16,\"asId\":102002}],\"16.计算机信息表.as_device_common.no.75\":\"J0601111\",\"16.计算机信息表.as_device_common.name.76\":\"桌面计算机\",\"16.计算机信息表.as_device_common.type_id.74\":\"联网终端\",\"16.计算机信息表.as_device_common.model.94\":\"M4500T\",\"16.计算机信息表.as_device_common.sn.95\":\"111111111\",\"16.计算机信息表.as_device_common.buy_date.97\":\"2015-01-02\",\"16.计算机信息表.as_device_common.use_date.98\":\"2015-01-01\",\"16.计算机信息表.as_device_common.fund_src.78\":\"折旧资金\",\"16.计算机信息表.as_device_common.net_type.79\":\"试验网\",\"16.计算机信息表.as_device_common.state.85\":\"在用\",\"16.计算机信息表.as_device_common.user_name.89\":\"任勇林\",\"16.计算机信息表.as_device_common.user_dept.90\":\"信息化中心\"}");
        for (Map.Entry entry : jsonObject.entrySet()) {
            System.out.println(entry.getKey());
                System.out.println(entry.getValue().toString());

        }


            String a = "abc";
        String b = "ab11";
        String[] a11 = {a,b};
       // System.out.println(a11[0]);
        Integer c =this.bbb();
        System.out.println(ObjectUtil.isNotEmpty(0));

    }
    @Test
    public void deploy() {
        repositoryService.createDeployment()
                .name("MyProcess")
                .addClasspathResource("processes/MyProcess.bpmn")
                .addClasspathResource("processes/MyProcess.png")
                .deploy();
    }

    //20210711 测试启动流程实例
    @Test
    public void startProcess1() {
        //是否部署流程 20210201下面那句不加.deploymentId("ProcessForMini")会报错，提示查询出多个定义超过max of 1的限制之类的报错：暂不研
        Deployment deployment = repositoryService.createDeploymentQuery().deploymentName("MyProcess").singleResult();
        if (null == deployment) {
            System.out.println("no deploy");

        }
        String processDefinitionKey = "MyProcess";
        runtimeService.startProcessInstanceByKey(processDefinitionKey);
        System.out.println("start sucessfully");
    }

//
    /**
     * 启动流程实例,act7 api
     *
     */
//    @Test
//    public void testStartProcess(){
//        String userName = "zhangsan";
//        //securityUtil.logInAs("salaboy");
//        ProcessInstance myGroup = processRuntime.start(ProcessPayloadBuilder
//                .start()
//                .withProcessDefinitionKey("MyProcess")
////                .withVariable("username",userName) 流程定义变量
//                .build());
//
//        log.info("流程实例ID ={}",myGroup.getId());
//    }
//
//    /**
//     * 查询，并完成任务
//     */
//    @Test
//    public void testTask(){
//        securityUtil.logInAs("ryandawsonuk");
//        Page<Task> tasks = taskRuntime.tasks(Pageable.of(0, 10));
//        if (tasks.getTotalItems() > 0){
//            for (Task task: tasks.getContent()) {
//                //拾取任务
//                taskRuntime.claim(TaskPayloadBuilder.claim().withTaskId(task.getId()).build());
//                log.info("拾取任务={}",task);
//                //执行任务
//                taskRuntime.complete(TaskPayloadBuilder.complete().withTaskId(task.getId()).build());
//            }
//        }
//        tasks = taskRuntime.tasks(Pageable.of(0, 10));
//        if (tasks.getTotalItems() > 0){
//            for (Task task: tasks.getContent()) {
//                log.info("任务={}",task);
//            }
//        }
//    }


}