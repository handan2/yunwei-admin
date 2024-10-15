package com.sss.yunweiadmin.controller;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sss.yunweiadmin.common.result.ResponseResultWrapper;
import com.sss.yunweiadmin.model.entity.ProcessDefinitionTask;
import com.sss.yunweiadmin.model.entity.ProcessInstanceNode;
import com.sss.yunweiadmin.model.entity.Score;
import com.sss.yunweiadmin.model.entity.SysUser;
import com.sss.yunweiadmin.model.vo.ScoreNodeVO;
import com.sss.yunweiadmin.service.ProcessDefinitionTaskService;
import com.sss.yunweiadmin.service.ProcessInstanceNodeService;
import com.sss.yunweiadmin.service.ScoreService;
import com.sun.corba.se.spi.orbutil.threadpool.WorkQueue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author 任勇林
 * @since 2021-10-28
 */
@RestController
@RequestMapping("/score")
@ResponseResultWrapper
public class ScoreController {
    @Autowired
    ProcessInstanceNodeService processInstanceNodeService;
    @Autowired
    ProcessDefinitionTaskService processDefinitionTaskService;
    @Autowired
    ScoreService scoreService;
    @Autowired
    HttpSession httpSession;
    @GetMapping("test")
    public ProcessInstanceNode test(String id) {
        ProcessInstanceNode node = processInstanceNodeService.getOne(new QueryWrapper<ProcessInstanceNode>().eq("process_instance_data_id", id),false);
        return node;
    }

    @GetMapping("getNode")
    public List<ProcessInstanceNode> getNode(String id) {
        System.out.println("aaa");
        List<ProcessInstanceNode> node = processInstanceNodeService.list(new QueryWrapper<ProcessInstanceNode>().select("display_name").eq("process_instance_data_id", id));
        node.forEach(System.out::println);
        return node;
    }
    @GetMapping("isScored")
    public int isScored(String id){
        return scoreService.count(new QueryWrapper<Score>().eq("business_id",id));

    }
    @PostMapping("saveScore")
    public boolean saveScore(@RequestBody List<Score> score) {
        //scoreService.save(score);
        SysUser user = (SysUser) httpSession.getAttribute("user");
        if (user == null) {
            throw new RuntimeException("用户未登录或登陆超时，请关闭本页面，重新从登陆入口进入");
        }
        System.out.println(score);
        scoreService.saveBatch(score);
        return true;

    }





    @GetMapping("getScoreObject")
    public ScoreNodeVO getScoreObject(String id) {
        //20221217 改造节点类型的判断依据：从task表中读“节点类型”&&而不是之前根据“节点名称”：待后续验证逻辑准确性
        //整个DB中的takeDefKey
        List<Map<String, Object>> handleTaskKeysListMap = processDefinitionTaskService.listMaps(new QueryWrapper<ProcessDefinitionTask>().eq("task_type","bpmn:handleTask").select("task_def_key"));
        List<Map<String, Object>> approvalTaskKeysListMap = processDefinitionTaskService.listMaps(new QueryWrapper<ProcessDefinitionTask>().eq("task_type","bpmn:approvalTask").select("task_def_key"));
        List<String> handleTaskKeysList = handleTaskKeysListMap.stream().map(item -> (String) item.get("task_def_key")).collect(Collectors.toList());
        List<String> approvalTaskKeysList = approvalTaskKeysListMap.stream().map(item -> (String) item.get("task_def_key")).collect(Collectors.toList());

//        List<Map<String, Object>> operatorNameMapList = processInstanceNodeService.listMaps(new QueryWrapper<ProcessInstanceNode>().select("Distinct display_name,login_name").eq("process_instance_data_id", id).like("task_name", "处理"));
//        List<Map<String, Object>> approvalNodeNameMapList = processInstanceNodeService.listMaps(new QueryWrapper<ProcessInstanceNode>().select("Distinct task_name").eq("process_instance_data_id", id).and(aa -> aa.notLike("task_name", "处理")).and(aa -> aa.notLike("task_name", "发起")));
        //本流程实例中的处理者（含审批人）与节点（含处理环节）；这里的operator包含审批人：
        List<Map<String, Object>> operatorNameMapList = processInstanceNodeService.listMaps(new QueryWrapper<ProcessInstanceNode>().select("Distinct task_name,display_name,login_name,task_def_key").eq("process_instance_data_id", id));
        List<Map<String, Object>> nodeNameMapList = processInstanceNodeService.listMaps(new QueryWrapper<ProcessInstanceNode>().select("Distinct task_name,task_def_key").eq("process_instance_data_id", id));
        //过滤出处理者（不含审批人）与节点（不含处理节点）
        List<Map<String, Object>> operatorNameMapList2 = operatorNameMapList.stream().filter(item->handleTaskKeysList.contains(item.get("task_def_key"))).collect(Collectors.toList());
        List<Map<String, Object>> nodeNameMapList2 = nodeNameMapList.stream().filter(item->approvalTaskKeysList.contains(item.get("task_def_key"))).collect(Collectors.toList());
        ScoreNodeVO scoreNodeVO = new ScoreNodeVO();

        //List<Map<String, Object>> operatorNameMapList2 = operatorNameMapList.stream().filter(item->)

        List<String> operatorNameList =  operatorNameMapList2.stream().map(item ->(String) item.get("task_name") + "(" + (String) item.get("display_name") + ")").collect(Collectors.toList());
        List<String> nodeNameList = nodeNameMapList2.stream().map(item -> (String) item.get("task_name")).collect(Collectors.toList());

//        List<String> operatorNameList1 =  operatorNameList.stream().filter(item->handleTaskKeysList.contains(item)).collect(Collectors.toList());
//        List<String> approvalNodeNameList1 =  nodeNameList.stream().filter(item->approvalTaskKeysList.contains(item)).collect(Collectors.toList());
        scoreNodeVO.setOperatorNameList(operatorNameList);
        scoreNodeVO.setNodeNameList(nodeNameList);
      //  nodeNameList.forEach(System.out::println);

        //System.out.println(scoreNodeVO);
        return scoreNodeVO;

    }
}
