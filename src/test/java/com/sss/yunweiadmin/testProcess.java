package com.sss.yunweiadmin;

import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.sss.yunweiadmin.common.utils.SpringUtil;
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

import java.lang.reflect.Field;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;



class Person {

    private String name;
    private String desc;

    public Person(String name, String desc) {
        this.name = name;
        this.desc = desc;
    }

    public String getName() {
        return name;
    }

    public Person setName(String name) {
        this.name = name;
        return this;
    }

    public String getDesc() {
        return desc;
    }

    public Person setDesc(String desc) {
        this.desc = desc;
        return this;
    }
}
/**
 *
 */
@Slf4j
//@RunWith(SpringRunner.class)
//@SpringBootTest
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
@Test
    public void abc1() throws ClassNotFoundException {

       //Class CC = Class.forName("String");//这旬会报错：java.lang.ClassNotFoundException: String，可能没用全名
       // CC c = "ssss";
        Class CC = Class.forName("com.sss.yunweiadmin.Person");//可行

        List<String> list;

        String a = "abc";
        String b = "ab11";
        String[] a11 = {a,b};
        System.out.println(a11[0]);

        IService service = (IService) SpringUtil.getBean("asDeviceCommonServiceImpl");

        Object dbObject = null;
        String columnName = "baomi_no";


        //dbObject = service.getOne(new QueryWrapper<Object>().eq("id", "13"));
        //List<Map<String,Object>> aaa  = service.listMaps(new QueryWrapper<Object>().select(columnName).eq("id", "102002"));
        //20220622以下语句只返回的map只有一个元素：<baomi_no -> SN111122>这样的：可能select只读一行无级：暂不研，
        Map<String,Object> aaa  = service.getMap(new QueryWrapper<Object>().select(columnName));//.eq("id", "102002"));//
        //String bb = String.valueOf(null);
        System.out.println( aaa.get(columnName));
    }



    @Test
    public void testReflect() throws IllegalAccessException {

        System.out.println(ObjectUtil.isNotEmpty(""));
    //    Person a= new Person("john","xxxxxxxxxx");
  //      System.out.println(taskService);
//        Field[] fields = taskService.getClass().getDeclaredFields();
//        for (Field field : fields) {
//            if (!field.isAccessible()) {
//                field.setAccessible(true);
//            }
//            System.out.println(field.getName() + ":" + field.get(taskService));
//        }
    }


    @Test
    public void testJson() {
        //

        String str = "{\"1550\":\"秘密\", \"asset\":[{\"customTableId\":16, \"asId\":102002 },{\"customTableId\":17, \"asId\":102004 }],\"16.计算机信息表.as_device_common.no.75\":\"J0601111\"}";
        JSONObject jsonObject = JSONObject.parseObject(str);
        System.out.println(jsonObject);
        JSONArray jsonAssetArray = jsonObject.getJSONArray("asset");
        Map<String, String> map = new HashMap<>();
        jsonAssetArray.stream().forEach(item -> {
            JSONObject itemJson = (JSONObject) item;//注意：关于强转“直接用后半句(JSONObject)item.getString”是不行的，可能因为那个括号最后执行吧，也不是：todo记录
            System.out.println(item);
            //{16=102002, 17=102004}:用于遍历每个自定义表字段时查找对象资产id(来进一步查询资产类型)
            map.put(itemJson.getString("customTableId"), itemJson.getString("asId"));
        });
        System.out.println(map);

    }


    public Integer bbb() {
        return null;//null不能返回给int,但可以是Integer
    }

    @Test//@Test注解修饰的方法只能是void
    public void abc() {
       // return;

        String a = "abc";
        String b = "ab11";
        String[] a11 = {a, b};
        String d = "";
        // System.out.println(a11[0]);
        Integer c = this.bbb();
        System.out.println(ObjectUtil.isNotEmpty(d));
        String [] abc ="".split(",");
        System.out.println(abc[0]);

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