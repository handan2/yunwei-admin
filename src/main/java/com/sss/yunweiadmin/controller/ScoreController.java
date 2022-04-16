package com.sss.yunweiadmin.controller;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sss.yunweiadmin.common.result.ResponseResultWrapper;
import com.sss.yunweiadmin.model.entity.ProcessInstanceNode;
import com.sss.yunweiadmin.model.entity.Score;
import com.sss.yunweiadmin.model.entity.SysUser;
import com.sss.yunweiadmin.model.vo.ScoreNodeVO;
import com.sss.yunweiadmin.service.ProcessInstanceNodeService;
import com.sss.yunweiadmin.service.ScoreService;
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
            throw new RuntimeException("用户未登录");
        }
        System.out.println(score);
        scoreService.saveBatch(score);
        return true;

    }





    @GetMapping("getScoreObject")
    public ScoreNodeVO getScoreObject(String id) {
        List<Map<String, Object>> operatorNameMapList = processInstanceNodeService.listMaps(new QueryWrapper<ProcessInstanceNode>().select("Distinct display_name,login_name").eq("process_instance_data_id", id).like("task_name", "处理"));
        List<Map<String, Object>> nodeNameMapList = processInstanceNodeService.listMaps(new QueryWrapper<ProcessInstanceNode>().select("Distinct task_name").eq("process_instance_data_id", id).and(aa -> aa.notLike("task_name", "处理")).and(aa -> aa.notLike("task_name", "发起")));
        System.out.println(operatorNameMapList);
        System.out.println( nodeNameMapList);
        ScoreNodeVO scoreNodeVO = new ScoreNodeVO();

        List<String> operatorNameList = operatorNameMapList.stream().map(item -> (String) item.get("display_name")).collect(Collectors.toList());
        List<String> nodeNameList = nodeNameMapList.stream().map(item -> (String) item.get("task_name")).collect(Collectors.toList());
        scoreNodeVO.setOperatorNameList(operatorNameList);
        scoreNodeVO.setNodeNameList(nodeNameList);
      //  nodeNameList.forEach(System.out::println);

        //System.out.println(scoreNodeVO);
        return scoreNodeVO;

    }
}
