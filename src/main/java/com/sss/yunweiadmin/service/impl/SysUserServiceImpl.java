package com.sss.yunweiadmin.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sss.yunweiadmin.common.config.GlobalParam;
import com.sss.yunweiadmin.common.result.ResponseResult;
import com.sss.yunweiadmin.mapper.SysUserMapper;
import com.sss.yunweiadmin.model.entity.Person;
import com.sss.yunweiadmin.model.entity.SysDept;
import com.sss.yunweiadmin.model.entity.SysRoleUser;
import com.sss.yunweiadmin.model.entity.SysUser;
import com.sss.yunweiadmin.service.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Struct;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * <p>
 * 用户表 服务实现类
 * </p>
 *
 * @author 任勇林
 * @since 2021-04-09
 */
@Service
@Slf4j
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {
    @Autowired
    private SysRoleUserService sysRoleUserService;
    @Autowired
    private SysDeptService sysDeptService;
    @Autowired
    private PersonService personService;
    @Override
    public void getRenliUsers(){
        String url = "http://10.84.7.11:8088/hcm/emp/chglist/get";
        Map<String, Object> params = new HashMap<>();

        params.put("access_token", "D8D65781E134DB327FBCCEDC5F6A69CA");
        params.put("chg_flag","new");
        params.put("yunwei","0");

        String responseStr = HttpRequest.post(url)
                .header("Content-Type","application/json")
                .body(JSONUtil.toJsonStr(params))
                .execute()
                .body();
        // String responseStr = "{\"page_count\":1,\"cur_page\":1,\"record_num\":2439,\"data\":[{\"A0000\":\"1490215\",\"UNIQUE_ID\":\"FFA9200410F911EF01F53009F920A3CD\",\"NBASE\":\"在岗人员库\",\"NBASE_0\":\"Usr\",\"A0100\":\"00002477\",\"A0101\":\"王夫田\",\"B0110_0\":\"94\",\"E0122_0\":\"94A204A6\",\"E01A1_0\":\"94204A612\",\"B0110_CODE\":\"0094\",\"E0122_CODE\":\"\",\"E01A1_CODE\":\"\",\"SYS_FLAG\":\"1\",\"SDATE\":\"2024-12-24 15:06:37\",\"FLAG\":\"1\",\"E01A1\":\"光学器件测试\",\"B0110\":\"94\",\"A011I\":\"94A204:四室\",\"A0183\":\"4:劳务外包\",\"A0184\":\"10:员工\",\"A010N\":\"01:丰密\",\"A011M\":\"4:技能岗\",\"A0177\":\"132201199409096618\",\"STATUS\":\"1\",\"YUNWEI\":\"1\",\"YUNWEI_SDATE\":\"\"}],\"flag\":\"1\",\"msg\":\"成功\"}";

        //20260115
        Map<String, String> departmentMap = MapUtil.builder(new HashMap<String, String>())
                // 室
                .put("94A201", "一室")
                .put("94A202", "二室")
                .put("94A203", "三室") // 根据数据推断
                .put("94A204", "四室")
                .put("94A205", "七室")
                .put("94A206", "六室") // 根据数据推断
                .put("94A207", "九室")
                .put("94A208", "十室")
                .put("94A209", "十一室")
                .put("94A20A", "十二室")
                .put("94A20B", "十三室")//原十三室（量子工程中心）
                .put("94A20C", "十七室")
                .put("94A20D", "十八室")
                .put("94A20E", "十九室")
                .put("94A20F", "二十室")
                .put("94A20G", "二十一室")
                .put("94A20H", "十八室")//原成都研发中心
                .put("94A20J", "三室")
                .put("94A20K", "八室")
                .put("94A101", "所办")//原所领导
                .put("94A102", "所办")//原科技委
                .put("94A103", "所办")//原所办公室
                .put("94A104", "党委办公室")
                .put("94A105", "发展计划处")
                .put("94A106", "财务处")
                .put("94A107", "预研处")
                .put("94A108", "科研生产处")
                .put("94A109", "质量处")
                .put("94A10A", "工艺处")
                .put("94A10B", "民用产业处")
                .put("94A10C", "人事教育处")
                .put("94A10D", "科研生产保障处")
                .put("94A10E", "保密处")
                .put("94A10F", "纪监法审处")
                .put("94A10G", "工会办公室")
                .put("94A10H", "市场部")
                .put("94A10I", "物资中心")
                .put("94A10J", "情报档案处")
                .put("94A10K", "信息化中心")
                .put("94A10L", "军贸代表室")
                .put("94A10M", "技安处")
                .put("94A10N", "保卫处")
                .put("94A10O", "所办")//原其他领导
                // 生产部门
                .put("94A301", "惯性系统生产一部")
                .put("94A302", "惯性系统生产二部")
                .put("94A303", "电子产品部")
                .put("94A304", "惯控实验中心")
                .put("94A305", "伺服系统生产部")
                // 公司
//                .put("94A401", "惯性公司")
//                .put("94A403", "天石和创公司")
//                .put("94A404", "机器人公司")
                // 外协
                .put("94A49A", "四室")//四室外协
//                .put("94A49D", "测试部门5")
//                .put("94A49I", "测试部门13")
//                .put("94A499", "外协人员")
//                // 特殊
//                .put("94A40101", "惯性(五室)")
                .build();

        // 解析 JSON 字符串为 JSONObject
        JSONObject jsonObject = JSONObject.parseObject(responseStr);
        // 获取 data 字段对应的 JSON 数组
        JSONArray dataArray = jsonObject.getJSONArray("data");
        List<Person> persons = dataArray.toJavaList( Person.class);
        int m = 500;

        //<三室，34>
        List<SysDept> deptList = sysDeptService.list(new  QueryWrapper<SysDept>().eq("org_id", GlobalParam.orgId).ne("pid", 0).orderByAsc("sort"));
        Map<String, Integer> deptMap = deptList.stream()
                .collect(Collectors.toMap(
                        SysDept::getName,
                        SysDept::getId
                ));

        List<SysUser> userList0 = this.list(new  QueryWrapper<SysUser>().eq("org_id", GlobalParam.orgId).in("status", Arrays.asList("正常","待审核")).orderByAsc("sort"));
        //<张三，”设备责任人，非正式用户“>
        Map<String, String> userIdRemarkMap = userList0.stream()
                .collect(Collectors.toMap(
                        SysUser::getIdNumber,
                        user -> Objects.toString(user.getRemark(), "") // 关键修改点
                ));
        //<身份ID，”待审核“> 仅用于遍历
        final List<String> DISALLOWED_WORDS = Arrays.asList("管理", "审计", "系统", "安全", "控制", "报销","运维人员");
        Map<String, String> userIdBianliMap = userList0.stream()
                .filter(user ->( Arrays.asList("一般", "重要", "核心").contains(user.getSecretDegree()) && "正常".contains(user.getStatus())))
                .filter(user -> DISALLOWED_WORDS.stream().noneMatch(word -> user.getDisplayName().contains(word)))
                .collect(Collectors.toMap(
                        SysUser::getIdNumber,
                        user -> "未扫描" // 关键修改点
                ));
        //<身份ID，userID>
        Map<String, Integer> userIdNumIdMap = userList0.stream()
                .collect(Collectors.toMap(
                        SysUser::getIdNumber,
                        SysUser::getId
                ));
        //<身份ID，密级>
        Map<String, String> userIdMijiMap = userList0.stream()
                .collect(Collectors.toMap(
                        SysUser::getIdNumber,
                        SysUser::getSecretDegree
                ));
        // 获取当前的日期和时间
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String nowTime = now.format(formatter);


        List<SysUser> sysUserList = new ArrayList<>();
        personService.remove(new  QueryWrapper<Person>());
        for (int i = 0; i < persons.size(); i += m) {
            List<Person> persons1=  new ArrayList<>(persons.subList(i, Math.min(i + m, persons.size())));
            personService.saveBatch(persons1);
            for (Person person : persons1) {
                if(ObjectUtil.isNotEmpty(person.getA010n())) {
                    String miji =  person.getA010n().split(":")[1];
                    if (ObjectUtil.isNotEmpty(miji) && Arrays.asList("一般", "重要", "核心").contains(miji) && Arrays.asList("在岗人员库", "学生库").contains(person.getNbase()) ) {
                        if( ObjectUtil.isNotEmpty(person.getA0177()) && ObjectUtil.isNotEmpty(person.getA011i()) && ObjectUtil.isNotEmpty(person.getA0101())){
                            SysUser sysUser = new SysUser();
                            sysUser.setIdNumber(person.getA0177());
                            sysUser.setSecretDegree(miji);
                            sysUser.setDisplayName(person.getA0101());

                            sysUser.setOrgCode(person.getE0122_0());//部门编码
                            sysUser.setUserCode(person.getA0100());//人员编吗
                            sysUser.setIdentity(person.getA0183());//人员身份
                            sysUser.setPosition(StringUtils.substringAfter(person.getA0184(),":"));//职务


                            userIdBianliMap.put(person.getA0177(),"已扫描");
                            if(ObjectUtil.isEmpty(userIdNumIdMap.get(person.getA0177()))){//身份证号不存在时，应为新增
                                sysUser.setRemark( "人力新增" + nowTime);
                                sysUser.setPassword(SecureUtil.md5("123456"));
                                String[] deptArr = person.getA011i().split(":");//94A208:十室
                                String deptName = departmentMap.get(deptArr[0]);
                                if(ObjectUtil.isNotEmpty(deptName)){
                                    sysUser.setDeptId(deptMap.get(deptName));

                                    sysUserList.add(sysUser);

                                } else
                                    System.out.println(deptArr[1]);
                            } else {
                                if(!miji.equals(userIdMijiMap.get(person.getA0177()))){//目前仅同步密级
                                    sysUser.setId(userIdNumIdMap.get(person.getA0177()));
                                    if("设备责任人，非正式用户".contains(userIdRemarkMap.get(person.getA0177()))){
                                        sysUser.setRemark("设备责任人，非正式用户" + "。人力密级更新" + nowTime);
                                    } else
                                        sysUser.setRemark( "人力密级更新" + nowTime);
                                    sysUserList.add(sysUser);

                                }

                            }



                        } else
                            System.out.println(person);

                    }
                }

            }

        }
        if(CollUtil.isNotEmpty(sysUserList)){
            this.saveOrUpdateBatch(sysUserList);
        }
        String result = userIdBianliMap.entrySet().stream()
                .filter(entry -> "未扫描".equals(entry.getValue()))
                .map(Map.Entry::getKey)
                .collect(Collectors.joining("\n"));
        log.info("可能要清除的涉密在岗人员------\n" + result);

    }
    
    
    @Override
    public int add(SysUser sysUser) {
        List<SysUser> list = this.list(new  QueryWrapper<SysUser>().eq("org_id",GlobalParam.orgId).eq("login_name", sysUser.getLoginName()));
        if (list.size() > 0) {
            throw new RuntimeException(sysUser.getLoginName() + "已存在");
        }
        List<SysUser> list2 = this.list(new  QueryWrapper<SysUser>().eq("org_id",GlobalParam.orgId).eq("id_number", sysUser.getIdNumber()));
        if (list2.size() > 0) {
            throw new RuntimeException(sysUser.getIdNumber() + "已存在");
        }
        //20260119 禁止手工建涉密人员
        if(GlobalParam.orgId == 0 && Arrays.asList(new String[]{"一般", "重要", "核心"}).contains(sysUser.getSecretDegree())){
            throw new RuntimeException("涉密人员基本信息已从人力资源系统同步，暂不可手工添加涉密人员");
        }
        //限制了本部门的重名情况，这种由管理员手工添加吧；20240228取消重名限制：因为会议机密钥这种“非真实人”的责任人Display名必须和实际人一样：便于后者统计他名下的“其他登陆用户”
//        List<SysUser> list3 = this.list(new  QueryWrapper<SysUser>().eq("org_id",GlobalParam.orgId).eq("display_name", sysUser.getDisplayName()).eq("dept_id",sysUser.getDeptId()));
//        if (list3.size() > 0) {
//            throw new RuntimeException(sysUser.getIdNumber() + "已存在");
//        }
        boolean flag1, flag2;
        sysUser.setPassword(SecureUtil.md5("123456"));
        flag1 = this.save(sysUser);
        //默认为普通用户
        SysRoleUser sysRoleUser = new SysRoleUser();
        sysRoleUser.setRoleId(GlobalParam.roleIdForAverage);//约定普通用户ID
        sysRoleUser.setUserId(sysUser.getId());
        flag2 = sysRoleUserService.save(sysRoleUser);
        //return flag1 && flag2;
        return sysUser.getId();//20220820改成返回新增用户的ID：为了前端在手工添加代理人后需要获得用户ID(以拼成committer_str)
    }

    @Override
    public boolean upateByIdentity(String identity, String loginName, Integer crossOrgId) {
        if(StrUtil.isNotEmpty(identity) && StrUtil.isNotEmpty(loginName)){
            Integer orgId = GlobalParam.orgId;
            if(ObjUtil.isNotEmpty(crossOrgId))
                orgId = crossOrgId;
            List<SysUser> list = this.list(new  QueryWrapper<SysUser>().eq("org_id", orgId).eq("id_number",identity));
            if(CollUtil.isNotEmpty(list)){
                SysUser user = list.get(0);
                user.setLoginName(loginName);
                this.updateById(user);
                return true;
            }
        }

        return false;

    }

    @Override
    public boolean delete(Integer[] idArr) {//20211115这个flag应该没啥用
        boolean flag1, flag2;
        List<Integer> userIdList = Stream.of(idArr).collect(Collectors.toList());
        flag1 = this.removeByIds(userIdList);
        flag2 = sysRoleUserService.remove(new  QueryWrapper<SysRoleUser>().eq("org_id",GlobalParam.orgId).in("user_id", userIdList));
        return flag1 && flag2;
    }

    @Override
    public boolean updateRoleUser(Integer userId, List<Integer> roleIdList) {
        boolean flag;
        //先删除，后插入
        sysRoleUserService.remove(new  QueryWrapper<SysRoleUser>().eq("org_id",GlobalParam.orgId).eq("user_id", userId));
        List<SysRoleUser> list = roleIdList.stream().map(roleId -> {
            SysRoleUser roleUser = new SysRoleUser();
            roleUser.setUserId(userId);
            roleUser.setRoleId(roleId);
            return roleUser;
        }).collect(Collectors.toList());
        flag = sysRoleUserService.saveBatch(list);
        return flag;
    }
}
