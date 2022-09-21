package com.sss.yunweiadmin.controller;


import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.XmlUtil;
import cn.hutool.crypto.SecureUtil;
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
import com.sss.yunweiadmin.common.operate.OperateLog;
import com.sss.yunweiadmin.common.result.ResponseResult;
import com.sss.yunweiadmin.common.result.ResponseResultWrapper;
import com.sss.yunweiadmin.common.utils.TreeUtil;
import com.sss.yunweiadmin.model.entity.*;
import com.sss.yunweiadmin.model.excel.ExcelListener;
import com.sss.yunweiadmin.model.excel.SysUserExcel;
import com.sss.yunweiadmin.model.vo.RoleGiveVO;
import com.sss.yunweiadmin.model.vo.TreeSelectVO;
import com.sss.yunweiadmin.model.vo.UserVO;
import com.sss.yunweiadmin.model.vo.ValueLabelVO;
import com.sss.yunweiadmin.service.*;
import lombok.SneakyThrows;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.axis.client.Call;
import org.apache.axis.client.Service;
import org.apache.axis.encoding.XMLType;
import org.w3c.dom.Document;

import javax.xml.namespace.QName;
import javax.xml.rpc.ParameterMode;
import javax.xml.rpc.ServiceException;
import javax.xml.xpath.XPathConstants;
import java.rmi.RemoteException;

/**
 * <p>
 * 用户表 前端控制器
 * </p>
 *
 * @author 任勇林
 * @since 2021-04-09
 */
//@RestController
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

    @ResponseBody
    @ResponseResultWrapper
    @GetMapping("list")
    public IPage<SysUser> list(int currentPage, int pageSize, String loginName, String displayName, Integer deptId) {
        QueryWrapper<SysUser> queryWrapper = new QueryWrapper<>();
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
        page.getRecords().forEach(item -> item.setTemp(sysDeptService.getById(ObjectUtil.isNotEmpty(item.getDeptId()) ? item.getDeptId() : 2).getName()));
        return page;
    }

    @ResponseBody
    @OperateLog(module = "用户模块", type = "添加用户")
    @ResponseResultWrapper
    @PostMapping("add")
    public int add(@RequestBody SysUser sysUser) {//20220820返回值类型改成int:返回新增用户的ID：为了前端在手工添加代理人后需要获得用户ID(以拼成committer_str)
        return sysUserService.add(sysUser);
    }


    @ResponseBody
    @OperateLog(module = "用户模块", type = "编辑用户")
    @PostMapping("edit")
    @ResponseResultWrapper
    public boolean edit(@RequestBody SysUser sysUser) {
        System.out.println(sysUser);
        return sysUserService.updateById(sysUser);
    }

    @ResponseBody
    @OperateLog(module = "用户模块", type = "编辑用户")
    @GetMapping("upateByIdentity")
    @ResponseResultWrapper
    public boolean upateByIdentity(String identity ,String loginName) {
        System.out.println("sysUser");

        return sysUserService.upateByIdentity(identity ,loginName);

    }

    @ResponseBody
    @GetMapping("get")
    @ResponseResultWrapper
    public SysUser getById(String id) {
        return sysUserService.getById(id);
    }

    @OperateLog(module = "用户模块", type = "删除用户")
    @ResponseBody
    @GetMapping("delete")
    @ResponseResultWrapper
    public boolean delete(Integer[] idArr) {
        return sysUserService.delete(idArr);
    }

    //20211128张强加，我先不封装，我的一些东东略有变化
    @ResponseBody
    @ResponseResultWrapper
    private UserVO getUserVO(SysUser user) {
        UserVO userVO = new UserVO();
        //根据用户获取角色
        List<SysRoleUser> roleUserList = sysRoleUserService.list(new QueryWrapper<SysRoleUser>().eq("user_id", user.getId()));
        if (CollUtil.isEmpty(roleUserList)) {
            throw new RuntimeException("用户没有分配角色");
        }
        List<Integer> roleIdList = roleUserList.stream().map(SysRoleUser::getRoleId).collect(Collectors.toList());
        //根据角色获取权限
        List<SysRolePermission> rolePermissionList = sysRolePermissionService.list(new QueryWrapper<SysRolePermission>().in("role_id", roleIdList));
        if (CollUtil.isEmpty(rolePermissionList)) {
            throw new RuntimeException("用户没有分配菜单");
        }
        //根据权限获取完整的权限
        List<Integer> permissionIdList = rolePermissionList.stream().map(SysRolePermission::getPermissionId).collect(Collectors.toList());
        List<SysPermission> permissionList = sysPermissionService.list(new QueryWrapper<SysPermission>().in("id", permissionIdList));
        List<SysPermission> allPermissionList = new ArrayList<SysPermission>();
        while (true) {
            if (ObjectUtil.isEmpty(permissionList)) {
                break;
            }
            allPermissionList.addAll(permissionList);
            //
            List<Integer> parentPermissionIdList = permissionList.stream().map(SysPermission::getPid).collect(Collectors.toList());
            permissionList = sysPermissionService.list(new QueryWrapper<SysPermission>().in("id", parentPermissionIdList));
        }
        //
        List<Integer> allPermissionIdList = allPermissionList.stream().map(SysPermission::getId).collect(Collectors.toList());
        List<SysPermission> permissionList1 = sysPermissionService.list(new QueryWrapper<SysPermission>().in("id", allPermissionIdList).orderByAsc("sort"));
        List<SysPermission> permissionList2 = sysPermissionService.list(new QueryWrapper<SysPermission>().in("id", allPermissionIdList).orderByAsc("sort"));
        List<SysPermission> permissionList3 = sysPermissionService.list(new QueryWrapper<SysPermission>().in("id", allPermissionIdList).orderByAsc("sort"));
        List<SysPermission> permissionList4 = sysPermissionService.list(new QueryWrapper<SysPermission>().in("id", allPermissionIdList).orderByAsc("sort"));
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
        SysPermission permission = sysPermissionService.getOne(new QueryWrapper<SysPermission>().eq("permission_type", "startProcess"));
        /*
            1.根据用户取出角色
            2.根据角色获取流程定义
         */
        //List<SysRole> roleList = sysRoleService.listByIds(roleIdList);
        //List<String> ProcessRoleIdList = roleList.stream().map((SysRole::getId).collect(Collectors.toList());
        List<ProcessDefinition> definitionList = processDefinitionService.list();
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


    @OperateLog(module = "用户模块", type = "登录")
    @GetMapping("login")
    @ResponseResultWrapper
    @ResponseBody
    public UserVO login(String loginName, String password) {

        //根据 登录账号 查询出用户
        List<SysUser> userList = sysUserService.list(new QueryWrapper<SysUser>().eq("login_name", loginName));
        //给user/temp字段放部门name
        userList.forEach(item -> item.setTemp(sysDeptService.getById(ObjectUtil.isNotEmpty(item.getDeptId()) ? item.getDeptId() : 2).getName()));
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
        List<SysRoleUser> roleUserList = sysRoleUserService.list(new QueryWrapper<SysRoleUser>().eq("user_id", dbUser.getId()));
        if (CollUtil.isEmpty(roleUserList)) {
            throw new RuntimeException("用户没有分配角色");
        }
        List<Integer> roleIdList = roleUserList.stream().map(SysRoleUser::getRoleId).collect(Collectors.toList());
        dbUser.setRoleIdList(roleIdList);
        List<SysRole> roleList = sysRoleService.list(new QueryWrapper<SysRole>().in("id",roleIdList));
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

    //20211128张强的 先不研
    @GetMapping("/ssoLoginForPost")
    @ResponseResultWrapper
    @ResponseBody
    public UserVO ssoLoginForPost() {
        SysUser user = (SysUser) httpSession.getAttribute("user");
        if (user == null) {
            throw new RuntimeException("用户未登录");
        }
        return getUserVO(user);
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
                SysUser user = sysUserService.getOne(new QueryWrapper<SysUser>().eq("id_number", PID));
                if (ObjectUtil.isEmpty(user)) {
                    System.out.println("身份证号不存在！");
                    return "redirect:/login";
                }
                List<SysRoleUser> roleUserList = sysRoleUserService.list(new QueryWrapper<SysRoleUser>().eq("user_id", user.getId()));
                if (ObjectUtil.isEmpty(roleUserList)) {
                    throw new RuntimeException("用户没有分配角色");
                }
                //20211128添加角色ID
                List<Integer> roleIdList = roleUserList.stream().map(SysRoleUser::getRoleId).collect(Collectors.toList());
                user.setRoleIdList(roleIdList);
                List<SysRole> roleList = sysRoleService.list(new QueryWrapper<SysRole>().in("id",roleIdList));
                List<String> roleNameList = roleList.stream().map(SysRole::getName).collect(Collectors.toList());
                user.setRoleNameList(roleNameList);
                httpSession.removeAttribute("user");
                httpSession.setAttribute("user", user);
                //
                return "index";
            }
        }

        return "redirect:/login";


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

            return "redirect:/login";
        }

    }


    @ResponseBody
    @GetMapping("logout")
    @ResponseResultWrapper
    public boolean logout() {
        httpSession.removeAttribute("user");
        return true;
    }


    @ResponseBody
    @GetMapping("getNameStr")
    @ResponseResultWrapper
    public ResponseResult getNameStr(Integer[] idArr) {
        List<Integer> idList = Stream.of(idArr).collect(Collectors.toList());
        //查询部门
        List<SysDept> deptList = sysDeptService.list();
        Map<Integer, String> deptMap = deptList.stream().collect(Collectors.toMap(SysDept::getId, SysDept::getName));

        List<SysUser> userList = sysUserService.listByIds(idList);
        String nameStr = userList.stream().map(user -> deptMap.get(user.getDeptId()) + "[" + user.getDisplayName() + "]").collect(Collectors.joining(","));
        return ResponseResult.success(nameStr);
    }
//    @GetMapping("getUserTree")
//    public List<TreeSelectVO> getUserTree() {
//        //20211121
//        List<SysUser> list =sysUserService.list(new QueryWrapper<SysUser>());
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
            throw new RuntimeException("用户未登录");
        }
        List<ValueLabelVO> list = new ArrayList<>();
        List<SysUser> userList = sysUserService.list(new QueryWrapper<SysUser>().eq("dept_id", ((SysUser) httpSession.getAttribute("user")).getDeptId()));
        List<SysDept> deptList = sysDeptService.list();
        Map<Integer, String> deptMap = deptList.stream().collect(Collectors.toMap(SysDept::getId, SysDept::getName));
        return userList.stream().map(user -> new ValueLabelVO(user.getId() + "." + user.getDisplayName() + "."+ user.getLoginName() + "." + deptMap.get(user.getDeptId()) + "." + user.getSecretDegree(), user.getDisplayName() + "." + deptMap.get(user.getDeptId()))).collect(Collectors.toList());
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
        List<ValueLabelVO> roleList = sysRoleService.list().stream().map(item -> new ValueLabelVO(item.getId(), item.getName())).collect(Collectors.toList());
        List<Integer> checkRoleIdList = sysRoleUserService.list(new QueryWrapper<SysRoleUser>().eq("user_id", userId)).stream().map(SysRoleUser::getRoleId).collect(Collectors.toList());

        RoleGiveVO roleGiveVO = new RoleGiveVO();
        roleGiveVO.setRoleList(roleList);
        roleGiveVO.setCheckRoleIdList(checkRoleIdList);
        return roleGiveVO;
    }

    //修改-角色分配
    @ResponseBody
    @GetMapping("roleGive")
    @ResponseResultWrapper
    public boolean roleGive(Integer userId, Integer[] roleIdArr) {
        List<Integer> roleIdList = Stream.of(roleIdArr).collect(Collectors.toList());
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
            throw new RuntimeException("用户未登录");
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
    public List<String> importAsset(MultipartFile[] files, String formValue) {
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
            List<SysUser> redundantUserDbList = sysUserService.list(new QueryWrapper<SysUser>().in("login_name", list0.stream().map(item -> item.getLoginName()).collect(Collectors.toList())));
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
                    SysDept sysDept = sysDeptService.getOne(new QueryWrapper<SysDept>().eq("name", sysUserExcel.getDeptName()));
                    if (ObjectUtil.isEmpty(sysDept)) throw new RuntimeException("请检查部门信息的准确性");
                    Integer deptId = sysDept.getId();
                    SysUser user = new SysUser();
                    BeanUtils.copyProperties(sysUserExcel, user);
                    user.setDeptId(deptId);
                    //设置默认密码
                    user.setPassword(SecureUtil.md5("123"));
                    userList.add(user);
                }
                sysUserService.saveBatch(userList);
//                for (SysUser user : userList) {
//                    sysUserService.add(user);
//                }
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
