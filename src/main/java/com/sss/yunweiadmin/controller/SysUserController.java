package com.sss.yunweiadmin.controller;


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
 * ????????? ???????????????
 * </p>
 *
 * @author ?????????
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
        IPage<SysUser> page=sysUserService.page(new Page<>(currentPage, pageSize), queryWrapper);
        page.getRecords().forEach(item->item.setTemp(sysDeptService.getById(ObjectUtil.isNotEmpty(item.getDeptId())?item.getDeptId():2).getName()));
        return page;
    }

    @ResponseBody
    @OperateLog(module = "????????????", type = "????????????")
    @ResponseResultWrapper
    @PostMapping("add")
    public boolean add(@RequestBody SysUser sysUser) {
        return sysUserService.add(sysUser);
    }


    @ResponseBody
    @OperateLog(module = "????????????", type = "????????????")
    @PostMapping("edit")
    @ResponseResultWrapper
    public boolean edit(@RequestBody SysUser sysUser) {
        System.out.println(sysUser);
        return sysUserService.updateById(sysUser);
    }

    @ResponseBody
    @GetMapping("get")
    @ResponseResultWrapper
    public SysUser getById(String id) {
        return sysUserService.getById(id);
    }

    @OperateLog(module = "????????????", type = "????????????")
    @GetMapping("delete")
    @ResponseResultWrapper
    public boolean delete(Integer[] idArr) {
        return sysUserService.delete(idArr);
    }

    //20211128????????????????????????????????????????????????????????????
    @ResponseBody
    @ResponseResultWrapper
    private UserVO getUserVO(SysUser user) {
        UserVO userVO = new UserVO();
        //????????????????????????
        List<SysRoleUser> roleUserList = sysRoleUserService.list(new QueryWrapper<SysRoleUser>().eq("user_id", user.getId()));
        if (ObjectUtil.isEmpty(roleUserList)) {
            throw new RuntimeException("????????????????????????");
        }
        List<Integer> roleIdList = roleUserList.stream().map(SysRoleUser::getRoleId).collect(Collectors.toList());
        //????????????????????????
        List<SysRolePermission> rolePermissionList = sysRolePermissionService.list(new QueryWrapper<SysRolePermission>().in("role_id", roleIdList));
        if (ObjectUtil.isEmpty(rolePermissionList)) {
            throw new RuntimeException("????????????????????????");
        }
        //?????????????????????????????????
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
        //????????????
        List<SysPermission> menuList = TreeUtil.getTreeSelect(permissionList1.stream().filter(item -> item.getType().equals("??????") || item.getType().equals("????????????")).collect(Collectors.toList()));
        //????????????-?????????
        Map<String, List<SysPermission>> operateButtonMap = new HashMap<>();
        List<SysPermission> operateButtonList = TreeUtil.getTreeSelect(permissionList2.stream().filter(item -> item.getType().equals("????????????") || "?????????".equals(item.getPosition())).collect(Collectors.toList()));
        for (SysPermission sysPermission : operateButtonList) {
            if (ObjectUtil.isNotEmpty(sysPermission.getChildren())) {
                operateButtonMap.put(sysPermission.getPath(), sysPermission.getChildren());
            }
        }
        //????????????-??????
        Map<String, List<SysPermission>> dataListButtonMap = new HashMap<>();
        List<SysPermission> dataListButtonList = TreeUtil.getTreeSelect(permissionList3.stream().filter(item -> item.getType().equals("????????????") || ("????????????".equals(item.getPosition()) && !item.getPermissionType().equals("startProcess"))).collect(Collectors.toList()));
        for (SysPermission sysPermission : dataListButtonList) {
            if (ObjectUtil.isNotEmpty(sysPermission.getChildren())) {
                dataListButtonMap.put(sysPermission.getPath(), sysPermission.getChildren());
            }
        }

        //????????????-??????????????????
        Map<Integer, SysPermission> startProcessButtonMap = new HashMap<>();
        SysPermission permission = sysPermissionService.getOne(new QueryWrapper<SysPermission>().eq("permission_type", "startProcess"));
        /*
            1.????????????????????????
            2.??????????????????????????????
         */
        //List<SysRole> roleList = sysRoleService.listByIds(roleIdList);
        //List<String> ProcessRoleIdList = roleList.stream().map((SysRole::getId).collect(Collectors.toList());
        List<ProcessDefinition> definitionList = processDefinitionService.list();
        for (ProcessDefinition processDefinition : definitionList) {
            List<String> definitionRoleIdList = Stream.of(processDefinition.getRoleId().split(",")).collect(Collectors.toList());
            List<Integer> definitionRoleIdListToInt = definitionRoleIdList.stream().map(value->Integer.valueOf(value)).collect(Collectors.toList());
            //??????
            definitionRoleIdListToInt.retainAll(roleIdList);//
            if (ObjectUtil.isNotEmpty(definitionRoleIdListToInt)) {
                startProcessButtonMap.put(processDefinition.getId(), permission);
            }
        }
        //??????
        Map<String, SysPermission> queryMap = new HashMap<>();
        List<SysPermission> queryList = TreeUtil.getTreeSelect(permissionList4.stream().filter(item -> item.getType().equals("????????????") || "query".equals(item.getPermissionType())).collect(Collectors.toList()));
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
        userVO.setRoleIdList(roleIdList);//20211113
        return userVO;
    }


    @OperateLog(module = "????????????", type = "??????")
    @GetMapping("login")
    @ResponseResultWrapper
    @ResponseBody
    public UserVO login(String loginName, String password) {

        //?????? ???????????? ???????????????
        List<SysUser> userList = sysUserService.list(new QueryWrapper<SysUser>().eq("login_name", loginName));
        //???user/temp???????????????name
        userList.forEach(item->item.setTemp(sysDeptService.getById(ObjectUtil.isNotEmpty(item.getDeptId())?item.getDeptId():2).getName()));
        if (userList.size() != 1) {
            throw new RuntimeException("???????????????");
        }
        SysUser dbUser = userList.get(0);
        //?????? ????????????
        String dbPassword = dbUser.getPassword();
        String pagePassword = SecureUtil.md5(password);
        if (!dbPassword.equals(pagePassword)) {
            throw new RuntimeException("????????????");
        }
        //20211128??????????????????????????????????????????????????????getUserVO
        UserVO userVO =getUserVO(dbUser);
        //20211113dbUser?????????????????????Table???roleIdList
        dbUser.setRoleIdList(userVO.getRoleIdList());
        //20211120??????????????????
        httpSession.removeAttribute("user");//todo??????
        httpSession.setAttribute("user", dbUser);
        return userVO;
    }

//20211128????????? ?????????
    @GetMapping("/ssoLoginForPost")
    @ResponseResultWrapper
    @ResponseBody
    public UserVO ssoLoginForPost() {
        SysUser user = (SysUser) httpSession.getAttribute("user");
        if (user == null) {
            throw new RuntimeException("???????????????");
        }
        return getUserVO(user);
    }

//???????????????????????????
    @PostMapping("/ssoLogin")//20220322?????????????????????????????????post
    public String ssoLogin(HttpServletRequest request) {
        System.out.println("-----ssoLogin23----");
        //??????????????????
        String userID = request.getParameter("userID");
        String userName= request.getParameter("userName");
        String PID = request.getParameter("PID");
        String sessionID= request.getParameter("sessionID");
        String WSUrl= request.getParameter("WSUrl");
        String verifySSO= request.getParameter("verifySSO");
        String projectDetails="<?xml version=\"1.0\" encoding=\"GB2312\"?>" +
                "<root>" +
                "<data>" +
                "<sessionID>"+sessionID+"</sessionID>" +
                "<userID>"+userID+"</userID>" +
                "<PID>"+PID+"</PID>" +
                "<verifySSO>"+verifySSO+"</verifySSO>" +
                "</data>" +
                "</root>";
        System.out.println("PID:"+projectDetails);
        String[] param={"common","0","biz.bizCheckSSO",projectDetails};

        for (int i = 0; i < 3; i++) {
            String msg = loginWebService(WSUrl, param);
            if ("1".equals(msg)) {

                //????????????session
                // SysUser user = sysUserService.getById(19);
                SysUser user = sysUserService.getOne(new QueryWrapper<SysUser>().eq("id_number", PID));
                if (ObjectUtil.isEmpty( user)) {
                    System.out.println("????????????????????????");
                    return "redirect:/login";
                }
                List<SysRoleUser> roleUserList = sysRoleUserService.list(new QueryWrapper<SysRoleUser>().eq("user_id",user.getId()));
                if (ObjectUtil.isEmpty(roleUserList)) {
                    throw new RuntimeException("????????????????????????");
                }
                //20211128????????????ID
                List<Integer> roleIdList = roleUserList.stream().map(SysRoleUser::getRoleId).collect(Collectors.toList());
                user.setRoleIdList( roleIdList );
                httpSession.removeAttribute("user");
                httpSession.setAttribute("user", user);
                //
                return "index";
            }
        }

        return "redirect:/login";






    }

    //20220327
    public String loginWebService(String WSUrl,String[] param) {
        System.out.println("come into loginWebService");
        try {
            Service service=new Service();
            Call call=(Call)service.createCall();
            call.setTargetEndpointAddress(WSUrl);
            call.setOperationName("runBiz");//???????????????
            /*??????????????????*/
            call.addParameter("packageName", XMLType.XSD_STRING, ParameterMode.IN);
            call.addParameter("unitId", XMLType.XSD_STRING, ParameterMode.IN);
            call.addParameter("processName", XMLType.XSD_STRING, ParameterMode.IN);
            call.addParameter("bizDataXML", XMLType.XSD_STRING, ParameterMode.IN);
            call.setReturnType(XMLType.XSD_STRING);
            Object obj=call.invoke(param);//obj?????????????????????XML????????????

//            String xml = "<root><a name = \"???????????????\"><b>??????????????????</b></a></root>";
//            String xml1 = "<root><data><msg>1</msg></data></root>";
            Document document = XmlUtil.parseXml((String)obj);
            Object msgString = XmlUtil.getByXPath("//root/data/msg", document, XPathConstants.STRING);
            System.out.println("msgString:"+msgString);
            return (String)msgString;
        }catch (Exception e) {
            System.out.println("=========?????????????????????????????????????????????"+e.getMessage());

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
        //????????????
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
    //???????????????????????????????????????????????????????????????
    //20211203????????????????????????????????????????????????????????????????????????????????????session???;value?????????????????????????????????
    public List<ValueLabelVO> getUserVL() {
        List<ValueLabelVO> list = new ArrayList<>();
        List<SysUser> userList = sysUserService.list(new QueryWrapper<SysUser>().eq("dept_id", ((SysUser) httpSession.getAttribute("user")).getDeptId()));
        List<SysDept> deptList = sysDeptService.list();
        Map<Integer, String> deptMap = deptList.stream().collect(Collectors.toMap(SysDept::getId, SysDept::getName));
        return userList.stream().map(user -> new ValueLabelVO(user.getId() + "." + user.getDisplayName() + "." + deptMap.get(user.getDeptId()) + "." +user.getSecretDegree(), user.getDisplayName() + "." + deptMap.get(user.getDeptId()))).collect(Collectors.toList());
    }


    //??????????????????????????????

    //???????????????????????????
    @ResponseBody
    @GetMapping("changePassword")
    @ResponseResultWrapper
    public boolean changePassword(String oldPassword, String newPassword) {
        SysUser user = (SysUser) httpSession.getAttribute("user");
        if (!user.getPassword().equals(SecureUtil.md5(oldPassword))) {
            throw new RuntimeException("?????????????????????");
        }
        user.setPassword(SecureUtil.md5(newPassword));
        return sysUserService.updateById(user);
    }


    //??????-????????????
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

    //??????-????????????
    @ResponseBody
    @GetMapping("roleGive")
    @ResponseResultWrapper
    public boolean roleGive(Integer userId, Integer[] roleIdArr) {
        List<Integer> roleIdList = Stream.of(roleIdArr).collect(Collectors.toList());
        return sysUserService.updateRoleUser(userId, roleIdList);
    }

    //?????????????????????,??????????????????

    @ResponseBody
    @GetMapping("/checkUser")
    @ResponseResultWrapper
    public SysUser checkUser() {
        //??????????????????
        SysUser user = (SysUser) httpSession.getAttribute("user");
        if (user == null) {
            throw new RuntimeException("???????????????");
        }
        return user;
    }


    //??????????????????
    @ResponseBody
    @GetMapping("download1")
    @ResponseResultWrapper
    public void downloadTemplate(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.ms-excel");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("????????????????????????", "UTF-8");
        response.setHeader("Content-disposition", "attachment;filename=" + fileName + ".xls");
        //
        ExcelWriter excelWriter = EasyExcel.write(response.getOutputStream()).useDefaultStyle(false).excelType(ExcelTypeEnum.XLS).build();
        //
        List<SysUserExcel> data0List = new ArrayList<>();
        WriteSheet sheet0 = EasyExcel.writerSheet(0, "????????????").head(SysUserExcel.class).build();
        //
        excelWriter.write(data0List, sheet0);
        //
        excelWriter.finish();
    }

    //????????????
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
        //??????sheet??????
        ReadSheet sheet0 = EasyExcel.readSheet(0).head(SysUserExcel.class).registerReadListener(listener0).build();
        //????????????
        excelReader.read(sheet0);
        //????????????
        List<SysUserExcel> list0 = listener0.getData();
        //????????????Excellist
        List<SysUserExcel> resultExcelList;
        if (ObjectUtil.isNotEmpty(list0)) {
            List<SysUser> userList = new ArrayList<>();
            //20211116
            List<SysUser> redundantUserDbList = sysUserService.list(new QueryWrapper<SysUser>().in("login_name",list0.stream().map(item ->item.getLoginName()).collect(Collectors.toList())));
            if (ObjectUtil.isEmpty(redundantUserDbList)){
                resultExcelList = list0;

            }
            else{//??????????????????ExcelList
                Set<String> redundantNoSet = redundantUserDbList.stream().map(item->item.getLoginName()).collect(Collectors.toSet());
                resultExcelList = list0.stream().filter(item->!redundantNoSet.contains(item.getLoginName())).collect(Collectors.toList());
            }
            if(ObjectUtil.isNotEmpty(resultExcelList)) {
                for (SysUserExcel sysUserExcel : resultExcelList) {
                    //20211116??????????????????????????????????????????/????????????????????????????????????????????????????????????????????????????????????????????????todo????????????
                    if (ObjectUtil.isEmpty(sysUserExcel.getLoginName())||ObjectUtil.isEmpty(sysUserExcel.getDeptName())||ObjectUtil.isEmpty(sysUserExcel.getIdNumber())||ObjectUtil.isEmpty(sysUserExcel.getSecretDegree())) {
                        throw new RuntimeException( "?????????????????????????????????");
                    }
                     SysDept sysDept= sysDeptService.getOne(new QueryWrapper<SysDept>().eq("name",sysUserExcel.getDeptName()));
                    if(ObjectUtil.isEmpty(sysDept)) throw new RuntimeException( "?????????????????????????????????");
                    Integer deptId = sysDept.getId();
                    SysUser user = new SysUser();
                    BeanUtils.copyProperties(sysUserExcel, user);
                    user.setDeptId(deptId);
                    //??????????????????
                    user.setPassword(SecureUtil.md5("123"));
                    userList.add(user);
                }
                sysUserService.saveBatch(userList);
//                for (SysUser user : userList) {
//                    sysUserService.add(user);
//                }
            }
            if(ObjectUtil.isEmpty(redundantUserDbList)){
                resultList.add(userList.size() + "??????????????????");
            } else{
                resultList.add(userList.size() + "??????????????????;????????????:"+redundantUserDbList.stream().map(item->item.getLoginName()).collect(Collectors.joining(",")) + "????????????????????????");
            }
        } else {
            resultList.add("EXCEL??????????????????????????????");
        }
        return resultList;
    }
}
