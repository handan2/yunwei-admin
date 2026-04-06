package com.sss.yunweiadmin.controller;


import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.XmlUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.extra.servlet.ServletUtil;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelReader;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.read.metadata.ReadSheet;
import com.alibaba.excel.support.ExcelTypeEnum;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.common.base.Strings;
import com.sss.yunweiadmin.common.config.GlobalParam;
import com.sss.yunweiadmin.common.operate.OperateLog;
import com.sss.yunweiadmin.common.result.ResponseResult;
import com.sss.yunweiadmin.common.result.ResponseResultWrapper;
import com.sss.yunweiadmin.common.utils.TreeUtil;
import com.sss.yunweiadmin.model.entity.*;
import com.sss.yunweiadmin.model.excel.ExcelListener;
import com.sss.yunweiadmin.model.excel.SysUserExcel;
import com.sss.yunweiadmin.model.vo.*;
import com.sss.yunweiadmin.service.*;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.axis.client.Call;
import org.apache.axis.client.Service;
import org.apache.axis.encoding.XMLType;
import org.w3c.dom.Document;

import javax.xml.rpc.ParameterMode;
import javax.xml.xpath.XPathConstants;

/**
 * <p>
 * 用户表 前端控制器
 * </p>
 *
 * @author 任勇林
 * @since 2021-04-09
 */
//@RestController
@Slf4j
@Controller
@RequestMapping("/sysUser")
//@ResponseResultWrapper
public class SysUserController {
    @Autowired
    SysUserService sysUserService;
    @Autowired
    SysRoleUserService sysRoleUserService;
    @Autowired
    SysRolePermissionService sysRolePermissionService;
    @Autowired
    SysDeptService sysDeptService;
    @Autowired
    SysRoleService sysRoleService;
    @Autowired
    SysPermissionService sysPermissionService;
    @Autowired
    HttpSession httpSession;
    @Autowired
    ProcessDefinitionService processDefinitionService;
    @Autowired
    ProcessInstanceDataService processInstanceDataService;
    @Autowired
    private Environment environment;
    @Autowired
    OperateeLogService operateeLogService;
    @Autowired
    AsDeviceCommonService asDeviceCommonService;
    @Autowired
    SysDicService sysDicService;
    @Autowired
    PersonService personService;

    //20231205 专用于手工记日志：用户相关
    void saveLog(String mode, String userInfo, Map<String, String> map){
        SysUser user = (SysUser) httpSession.getAttribute("user");
        OperateeLog operateeLog = new OperateeLog();
        String paramStr = "";
        if("edit".equals(mode)){
            paramStr =  "编辑用户【"+ userInfo + "】信息";
            if(ObjectUtil.isNotEmpty(map)){
                if(map.containsKey("miji_change"))
                    paramStr  +=":" + map.get("miji_change") + ";";
                if(map.containsKey("dept_change"))
                    paramStr  +=":" + map.get("dept_change") + ";";
                if(map.containsKey("loginName_change"))
                    paramStr  +=":" + map.get("loginName_change") + ";";
                if(map.containsKey("displayName_change"))
                    paramStr  +=":" + map.get("displayName_change") + ";";
            }
            operateeLog.setOperateType("编辑用户");
            operateeLog.setMethod("com.sss.yunweiadmin.controller.edit()");
        } else if("delete".equals(mode)){
            paramStr =  "删除用户【"+ userInfo + "】信息";
            operateeLog.setOperateType("删除用户");
            operateeLog.setMethod("com.sss.yunweiadmin.controller.delete()");
        } else if("add".equals(mode)){
            paramStr =  "新增用户【"+ userInfo + "】信息";
            operateeLog.setOperateType("新增用户");
            operateeLog.setMethod("com.sss.yunweiadmin.controller.add()");
        }

//        if(CollUtil.isNotEmpty(roleNameList)) {
//            paramStr = paramStr +  String.join(",", roleNameList);
//        }
        operateeLog.setParam(paramStr);
        operateeLog.setOperateModule("用户模块");

        operateeLog.setLoginName(user.getLoginName());
        operateeLog.setDisplayName(user.getDisplayName());
        //operateeLog.setIp(ServletUtil.getClientIP(request));
        String ip = (String) httpSession.getAttribute("IP");
        operateeLog.setIp(ip);

        operateeLog.setCreateDatetime(LocalDateTime.now());
        operateeLogService.save(operateeLog);

    }

    @ResponseBody
    @ResponseResultWrapper
    @PostMapping("getUsers")
    public  GetUsersVO getUsers(@RequestBody GetUsersParamVO request){
        System.out.println("--------------getUsers---------------");
        Integer currentPage = request.getCurrentPage() != null ? request.getCurrentPage() : 1;
        Integer pageSize = request.getPageSize() != null ? request.getPageSize() : 10;

        String token = request.getToken();
        String updateDateTime = request.getUpdateDateTime();
        String appNo = request.getAppNo();
        if(!"D8D65781E134DB327FBCCEDC5F6A69CA".equals(token))
            throw new RuntimeException("token错误");



        GetUsersVO getUsersVO = new GetUsersVO();


        // --- 创建第一个用户对象 (张三) ---
        UserForAppVO user1 = new UserForAppVO();
        user1.setAppNo("YYXT014");
        user1.setIdNumber("13042619991010136");
        user1.setUserCode("030233333");
        user1.setOrgCode("045233333");
        user1.setDisplayName("张三");
        user1.setLoginName("zhangsan");
        user1.setGender("男");
        user1.setSecretDegree("一般");
        user1.setIdentity("正式");
        user1.setPosition("正主任");
        user1.setRoleName("部门正职|保密主管");
        user1.setUpdateDateTime("2024-12-02 17:18:38");
        user1.setUpdateDateType("新增");

        // --- 创建第二个用户对象 (第二个JSON元素) ---
        // 注意：原始JSON中缺少了 displayName, loginName, gender，所以这里我们不设置它们，它们的值将是 null
        UserForAppVO user2 = new UserForAppVO();
        user2.setAppNo("YYXT014");
        user2.setIdNumber("13054545491010136");
        user2.setUserCode("030233533");
        user2.setOrgCode("04543333");
        user2.setDisplayName("李四"); // 不设置，保持为 null
        user2.setLoginName("lisi");   // 不设置，保持为 null
        user2.setGender("女");      // 不设置，保持为 null
        user2.setSecretDegree("一般");
        user2.setIdentity("正式");
        user2.setPosition("一般员工");
        user2.setRoleName("部门正职|保密主管");
        user2.setUpdateDateTime("2024-12-05 17:24:38");
        user2.setUpdateDateType("修改");

        List<UserForAppVO> userForAppVOList = new ArrayList<UserForAppVO>();
        userForAppVOList.add(user1);
        userForAppVOList.add(user2);

        getUsersVO.setDataList(userForAppVOList);
        getUsersVO.setTotal(2);
        getUsersVO.setCurrentPage(1);
        getUsersVO.setCurrentPage(1);
        return getUsersVO;

       // return ResponseResult.success("D8D65781E134DB327FBCCEDC5F6A69CA");
    }


    @ResponseBody
    @ResponseResultWrapper
    @PostMapping("updateUsers")
    public ResponseResult updateUsers(@RequestBody SysUser user){

        if(!"D8D65781E134DB327FBCCEDC5F6A69CA".equals(user.getToken()))
            throw new RuntimeException("token错误");



        return ResponseResult.success("更新成功");
    }


    @ResponseBody
    @ResponseResultWrapper
    @GetMapping("getToken")
    public Map getToken(){
        Map<String, String> abc = new HashMap<>();
        abc.put("token","D8D65781E134DB327FBCCEDC5F6A69CA");return abc;// ResponseResult.success("D8D65781E134DB327FBCCEDC5F6A69CA");
    }

    @ResponseBody
    @ResponseResultWrapper
    @GetMapping("userList")
    public List<SysUserForInterFace> userList(){
        //QueryWrapper<SysUserForInterFace> queryWrapper1 = new  QueryWrapper<SysUserForInterFace>().eq("org_id",GlobalParam.orgId);
        QueryWrapper<SysUser> queryWrapper = new  QueryWrapper<SysUser>().eq("org_id",GlobalParam.orgId).eq(" status",  "正常").in("secret_degree", Arrays.asList(new String[]{"一般", "重要", "核心"}));;
        List<SysUser> sysUserList = sysUserService.list(queryWrapper);
        //查询部门
        List<SysDept> deptList = sysDeptService.list(new  QueryWrapper<SysDept>().eq("org_id",GlobalParam.orgId));
        Map<Integer, String> deptMap = deptList.stream().collect(Collectors.toMap(SysDept::getId, SysDept::getName));

        List<SysUserForInterFace> sysUserForInterFaceList = sysUserList.stream().map(item-> {
            SysUserForInterFace sysUserForInterFace = new SysUserForInterFace();
            BeanUtil.copyProperties(item,sysUserForInterFace);
            sysUserForInterFace.setDeptName(deptMap.get(item.getDeptId()));
            return  sysUserForInterFace;}).collect(Collectors.toList());
        return sysUserForInterFaceList;
    }


    @ResponseBody
    @ResponseResultWrapper
    @GetMapping("list")
    public IPage<SysUser> list(int currentPage, int pageSize, String loginName, String displayName, Integer deptId, String status) {
        QueryWrapper<SysUser> queryWrapper = new  QueryWrapper<SysUser>().eq("org_id",GlobalParam.orgId);
        if (ObjectUtil.isNotEmpty(loginName)) {
            if(loginName.contains("_")) {//20250121 新用户入网的前端参数标记
                String[] a = loginName.split("_");
                queryWrapper.eq("login_name", a[1]);
            } else
                queryWrapper.like("login_name", loginName);
        }
        if (ObjectUtil.isNotEmpty(displayName)) {
            queryWrapper.like("display_name", displayName);
        }
        if (deptId != null) {
            queryWrapper.eq("dept_id", deptId);
        }
        if (ObjectUtil.isNotEmpty(status)) {
            queryWrapper.eq(" status",  status);
        }
        //queryWrapper.eq("status","正常");
        queryWrapper.notIn("login_name", Arrays.asList(new String[]{"admin_yw", "system_yw", "audit_yw"}));//20231129为测评
        queryWrapper.orderByDesc("id");
        queryWrapper.orderByDesc("dept_id");
        //20211116
        IPage<SysUser> page = sysUserService.page(new Page<>(currentPage, pageSize), queryWrapper);
        page.getRecords().forEach(item -> item.setTemp(sysDeptService.getById(ObjectUtil.isNotEmpty(item.getDeptId()) ? item.getDeptId() : 2).getName()));
        return page;
    }

    @ResponseBody
    @ResponseResultWrapper
    @GetMapping("userRoleGiveList")
    //因为noForm/List对参数进行了类型转化：数组变成了（非json格式的）字符串，所以这里暂用 roleIdList接收Str类型 && 在函数体内再转化下
    public IPage<SysUser> userRoleGiveList(int currentPage, int pageSize, String loginName, String displayName, Integer deptId, String roleIdList) {
        QueryWrapper<SysUser> queryWrapper = new  QueryWrapper<SysUser>().eq("org_id",GlobalParam.orgId).eq("status","正常");//20251210 eq("status","正常")
        queryWrapper.notIn("login_name", Arrays.asList(new String[]{"admin_yw", "system_yw", "audit_yw"}));//20231129为测评
        queryWrapper.in("secret_degree", Arrays.asList(new String[]{"一般", "重要", "核心"}));
        String[] roleStrArr = null;
        List<Integer> roleIdList0 = null;
        if (!Strings.isNullOrEmpty(roleIdList)) {
//            roleIdList.replace("[","");
//            roleIdList.replace("]","");
            roleStrArr = roleIdList.replace("[", "").replace("]", "").split(",");
            List<String> roleIdStrList = Stream.of(roleStrArr).collect(Collectors.toList());
            roleIdList0 = roleIdStrList.stream().map(item -> Integer.valueOf(item)).collect(Collectors.toList());
            //初筛后的用户
            List<Map<String, Object>> userIdMapList = sysRoleUserService.listMaps(new  QueryWrapper<SysRoleUser>().eq("org_id",GlobalParam.orgId).in("role_id", roleIdList0).select("user_id"));
            List<Integer> userIdList = userIdMapList.stream().map(item -> Integer.parseInt(item.get("user_id").toString())).collect(Collectors.toList());
            List<Integer> selectedRoleIdList = roleIdList0;//下面那个“ finalRoleIdList.forEach()”提示"lambda表达式中的变量应为最终变量或为有效的最终变量"而使用自动修改模式自动加的变量：暂不研
            List<Integer> userIdListFiltered = userIdList.stream().filter(item -> {
                List<SysRoleUser> roleUserList = sysRoleUserService.list(new  QueryWrapper<SysRoleUser>().eq("org_id", GlobalParam.orgId).eq("user_id", item));
                if (CollUtil.isEmpty(roleUserList)) {
                    throw new RuntimeException("用户ID为" + item + "的用户没有分配角色");
                }
                List<Integer> roleIdListForIndividual = roleUserList.stream().map(SysRoleUser::getRoleId).collect(Collectors.toList());
                boolean hasAllSelectedRole = true;
                if (CollUtil.isNotEmpty(roleUserList)) {
                    for (Integer selectedRoleId : selectedRoleIdList) {
                        if (!roleIdListForIndividual.contains(selectedRoleId))
                            hasAllSelectedRole = false;
                    }
                    ;
                }
                return hasAllSelectedRole;
            }).collect(Collectors.toList());
            ;

            if (CollUtil.isNotEmpty(userIdListFiltered)) {
                queryWrapper.in("id", userIdListFiltered);
            } else
                queryWrapper.eq("id", -1);//角色查不到用户，直接让“他查询结果为空”
        }
        if (!Strings.isNullOrEmpty(loginName)) {
            queryWrapper.like("login_name", loginName);
        }
        if (!Strings.isNullOrEmpty(displayName)) {
            queryWrapper.like("display_name", displayName);
        }
        if (deptId != null) {
            queryWrapper.eq("dept_id", deptId);
        }
        queryWrapper.orderByDesc("id");
        queryWrapper.orderByDesc("dept_id");


        //20211116
        IPage<SysUser> page = sysUserService.page(new Page<>(currentPage, pageSize), queryWrapper);
        page.getRecords().forEach(item -> {
                item.setTemp(sysDeptService.getById(ObjectUtil.isNotEmpty(item.getDeptId()) ? item.getDeptId() : GlobalParam.depSubRootID).getName());
                //根据用户获取角色
                List<SysRoleUser> roleUserList = sysRoleUserService.list(new  QueryWrapper<SysRoleUser>().eq("org_id",GlobalParam.orgId).eq("user_id", item.getId()));
                if (CollUtil.isEmpty(roleUserList)) {
                    throw new RuntimeException(item.getLoginName() + "用户没有分配角色");
                }
                List<Integer> roleIdList2 = roleUserList.stream().map(SysRoleUser::getRoleId).collect(Collectors.toList());
                List<SysRole> roleList = sysRoleService.list(new  QueryWrapper<SysRole>().eq("org_id",GlobalParam.orgId).in("id", roleIdList2));
                String roleNameStr = roleList.stream().map(SysRole::getName).collect(Collectors.joining(","));
                item.setRoleNameStr(roleNameStr);
            }
        );
        return page;
    }

    @ResponseBody
//    @OperateLog(module = "用户模块", type = "添加用户")
    @ResponseResultWrapper
    @PostMapping("add")
    public int add(@RequestBody SysUser sysUser) {//20220820返回值类型改成int:返回新增用户的ID：为了前端在手工添加代理人后需要获得用户ID(以拼成committer_str)
        saveLog("add", sysUser.getDisplayName(),null);
        return sysUserService.add(sysUser);
    }


    @ResponseBody
//    @OperateLog(module = "用户模块", type = "编辑用户")
    @PostMapping("edit")
    @ResponseResultWrapper
    public boolean edit(@RequestBody SysUser sysUser) {
        System.out.println(sysUser);
        //找出人员密级变更的情况
        SysUser sysUser_ori = sysUserService.getById(sysUser.getId());
        Map<String, String> map = new HashMap<>();
        if(!sysUser_ori.getSecretDegree().equals(sysUser.getSecretDegree())){
            map.put("miji_change","由"+ sysUser_ori.getSecretDegree() + "变为" + sysUser.getSecretDegree());
        }
        if(!sysUser_ori.getLoginName().equals(sysUser.getLoginName())){
            map.put("loginName_change","由"+ sysUser_ori.getLoginName() + "变为" + sysUser.getLoginName());
        }
        if(!sysUser_ori.getDisplayName().equals(sysUser.getDisplayName())){
            map.put("displayName_change","由"+ sysUser_ori.getDisplayName() + "变为" + sysUser.getDisplayName());
        }
        if(!sysUser_ori.getDeptId().equals(sysUser.getDeptId())){
            SysDept sysDept_ori = sysDeptService.getById(sysUser_ori.getDeptId());
            SysDept sysDept = sysDeptService.getById(sysUser.getDeptId());
            String deptName_ori  = "";
            if(ObjectUtil.isNotEmpty(sysDept_ori))
                deptName_ori = sysDept_ori.getName();
            map.put("dept_change","由"+  deptName_ori + "变为" + sysDept.getName());
        }
        saveLog("edit",sysUser.getDisplayName(),map);
        return sysUserService.updateById(sysUser);
    }

    @ResponseBody
    @OperateLog(module = "用户模块", type = "编辑用户")
    @GetMapping("upateByIdentity")
    @ResponseResultWrapper
    public boolean upateByIdentity(String identity, String loginName, Integer crossOrgId) {
        System.out.println("sysUser");
        return sysUserService.upateByIdentity(identity, loginName, crossOrgId);
    }

    @ResponseBody
    @GetMapping("get")
    @ResponseResultWrapper
    public SysUser getById(String id) {
        return sysUserService.getById(id);
    }

//    @OperateLog(module = "用户模块", type = "删除用户")
    @ResponseBody
    @GetMapping("delete")
    @ResponseResultWrapper
    public boolean delete(Integer[] idArr) {

        List<Map<String, Object>> listMaps1 =  sysUserService.listMaps(new  QueryWrapper<SysUser>().eq("org_id",GlobalParam.orgId).in("id", Arrays.asList(idArr)).select("display_name"));
        List<String> displayNameList = listMaps1.stream().map(item -> item.get("display_name").toString()).collect(Collectors.toList());
        saveLog("delete", String.join(",", displayNameList),null);
        return sysUserService.delete(idArr);
    }

    //20211128张强加，我先不封装，我的一些东东略有变化
    @ResponseBody
    @ResponseResultWrapper
    private UserVO getUserVO(SysUser user) {
        UserVO userVO = new UserVO();
        //根据用户获取角色
        List<SysRoleUser> roleUserList = sysRoleUserService.list(new  QueryWrapper<SysRoleUser>().eq("org_id",GlobalParam.orgId).eq("user_id", user.getId()));
        if (CollUtil.isEmpty(roleUserList)) {
            throw new RuntimeException("用户没有分配角色");
        }
        List<Integer> roleIdList = roleUserList.stream().map(SysRoleUser::getRoleId).collect(Collectors.toList());
        //根据角色获取权限
        List<SysRolePermission> rolePermissionList = sysRolePermissionService.list(new  QueryWrapper<SysRolePermission>().eq("org_id",GlobalParam.orgId).in("role_id", roleIdList));
        if (CollUtil.isEmpty(rolePermissionList)) {
            throw new RuntimeException("用户没有分配菜单");
        }
        //根据权限获取完整的权限

        List<Integer> permissionIdList = rolePermissionList.stream().map(SysRolePermission::getPermissionId).collect(Collectors.toList());
        List<SysPermission> permissionList = sysPermissionService.list(new  QueryWrapper<SysPermission>().eq("org_id",GlobalParam.orgId).in("id", permissionIdList));
        List<SysPermission> allPermissionList = new ArrayList<SysPermission>();
        while (true) {
            if (ObjectUtil.isEmpty(permissionList)) {
                break;
            }
            allPermissionList.addAll(permissionList);
            //
            List<Integer> parentPermissionIdList = permissionList.stream().map(SysPermission::getPid).collect(Collectors.toList());

            permissionList = sysPermissionService.list(new  QueryWrapper<SysPermission>().eq("org_id",GlobalParam.orgId).in("id", parentPermissionIdList));
        }
        //20241121  permissionList经以上处理后（处理结果暂不研），其值并没有在下方应用 ，此变量可考虑删除
        List<Integer> allPermissionIdList = allPermissionList.stream().map(SysPermission::getId).collect(Collectors.toList());
        //20251210测评加.ne("remark","不可见")
        List<SysPermission> permissionList1 = sysPermissionService.list(new  QueryWrapper<SysPermission>().eq("org_id",GlobalParam.orgId).ne("remark","不可见").in("id", allPermissionIdList).orderByAsc("sort"));
        List<SysPermission> permissionList2 = sysPermissionService.list(new  QueryWrapper<SysPermission>().eq("org_id",GlobalParam.orgId).ne("remark","不可见").in("id", allPermissionIdList).orderByAsc("sort"));
        List<SysPermission> permissionList3 = sysPermissionService.list(new  QueryWrapper<SysPermission>().eq("org_id",GlobalParam.orgId).ne("remark","不可见").in("id", allPermissionIdList).orderByAsc("sort"));
        List<SysPermission> permissionList4 = sysPermissionService.list(new  QueryWrapper<SysPermission>().eq("org_id",GlobalParam.orgId).ne("remark","不可见").in("id", allPermissionIdList).orderByAsc("sort"));
        //导航菜单
        List<SysPermission> menuList = TreeUtil.getTreeSelect(permissionList1.stream().filter(item -> item.getType().equals("菜单") || item.getType().equals("叶子菜单")).collect(Collectors.toList()));
        //操作按钮-按钮组
        Map<String, List<SysPermission>> operateButtonMap = new HashMap<>();
        List<SysPermission> operateButtonList = TreeUtil.getTreeSelect(permissionList2.stream().filter(item -> item.getType().equals("叶子菜单") || "按钮组".equals(item.getPosition())).collect(Collectors.toList()));
        for (SysPermission sysPermission : operateButtonList) {
            if (ObjectUtil.isNotEmpty(sysPermission.getChildren())) {
                operateButtonMap.put(sysPermission.getPath(), sysPermission.getChildren());
            }
        }
        //数据列表-按钮
        Map<String, List<SysPermission>> dataListButtonMap = new HashMap<>();
        List<SysPermission> dataListButtonList = TreeUtil.getTreeSelect(permissionList3.stream().filter(item -> item.getType().equals("叶子菜单") || ("数据列表".equals(item.getPosition()) && !item.getPermissionType().equals("startProcess"))).collect(Collectors.toList()));
        for (SysPermission sysPermission : dataListButtonList) {
            if (ObjectUtil.isNotEmpty(sysPermission.getChildren())) {
                dataListButtonMap.put(sysPermission.getPath(), sysPermission.getChildren());
            }
        }

        //数据列表-发起流程按钮
        Map<Integer, SysPermission> startProcessButtonMap = new HashMap<>();
        SysPermission permission = sysPermissionService.getOne(new  QueryWrapper<SysPermission>().eq("org_id",GlobalParam.orgId).eq("permission_type", "startProcess"));
        /*
            1.根据用户取出角色
            2.根据角色获取流程定义
         */
        //List<SysRole> roleList = sysRoleService.listByIds(roleIdList);
        //List<String> ProcessRoleIdList = roleList.stream().map((SysRole::getId).collect(Collectors.toList());
        List<ProcessDefinition> definitionList = processDefinitionService.list(new  QueryWrapper<ProcessDefinition>().eq("org_id",GlobalParam.orgId));
        for (ProcessDefinition processDefinition : definitionList) {
            List<String> definitionRoleIdList = Stream.of(processDefinition.getRoleId().split(",")).collect(Collectors.toList());
            List<Integer> definitionRoleIdListToInt = definitionRoleIdList.stream().map(value -> Integer.valueOf(value)).collect(Collectors.toList());
            //判断
            definitionRoleIdListToInt.retainAll(roleIdList);//
            if (ObjectUtil.isNotEmpty(definitionRoleIdListToInt)) {
                startProcessButtonMap.put(processDefinition.getId(), permission);
            }
        }
        //查询
        Map<String, SysPermission> queryMap = new HashMap<>();
        List<SysPermission> queryList = TreeUtil.getTreeSelect(permissionList4.stream().filter(item -> item.getType().equals("叶子菜单") || "query".equals(item.getPermissionType())).collect(Collectors.toList()));
        for (SysPermission sysPermission : queryList) {
            if (ObjectUtil.isNotEmpty(sysPermission.getChildren())) {
                queryMap.put(sysPermission.getPath(), sysPermission.getChildren().get(0));
            }
        }
        userVO.setUser(user);
        userVO.setMenuList(menuList);
        userVO.setOperateButtonMap(operateButtonMap);
        userVO.setDataListButtonMap(dataListButtonMap);
        userVO.setStartProcessButtonMap(startProcessButtonMap);
        userVO.setQueryMap(queryMap);
        //userVO.setRoleIdList(roleIdList);//20211113

        return userVO;
    }





    @OperateLog(module = "用户模块", type = "网关登录")//“录”字区别与真正的“网关登陆”
    @GetMapping("login")
    @ResponseResultWrapper
    @ResponseBody
    public UserVO login(String loginName, String password) {

        httpSession.removeAttribute("IP");
        //根据 登录账号 查询出用户
        List<SysUser> userList = sysUserService.list(new  QueryWrapper<SysUser>().eq("org_id",GlobalParam.orgId).eq("login_name", loginName));
        //20260120 测试日志
//        String userRecords = userList.stream()
//                .map(user -> {
//                    // !!! 重要：同样，请替换为真实的 getter 方法 !!!
//                    return user.getId() + "|" + user.getLoginName() + "|" + user.getOrgId();
//                })
//                .collect(Collectors.joining("\n")); // 用换行符连接所有记录
//        // 2. 拼接上固定的第一行，并记录日志
//        log.info("20260120-----------------\n" + userRecords);


        //给user/temp字段放部门name
        userList.forEach(item -> item.setTemp(sysDeptService.getById(ObjectUtil.isNotEmpty(item.getDeptId()) ? item.getDeptId() : GlobalParam.depSubRootID).getName()));
        if (userList.size() != 1) {
            throw new RuntimeException("用户名错误");
        }
        SysUser dbUser = userList.get(0);
        //校验 登录密码
        String dbPassword = dbUser.getPassword();
        String pagePassword = SecureUtil.md5(password);
        if (!dbPassword.equals(pagePassword)) {
            throw new RuntimeException("密码错误");
        }


        //20220907
        List<SysRoleUser> roleUserList = sysRoleUserService.list(new  QueryWrapper<SysRoleUser>().eq("org_id",GlobalParam.orgId).eq("user_id", dbUser.getId()));
        if (CollUtil.isEmpty(roleUserList)) {
            throw new RuntimeException("用户没有分配角色");
        }
        List<Integer> roleIdList = roleUserList.stream().map(SysRoleUser::getRoleId).collect(Collectors.toList());
        dbUser.setRoleIdList(roleIdList);
        List<SysRole> roleList = sysRoleService.list(new  QueryWrapper<SysRole>().eq("org_id",GlobalParam.orgId).in("id", roleIdList));
        List<String> roleNameList = roleList.stream().map(SysRole::getName).collect(Collectors.toList());
        dbUser.setRoleNameList(roleNameList);


        //20211128获取角色极权限代码封装成一个私有函数getUserVO
        UserVO userVO = getUserVO(dbUser);
        //20211113dbUser里添加了无关联Table的roleIdList
        // dbUser.setRoleIdList(userVO.getRoleIdList());
        //20211120防止重复登陆
        httpSession.removeAttribute("user");//todo测试
        httpSession.setAttribute("user", dbUser);
        return userVO;
    }
    //20230804 网关登陆
    @SneakyThrows
    @GetMapping("/netgateLogin")
    public String netgateLogin(HttpServletRequest request){
        System.out.println("aaaaaaaaaaaaaaa");
        System.out.println(request.getHeader("dnname"));
        String dnName  = new String(request.getHeader("dnname").getBytes("ISO8859-1"),"UTF-8");
       //String dnName = UriUtils.decode(request.getHeader("dnname"), "UTF-8");
        String clientip = request.getHeader("clientip");
        System.out.println("-----clientip----");
        System.out.println(clientip);
        httpSession.setAttribute("IP", clientip);///20231129
        int start = dnName.indexOf("T=")+2;
        int end = dnName.indexOf(",", start);
        String tValue = dnName.substring(start, end);
       // String tValue = "130426198409130136";
        this.setSessionForPIDForNetgateLogin(tValue);
        return "index";
    }
//    @GetMapping ("/ssoLogin")//20220322正式与智企集成时要改成post
//    public String ssoLogin2(){
//        return "index";//返回字符串|视图名，也会被登陆拦截
//    }



    @ResponseBody//20260119 不能省略
    @GetMapping("/getRenliUsers")
    @ResponseResultWrapper
    @Scheduled(cron = "0 0 10 * * ?")//每天10点执行
    public ResponseResult getRenliUsers(){
        sysUserService.getRenliUsers();
        return ResponseResult.success("true");
    }


    //单点登陆与智企集成
    @PostMapping("/ssoLogin")//20220322正式与智企集成时要改成post
    public String ssoLogin(HttpServletRequest request) {
        System.out.println("-----ssoLogin23----");
        //单点登录代码
        String userID = request.getParameter("userID");
        String userName = request.getParameter("userName");
        String PID = request.getParameter("PID");
        String sessionID = request.getParameter("sessionID");
        String WSUrl = request.getParameter("WSUrl");
        String verifySSO = request.getParameter("verifySSO");
        String projectDetails = "<?xml version=\"1.0\" encoding=\"GB2312\"?>" +
                "<root>" +
                "<data>" +
                "<sessionID>" + sessionID + "</sessionID>" +
                "<userID>" + userID + "</userID>" +
                "<PID>" + PID + "</PID>" +
                "<verifySSO>" + verifySSO + "</verifySSO>" +
                "</data>" +
                "</root>";
        System.out.println("PID:" + projectDetails);
        String[] param = {"common", "0", "biz.bizCheckSSO", projectDetails};
        for (int i = 0; i < 3; i++) {
            String msg = loginWebService(WSUrl, param);
            if ("1".equals(msg)) {

                //用户放入session
                // SysUser user = sysUserService.getById(19);
//                SysUser user = sysUserService.getOne(new  QueryWrapper<SysUser>().eq("org_id",GlobalParam.orgId).eq("id_number", PID));
//                if (ObjectUtil.isEmpty(user)) {
//                    System.out.println("身份证号不存在！");
//                    return "redirect:/login1";
//                }
//                List<SysRoleUser> roleUserList = sysRoleUserService.list(new  QueryWrapper<SysRoleUser>().eq("org_id",GlobalParam.orgId).eq("user_id", user.getId()));
//                if (ObjectUtil.isEmpty(roleUserList)) {
//                    throw new RuntimeException("用户没有分配角色");
//                }
//                //20211128添加角色ID
//                List<Integer> roleIdList = roleUserList.stream().map(SysRoleUser::getRoleId).collect(Collectors.toList());
//                user.setRoleIdList(roleIdList);
//                List<SysRole> roleList = sysRoleService.list(new  QueryWrapper<SysRole>().eq("org_id",GlobalParam.orgId).in("id", roleIdList));
//                List<String> roleNameList = roleList.stream().map(SysRole::getName).collect(Collectors.toList());
//                user.setRoleNameList(roleNameList);
//                httpSession.removeAttribute("user");
//                httpSession.setAttribute("user", user);
                SysUser sysUser = this.setSessionForPID(PID);
                //                OperateeLog operateeLog = new OperateeLog();
                //                operateeLog.setLoginName(sysUser.getLoginName());
                //                operateeLog.setDisplayName(sysUser.getDisplayName());
                //                operateeLog.setIp(ServletUtil.getClientIP(request));
                //                operateeLog.setMethod("com.sss.yunweiadmin.controller.SysUserController.ssoLogin()");
                //                operateeLog.setCreateDatetime(LocalDateTime.now());
                //                operateeLog.setParam("[PID:" + PID + "]" );
                //                operateeLog.setOperateModule("用户模块");
                //                operateeLog.setOperateType("单点登陆");
                //                operateeLogService.save(operateeLog);
                //
                //                //
                return "index";//注：调用视图解析器展示这个页面文件时，还会把action里的路径（如本例中的/sysuser/ssologin）也“带过去”
            }
        }

        return "redirect:/login1";


    }

    //20220327
    public String loginWebService(String WSUrl, String[] param) {
        System.out.println("come into loginWebService");
        try {
            Service service = new Service();
            Call call = (Call) service.createCall();
            call.setTargetEndpointAddress(WSUrl);
            call.setOperationName("runBiz");//设置操作名
            /*设置入口参数*/
            call.addParameter("packageName", XMLType.XSD_STRING, ParameterMode.IN);
            call.addParameter("unitId", XMLType.XSD_STRING, ParameterMode.IN);
            call.addParameter("processName", XMLType.XSD_STRING, ParameterMode.IN);
            call.addParameter("bizDataXML", XMLType.XSD_STRING, ParameterMode.IN);
            call.setReturnType(XMLType.XSD_STRING);
            Object obj = call.invoke(param);//obj是代表返回一个XML格式的串

//            String xml = "<root><a name = \"第一个元素\"><b>最底层节点值</b></a></root>";
//            String xml1 = "<root><data><msg>1</msg></data></root>";
            Document document = XmlUtil.parseXml((String) obj);
            Object msgString = XmlUtil.getByXPath("//root/data/msg", document, XPathConstants.STRING);
            System.out.println("msgString:" + msgString);
            return (String) msgString;
        } catch (Exception e) {
            System.out.println("=========智慧管理系统登录系统出现异常：" + e.getMessage());
            return "登陆异常！";
          //  return "redirect:/login1";//20230614有时间做一个错误提醒界面
        }

    }


    /*
    用于单点登陆/打印标签/智企集成待办事项点击时： 第一次跳转到前端界面时，由前端页面再发起一个ajax来获取userVO:因为第一次访问action的
    登陆请求是从URL地址栏||别的系统界面上链接发起来的：不能接受我的系统的action返回值；
    20230513因为待办集成时智企新开的标签（相当于新打开浏览器）时并没有将这前页面的session继承（这点和单点登陆时不一样），导致无法用
    intercepter拦截器那种（通过request.getSession）来猎取用户信息；故该session验证应放在此处&&直接用服务器上的session验证
    */
    @GetMapping("/ssoLoginForUserVO")
    @ResponseResultWrapper
    @ResponseBody
//    @OperateLog(module = "用户模块", type = "点击待办")
    public UserVO ssoLoginForPost(HttpServletRequest request) {
        System.out.println("ssoLoginForPost:PID");
        String PID = request.getParameter("PID");
        System.out.println(PID);
        SysUser user = null;
        OperateeLog operateeLog = new OperateeLog();
        if(ObjectUtil.isNotEmpty(PID)) {//这个分支是专用于点击智企待办
            user = this.setSessionForPID(PID);
            operateeLog.setParam("[PID:" + PID + "]" );
            operateeLog.setOperateModule("用户模块");
            operateeLog.setOperateType("单点登陆(待办)");
        } else {
            operateeLog.setOperateModule("用户模块");
            operateeLog.setOperateType("单点登陆");
            user = (SysUser) httpSession.getAttribute("user");
        }
        if (user == null) {
            System.out.println("ssoLoginForPost:用户未登录或登陆超时，请关闭本页面，重新从登陆入口进入");
            throw new RuntimeException("用户未登录或登陆超时，请关闭本页面，重新从登陆入口进入");
        }
        operateeLog.setLoginName(user.getLoginName());
        operateeLog.setDisplayName(user.getDisplayName());
        //operateeLog.setIp(ServletUtil.getClientIP(request));
        String ip = (String) httpSession.getAttribute("IP");
        if(ObjectUtil.isEmpty(ip))
            ip = ServletUtil.getClientIP(request);
        httpSession.setAttribute("IP",ip);//20231229
        operateeLog.setIp(ip);
        operateeLog.setMethod("com.sss.yunweiadmin.controller.ssoLoginForPost()");
        operateeLog.setCreateDatetime(LocalDateTime.now());
        operateeLogService.save(operateeLog);
        return getUserVO(user);
    }

    //20230201 完成打印时，（航盾先访问我的前端页面，由我）发来的身份检验;在asDeviceCommon/ListForPrint发起
    @GetMapping("/printLogin")
    public String printLogin(String PID) {
        System.out.println(environment.getProperty("todoRouteForSSO"));
        System.out.println("-----ssoLogin23----");
        this.setSessionForPID(PID);
        return "index";
    }

    private SysUser setSessionForPID(String PID){
        SysUser user = sysUserService.getOne(new  QueryWrapper<SysUser>().eq("org_id",GlobalParam.orgId).eq("status","正常").eq("id_number", PID));
        if (ObjectUtil.isEmpty(user)) {
            throw new RuntimeException("用户信息不存在！");
    }
        List<SysRoleUser> roleUserList = sysRoleUserService.list(new  QueryWrapper<SysRoleUser>().eq("org_id",GlobalParam.orgId).eq("user_id", user.getId()));
        if (ObjectUtil.isEmpty(roleUserList)) {
            throw new RuntimeException("用户没有分配角色");
        }
        //20211128添加角色ID
        List<Integer> roleIdList = roleUserList.stream().map(SysRoleUser::getRoleId).collect(Collectors.toList());
        user.setRoleIdList(roleIdList);
        List<SysRole> roleList = sysRoleService.list(new  QueryWrapper<SysRole>().eq("org_id",GlobalParam.orgId).in("id", roleIdList));
        List<String> roleNameList = roleList.stream().map(SysRole::getName).collect(Collectors.toList());
        user.setRoleNameList(roleNameList);
        SysDept sysDept = sysDeptService.getById(user.getDeptId());
        user.setTemp(sysDept.getName());//temp字段存放“部门名称”
        httpSession.removeAttribute("user");
        httpSession.setAttribute("user", user);
        return user;
    }
    private SysUser setSessionForPIDForNetgateLogin(String PID){
        SysUser user = sysUserService.getOne(new  QueryWrapper<SysUser>().eq("org_id",GlobalParam.orgId).eq("status","正常").eq("id_number", PID));
        if (ObjectUtil.isEmpty(user)) {
            throw new RuntimeException("用户信息不存在！");
        }
        List<SysRoleUser> roleUserList = sysRoleUserService.list(new  QueryWrapper<SysRoleUser>().eq("org_id",GlobalParam.orgId).eq("user_id", user.getId()));
        if (ObjectUtil.isEmpty(roleUserList)) {
            throw new RuntimeException("用户没有分配角色");
        }
        //20211128添加角色ID
        List<Integer> roleIdList = roleUserList.stream().map(SysRoleUser::getRoleId).collect(Collectors.toList());
        user.setRoleIdList(roleIdList);
        List<SysRole> roleList = sysRoleService.list(new  QueryWrapper<SysRole>().eq("org_id",GlobalParam.orgId).in("id", roleIdList));
        List<String> roleNameList = roleList.stream().map(SysRole::getName).collect(Collectors.toList());
        user.setRoleNameList(roleNameList);
        SysDept sysDept = sysDeptService.getById(user.getDeptId());
        user.setTemp(sysDept.getName());//temp字段存放“部门名称”
        httpSession.removeAttribute("user");
        httpSession.setAttribute("user", user);

        OperateeLog operateeLog = new OperateeLog();
        operateeLog.setParam("[PID:" + PID + "]" );
        operateeLog.setOperateModule("用户模块");
        operateeLog.setOperateType("网关登陆");
        operateeLog.setLoginName(user.getLoginName());
        operateeLog.setDisplayName(user.getDisplayName());
        //operateeLog.setIp(ServletUtil.getClientIP(request));
        String ip = (String) httpSession.getAttribute("IP");
        if(ObjectUtil.isEmpty(ip))
            ip = "";
        operateeLog.setIp(ip);
        operateeLog.setMethod("com.sss.yunweiadmin.controller.netgetLogin()");
        operateeLog.setCreateDatetime(LocalDateTime.now());
        operateeLogService.save(operateeLog);

        return user;
    }

    //待办事项与智企集成：获取列表
    @GetMapping("/todoListForSSO")// 20230511 todo问张强，这种是不是不会被“返回包装”（即使用那个返回包装注解修饰 下）
    public void todoListForSSO(HttpServletRequest request, HttpServletResponse response) throws IOException {
        System.out.println("-----todoListForSSO----");
        //单点登录代码
        String userID = request.getParameter("userID");
        String userName = request.getParameter("userName");
        String PID = request.getParameter("PID");
        String sessionID = request.getParameter("sessionID");
        String WSUrl = request.getParameter("WSUrl");
        String verifySSO = request.getParameter("verifySSO");
        String projectDetails = "<?xml version=\"1.0\" encoding=\"GB2312\"?>" +
                "<root>" +
                "<data>" +
                "<sessionID>" + sessionID + "</sessionID>" +
                "<userID>" + userID + "</userID>" +
                "<PID>" + PID + "</PID>" +
                "<verifySSO>" + verifySSO + "</verifySSO>" +
                "</data>" +
                "</root>";
        System.out.println("PID:" + projectDetails);
        LocalDate date1 = LocalDate.now();
        DateTimeFormatter fmt1 = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String dateStr1 = date1.format(fmt1);
        System.out.println(dateStr1);

        String[] param = {"common", "0", "biz.bizCheckSSO", projectDetails};

        for (int i = 0; i < 3; i++) {
            String msg = loginWebService(WSUrl, param);
            if ("1".equals(msg)) {
                //用户放入session
                // SysUser user = sysUserService.getById(19);
//                SysUser user = sysUserService.getOne(new  QueryWrapper<SysUser>().eq("org_id",GlobalParam.orgId).eq("id_number", PID));
//                List<SysRoleUser> roleUserList = sysRoleUserService.list(new  QueryWrapper<SysRoleUser>().eq("org_id",GlobalParam.orgId).eq("user_id", user.getId()));
//                //20211128添加角色ID
//                List<Integer> roleIdList = roleUserList.stream().map(SysRoleUser::getRoleId).collect(Collectors.toList());
//                user.setRoleIdList(roleIdList);
//                List<SysRole> roleList = sysRoleService.list(new  QueryWrapper<SysRole>().eq("org_id",GlobalParam.orgId).in("id", roleIdList));
//                List<String> roleNameList = roleList.stream().map(SysRole::getName).collect(Collectors.toList());
//                user.setRoleNameList(roleNameList);
//                httpSession.removeAttribute("user");
//                httpSession.setAttribute("user", user);
                SysUser user = this.setSessionForPID(PID);
                //20241110todo
                List<String> loginNameListForOperatorForCross = null;//跨系统待办人员
                QueryWrapper<ProcessInstanceData> queryWrapper = new  QueryWrapper<>();//目前唯一一处不需要加eq("org_id",GlobalParam.orgId)
                queryWrapper.notIn("process_status", Arrays.asList(new String[]{"完成","终止"})).like("display_current_step", user.getDisplayName()).like("login_current_step", user.getLoginName()).orderByDesc("last_commit_datetime");
                if(GlobalParam.orgId == 0) {//只让orgId为0时，即信息化中心来执行；约定了所与惯性公司的ID分别0、1
                    SysDic dic_operatorForCross = sysDicService.getOne(new  QueryWrapper<SysDic>().eq("org_id",1).eq("flag", "跨系统待办人员").orderByAsc("sort"));
                    if(ObjectUtil.isNotEmpty(dic_operatorForCross))
                        loginNameListForOperatorForCross = Arrays.asList(dic_operatorForCross.getName().split(","));//Arrays.asList(operatorTypeIds.split(","))

                    if(CollUtil.isNotEmpty(loginNameListForOperatorForCross) && loginNameListForOperatorForCross.contains(user.getLoginName()))//同时要获取org_id为 0、1的待办 //
                        queryWrapper.in("org_id",Arrays.asList(new Integer[]{0,1}));
                    else
                        queryWrapper.eq("org_id",GlobalParam.orgId);
                } else
                    queryWrapper.eq("org_id",GlobalParam.orgId);



                //QueryWrapper<ProcessInstanceData> queryWrapper = new  QueryWrapper<ProcessInstanceData>().eq("org_id",GlobalParam.orgId).ne("process_status", "完成").like("display_current_step", user.getDisplayName()).like("login_current_step", user.getLoginName()).orderByDesc("id");
                List<ProcessInstanceData> processInstanceDataList = processInstanceDataService.list(queryWrapper);
                //20240801 过滤“同用户名”待办
                processInstanceDataList = processInstanceDataList.stream().filter(item->{
                    List<String> list = Arrays.asList(item.getLoginCurrentStep().split(","));
                    if(list.contains(user.getLoginName()))
                        return true;
                    else
                        return  false;
                }).collect(Collectors.toList());

                String msgForList = "";
                if (CollUtil.isNotEmpty(processInstanceDataList)) {
                    msgForList = "<?xml version=\"1.0\" encoding=\"GBK\"?>" + "<list type='Mgs' rowNum='" + processInstanceDataList.size() + "'>";
                    for (ProcessInstanceData p : processInstanceDataList) {
                        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
                        String dateStr = p.getStartDatetime().format(fmt);
                        String pattern = "\\[[^\\[\\]]*\\]";//将当前步骤字符串中的【xxx】这种字符串去掉
                        msgForList += "<Mgs>";
                        msgForList += "<MgsFromDept>" + p.getDeptName() + "</MgsFromDept>"; // 信息来源部门 $$
                        //msgForList += "<MgsFromSys>"+p.getMgsFromSys()+"</MgsFromSys>";//来源系统
                        msgForList += "<MgsFromSys>新版运维系统</MgsFromSys>";//来源系统
                        msgForList += "<MgsType>流程处理</MgsType>";//类型
                        msgForList += "<MgsFunc>处理</MgsFunc>";//功能
                        msgForList += "<MgsMessage></MgsMessage>";//待办留言
                        msgForList += "<MgsFromName>" + p.getDisplayName() + "</MgsFromName>";//信息来源人 $$
                        msgForList += "<SentTime>" + dateStr + "</SentTime>";//待办时间
                        msgForList += "<MgsUrgent>一般</MgsUrgent>";//紧急程度
                        msgForList += "<MgsStatus>0</MgsStatus>";//未浏览
                        msgForList += "<MgsAccessory>0</MgsAccessory>";//没有附件
                        msgForList += "<Title>" + p.getProcessName() + "一》"+p.getDisplayCurrentStep()+ "</Title>";//标题//.replaceAll(pattern, "").replaceAll(",", "|")
                        msgForList += "<Url>"+environment.getProperty("todoRouteForSSO")+p.getId()+"^PID="+PID+"</Url>";//待办链接地址
                        msgForList += "</Mgs>";
                    }
                    msgForList += "</list>";
                    //todo断点：
                    // 将XML字符串设置为响应体
                    response.setContentType("text/html");
                    response.setCharacterEncoding("GBK");
                    PrintWriter out = response.getWriter();
                    out.write(msgForList);
                    out.flush();//加了这两个智企待办才显示出来，不加的话智企调试时也能收到返回值，但不能展示在界面：也有可能是 response.setContentType("text/html");起作用，当然也有可能是之前for里没加break反复在写入有关，有空再验吧;
                    out.close();
                    //ResponseUtils.renderXml(response,msgForList.toString(),"UTF-8");
                }
                break;


            }
        }


    }

    //待办事项与智企集成：点击具体待办时
    @PostMapping("/todoForSSO")//正式部署改Post
    public String todoForSSO() {
        System.out.println("-----todoForSSO----");
        // return "redirect:/login1";
        return "index";


    }



    @ResponseBody
    @GetMapping("logout")
    @ResponseResultWrapper
    public boolean logout() {
        httpSession.removeAttribute("user");
        return true;
    }


    @GetMapping("download")//测试下载,放这是为了“避开全局返回”
    public String download() {


        return "index";
    }

    @ResponseBody
    @GetMapping("getNameStr")
    @ResponseResultWrapper
    public ResponseResult getNameStr(Integer[] idArr) {
        List<Integer> idList = Stream.of(idArr).collect(Collectors.toList());
        //查询部门
        List<SysDept> deptList = sysDeptService.list(new  QueryWrapper<SysDept>().eq("org_id",GlobalParam.orgId));
        Map<Integer, String> deptMap = deptList.stream().collect(Collectors.toMap(SysDept::getId, SysDept::getName));

        List<SysUser> userList = sysUserService.listByIds(idList);
        String nameStr = userList.stream().map(user -> deptMap.get(user.getDeptId()) + "[" + user.getDisplayName() + "]").collect(Collectors.joining(","));
        return ResponseResult.success(nameStr);
    }
//    @GetMapping("getUserTree")
//    public List<TreeSelectVO> getUserTree() {
//        //20211121
//        List<SysUser> list =sysUserService.list(new  QueryWrapper<SysUser>());
//        return TreeUtil.getTreeSelectVO(list);
//    }

    @ResponseBody
    @GetMapping("getUserVL")
    @ResponseResultWrapper
    //是那种选择流程发起责任人时的提示框组件内容
    //20211203完善：限定了查询本部门的人（查询条件可从前台传也可直接读session）;value里把人员密级也带了进去
    public List<ValueLabelVO> getUserVL() {
        SysUser user1 = (SysUser) httpSession.getAttribute("user");
        if (user1 == null) {
            throw new RuntimeException("用户未登录或登陆超时，请关闭本页面，重新从登陆入口进入");
        }
        List<ValueLabelVO> list = new ArrayList<>();
        //过滤掉有基本要素有空缺的人员; 20260127 把"login_name"判空去掉
        List<SysUser> userList = sysUserService.list(new  QueryWrapper<SysUser>().eq("org_id",GlobalParam.orgId).eq("dept_id", ((SysUser) httpSession.getAttribute("user")).getDeptId()).ne("id_number", "").ne("secret_degree", "").notIn("status",  Arrays.asList(new String[]{"离退","停用"})));
        List<SysDept> deptList = sysDeptService.list(new  QueryWrapper<SysDept>().eq("org_id",GlobalParam.orgId));
        Map<Integer, String> deptMap = deptList.stream().collect(Collectors.toMap(SysDept::getId, SysDept::getName));

        //20221109Label字段由user.getDisplayName() + "." + deptMap.get(user.getDeptId()改为user.getDisplayName()
        return userList.stream().map(user -> new ValueLabelVO(user.getId() + "." + user.getDisplayName() + "." + user.getLoginName() + "." + deptMap.get(user.getDeptId()) + "." + user.getSecretDegree() + "." + user.getIdNumber() + "." + user.getStatus(), user.getDisplayName())).collect(Collectors.toList());
    }


    //超级管理员，重置密码

    //用户自己，修改密码
    @ResponseBody
    @GetMapping("changePassword")
    @ResponseResultWrapper
    public boolean changePassword(String oldPassword, String newPassword) {
        SysUser user = (SysUser) httpSession.getAttribute("user");
        if (!user.getPassword().equals(SecureUtil.md5(oldPassword))) {
            throw new RuntimeException("旧密码输入错误");
        }
        user.setPassword(SecureUtil.md5(newPassword));
        return sysUserService.updateById(user);
    }


    //反显-角色分配
    @ResponseBody
    @GetMapping("getRoleGiveVO")
    @ResponseResultWrapper
    public RoleGiveVO getRoleGiveVO(Integer userId) {
        QueryWrapper<SysRole> queryWrapper = new  QueryWrapper<SysRole>().eq("org_id",GlobalParam.orgId).eq("org_id",GlobalParam.orgId);
        queryWrapper.notIn("name", Arrays.asList(new String[]{"系统管理员", "安全管理员", "系统审计员"}));//20231129测评
        List<ValueLabelVO> roleList = sysRoleService.list(queryWrapper).stream().map(item -> new ValueLabelVO(item.getId(), item.getName())).collect(Collectors.toList());
        List<Integer> checkRoleIdList = sysRoleUserService.list(new  QueryWrapper<SysRoleUser>().eq("org_id",GlobalParam.orgId).eq("user_id", userId)).stream().map(SysRoleUser::getRoleId).collect(Collectors.toList());

        RoleGiveVO roleGiveVO = new RoleGiveVO();
        roleGiveVO.setRoleList(roleList);
        roleGiveVO.setCheckRoleIdList(checkRoleIdList);
        return roleGiveVO;
    }

    //修改-角色分配;20231205 改为手工写日志
    @ResponseBody
//    @OperateLog(module = "用户模块", type = "角色分配")
    @GetMapping("roleGive")
    @ResponseResultWrapper
    public boolean roleGive(HttpServletRequest request, Integer userId, Integer[] roleIdArr) {
        List<Integer> roleIdList = Stream.of(roleIdArr).collect(Collectors.toList());

        //20231205 手工写日志
        SysUser user_alter = sysUserService.getById(userId);
        List<SysRole> sysRoleList;
        List<Map<String, Object>> listMaps1 =  sysRoleService.listMaps(new  QueryWrapper<SysRole>().eq("org_id",GlobalParam.orgId).in("id",roleIdList).select("name"));
        List<String> roleNameList = listMaps1.stream().map(item -> item.get("name").toString()).collect(Collectors.toList());
        SysUser user = (SysUser) httpSession.getAttribute("user");
        OperateeLog operateeLog = new OperateeLog();
        String paramStr = "给【" + user_alter.getDisplayName() + "】分配角色：";
        if(CollUtil.isNotEmpty(roleNameList)) {
            paramStr = paramStr +  String.join(",", roleNameList);
        }
        operateeLog.setParam(paramStr);
        operateeLog.setOperateModule("角色模块");
        operateeLog.setOperateType("角色分配");
        operateeLog.setLoginName(user.getLoginName());
        operateeLog.setDisplayName(user.getDisplayName());
        //operateeLog.setIp(ServletUtil.getClientIP(request));
        String ip = (String) httpSession.getAttribute("IP");
        if(ObjectUtil.isEmpty(ip))
            ip = ServletUtil.getClientIP(request);
        operateeLog.setIp(ip);
        operateeLog.setMethod("com.sss.yunweiadmin.controller.roleGive()");
        operateeLog.setCreateDatetime(LocalDateTime.now());
        operateeLogService.save(operateeLog);

        return sysUserService.updateRoleUser(userId, roleIdList);
    }

    //检查是否已登录,或者登录过期

    @ResponseBody
    @GetMapping("/checkUser")
    @ResponseResultWrapper
    public SysUser checkUser() {
        //取出登录用户
        SysUser user = (SysUser) httpSession.getAttribute("user");
        if (user == null) {
            throw new RuntimeException("用户未登录或登陆超时，请关闭本页面，重新从登陆入口进入");
        }
        return user;
    }


    //下载用户模板
    @ResponseBody
    @GetMapping("download1")
    @ResponseResultWrapper
    public void downloadTemplate(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.ms-excel");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("用户模板（非密）", "UTF-8");
        //  String fileName = URLEncoder.encode("test", "UTF-8");
        response.setHeader("Content-disposition", "attachment;filename=" + fileName + ".xls");
        //
        ExcelWriter excelWriter = EasyExcel.write(response.getOutputStream()).useDefaultStyle(false).excelType(ExcelTypeEnum.XLS).build();
        //
        List<SysUserExcel> data0List = new ArrayList<>();
        WriteSheet sheet0 = EasyExcel.writerSheet(0, "用户信息").head(SysUserExcel.class).build();
        //
        excelWriter.write(data0List, sheet0);
        //
        excelWriter.finish();
    }

    //上传用户
    @ResponseBody
    @PostMapping("upload1")
    @ResponseResultWrapper
    @SneakyThrows
    public List<String> importUser(MultipartFile[] files, String formValue) {
        List<String> resultList = new ArrayList<>();
        //
        MultipartFile file = files[0];
        InputStream inputStream = file.getInputStream();
        //
        ExcelReader excelReader = EasyExcel.read(inputStream).build();
        //
        ExcelListener<SysUserExcel> listener0 = new ExcelListener<>();
        //获取sheet对象
        ReadSheet sheet0 = EasyExcel.readSheet(0).head(SysUserExcel.class).registerReadListener(listener0).build();
        //读取数据
        excelReader.read(sheet0);
        //获取数据
        List<SysUserExcel> list0 = listener0.getData();
        //去重后的Excellist
        List<SysUserExcel> resultExcelList;
        if (ObjectUtil.isNotEmpty(list0)) {
            List<SysUser> userList = new ArrayList<>();
            //20211116
            List<SysUser> redundantUserDbList = sysUserService.list(new  QueryWrapper<SysUser>().eq("org_id",GlobalParam.orgId).in("login_name", list0.stream().map(item -> item.getLoginName()).collect(Collectors.toList())));
            if (ObjectUtil.isEmpty(redundantUserDbList)) {
                resultExcelList = list0;

            } else {//过滤掉重复的ExcelList
                Set<String> redundantNoSet = redundantUserDbList.stream().map(item -> item.getLoginName()).collect(Collectors.toSet());
                resultExcelList = list0.stream().filter(item -> !redundantNoSet.contains(item.getLoginName())).collect(Collectors.toList());
            }
            if (ObjectUtil.isNotEmpty(resultExcelList)) {
                for (SysUserExcel sysUserExcel : resultExcelList) {
                    //20211116实际还应判断表格中的身份证号/用户登陆名是不是有重复，有的话也要拒绝；相应设备导入时也是这样：todo后绪添加
                    if (ObjectUtil.isEmpty(sysUserExcel.getLoginName()) || ObjectUtil.isEmpty(sysUserExcel.getDeptName()) || ObjectUtil.isEmpty(sysUserExcel.getIdNumber()) || ObjectUtil.isEmpty(sysUserExcel.getSecretDegree())) {
                        throw new RuntimeException("请检查用户信息的完整性");
                    }
                    SysDept sysDept = sysDeptService.getOne(new  QueryWrapper<SysDept>().eq("org_id",GlobalParam.orgId).eq("name", sysUserExcel.getDeptName()));
                    if (ObjectUtil.isEmpty(sysDept)) throw new RuntimeException("请检查部门信息的准确性");
                    Integer deptId = sysDept.getId();
                    SysUser user = new SysUser();
                    BeanUtils.copyProperties(sysUserExcel, user);
                    user.setDeptId(deptId);
                    //设置默认密码
                    user.setPassword(SecureUtil.md5("12345678"));
                    userList.add(user);
                }
                sysUserService.saveBatch(userList);
                //20221202
                List<Integer> idList = userList.stream().map(item -> item.getId()).collect(Collectors.toList());
                //  List<SysRoleUser> sysRoleUserList =sysRoleUserService.list(new  QueryWrapper<SysRoleUser>().eq("org_id",GlobalParam.orgId).in("id",idList));
                List<SysRoleUser> sysRoleUserList = new ArrayList<>();
                for (Integer i : idList) {
                    SysRoleUser sysRoleUser = new SysRoleUser();
                    sysRoleUser.setUserId(i);
                    sysRoleUser.setRoleId(GlobalParam.roleIdForAverage);//普通用户的角色ID
                    sysRoleUserList.add(sysRoleUser);
                }
                if (CollUtil.isNotEmpty(sysRoleUserList)) {
                    sysRoleUserService.saveBatch(sysRoleUserList);
                }
                // sysRoleUserService.update
//
            }
            if (ObjectUtil.isEmpty(redundantUserDbList)) {
                resultList.add(userList.size() + "条用户被导入");
            } else {
                resultList.add(userList.size() + "条用户被导入;用户名称:" + redundantUserDbList.stream().map(item -> item.getLoginName()).collect(Collectors.joining(",")) + "已经存在，未导入");
            }
        } else {
            resultList.add("EXCEL中未填入有效用户信息");
        }
        return resultList;
    }
}
