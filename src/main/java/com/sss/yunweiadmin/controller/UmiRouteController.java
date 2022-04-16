package com.sss.yunweiadmin.controller;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sss.yunweiadmin.model.entity.SysRoleUser;
import com.sss.yunweiadmin.model.entity.SysUser;
import com.sss.yunweiadmin.service.SysRoleUserService;
import com.sss.yunweiadmin.service.SysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpSession;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class UmiRouteController {


    @Autowired
    SysUserService sysUserService;
    @Autowired
    SysRoleUserService sysRoleUserService;
    @Autowired
    HttpSession httpSession;
    //
    @GetMapping("/redirect")
    public String redirect() {
        System.out.println("redirect");
        // return "redirect:/ssologin";
        return "redirect:/login";
    }


//20220322目前 已不用
    @GetMapping("/ssologin")
    public String ssologin() {
        System.out.println("-----ssologin----");
        //单点登录代码
        //用户放入session
        SysUser user = sysUserService.getById(19);
        List<SysRoleUser> roleUserList = sysRoleUserService.list(new QueryWrapper<SysRoleUser>().eq("user_id",19));
        if (ObjectUtil.isEmpty(roleUserList)) {
            throw new RuntimeException("用户没有分配角色");
        }
        //20211128添加角色ID
        List<Integer> roleIdList = roleUserList.stream().map(SysRoleUser::getRoleId).collect(Collectors.toList());
        user.setRoleIdList( roleIdList );
        httpSession.removeAttribute("user");
        httpSession.setAttribute("user", user);
        //
        return "index";
    }

    //umirc.ts -> routes -> path
    //20211128调试运行时不注释也没事：只要前端页面从8000端口(8000/login)访问，而不要访问后端8080（也不能8000/yunwei/login这样访问）对应的这个/login
   @GetMapping({"/login", "/back"})//仅仅是为了和前端集成时，访问前端的view
    public String route() {
        //resources -> templates -> index.html
        return "index";
    }
}
