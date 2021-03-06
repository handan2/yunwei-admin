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

       //Class CC = Class.forName("String");//??????????????????java.lang.ClassNotFoundException: String?????????????????????
       // CC c = "ssss";
        Class CC = Class.forName("com.sss.yunweiadmin.Person");//??????

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
        //20220622????????????????????????map?????????????????????<baomi_no -> SN111122>??????????????????select?????????????????????????????????
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

        String str = "{\"1550\":\"??????\", \"asset\":[{\"customTableId\":16, \"asId\":102002 },{\"customTableId\":17, \"asId\":102004 }],\"16.??????????????????.as_device_common.no.75\":\"J0601111\"}";
        JSONObject jsonObject = JSONObject.parseObject(str);
        System.out.println(jsonObject);
        JSONArray jsonAssetArray = jsonObject.getJSONArray("asset");
        Map<String, String> map = new HashMap<>();
        jsonAssetArray.stream().forEach(item -> {
            JSONObject itemJson = (JSONObject) item;//??????????????????????????????????????????(JSONObject)item.getString????????????????????????????????????????????????????????????????????????todo??????
            System.out.println(item);
            //{16=102002, 17=102004}:?????????????????????????????????????????????????????????id(??????????????????????????????)
            map.put(itemJson.getString("customTableId"), itemJson.getString("asId"));
        });
        System.out.println(map);

    }


    public Integer bbb() {
        return null;//null???????????????int,????????????Integer
    }

    @Test//@Test??????????????????????????????void
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

    //20210711 ????????????????????????
    @Test
    public void startProcess1() {
        //?????????????????? 20210201??????????????????.deploymentId("ProcessForMini")?????????????????????????????????????????????max of 1????????????????????????????????????
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
     * ??????????????????,act7 api
     *
     */
//    @Test
//    public void testStartProcess(){
//        String userName = "zhangsan";
//        //securityUtil.logInAs("salaboy");
//        ProcessInstance myGroup = processRuntime.start(ProcessPayloadBuilder
//                .start()
//                .withProcessDefinitionKey("MyProcess")
////                .withVariable("username",userName) ??????????????????
//                .build());
//
//        log.info("????????????ID ={}",myGroup.getId());
//    }
//
//    /**
//     * ????????????????????????
//     */
//    @Test
//    public void testTask(){
//        securityUtil.logInAs("ryandawsonuk");
//        Page<Task> tasks = taskRuntime.tasks(Pageable.of(0, 10));
//        if (tasks.getTotalItems() > 0){
//            for (Task task: tasks.getContent()) {
//                //????????????
//                taskRuntime.claim(TaskPayloadBuilder.claim().withTaskId(task.getId()).build());
//                log.info("????????????={}",task);
//                //????????????
//                taskRuntime.complete(TaskPayloadBuilder.complete().withTaskId(task.getId()).build());
//            }
//        }
//        tasks = taskRuntime.tasks(Pageable.of(0, 10));
//        if (tasks.getTotalItems() > 0){
//            for (Task task: tasks.getContent()) {
//                log.info("??????={}",task);
//            }
//        }
//    }


}