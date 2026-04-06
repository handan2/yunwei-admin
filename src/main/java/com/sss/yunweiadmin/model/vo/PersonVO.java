package com.sss.yunweiadmin.model.vo;
import lombok.Data;



public class PersonVO {
        private String A0000;//可能是人力内部的ID  无。。。。。。。。
        private String UNIQUE_ID;//人力GUID
        private String NBASE;//来源库类别：如在职人员库、离退人员库
        private String NBASE_0;//来源库编码
        private String A0100;//人员编号|00002477；人力内部ID
        private String A0101;//中文姓名
        private String B0110_0;//所级单位编号 | 94
        private String B0110;//所级单位名称 |  返回json无
        private String E0122_0;//部门编号 | 94A204A6
        private String A011I;//“部门编号：部门名称” | API无 | "94A204A6:四室"
       // private String A0144;  //无。。。。。。。
        private String E01A1_0;//岗位编号 | 94A204A612
        private String E01A1;//岗位名称 | 返回json无
        private String B0110_CODE;//API无 | 0094
        private String E0122_CODE;//API无 | ""
        private String E01A1_CODE;//API无 | ""
        private String SYS_FLAG;//API无 | 1
        private String SDATE;//更新时间 |2024-12-01 12：36：36
        private String FLAG;//API无 | 1
        private String A0183;//"4:劳动外包"
        private String A0184;//"10:员工"
        private String A010N;//"01:非密"
        private String A011M;//"4:技能岗"
        private String A0177;//身份证号|130246198409130136
        private String STATUS;//状态标识 |0：已同步、1：新增、2：更新、3：删除
        private String YUNWEI;//运维状态标识 |0：已同步、1：新增、2：更新、3：删除
        private String YUNWEI_SDATE;

//            "A0000":"1490215",
        //                "UNIQUE_ID":"FFA9200410F911EF01F53009F920A3CD",
//                "NBASE":"在岗人员库",
//                "NBASE_0": "Usr",
//                "A0100":"00002477",
//                "A0101":"王夫田",
//                "B0110_0": "94",
//                "E0122_0":"94A204A6",
//                "E01A1_0":"94204A612",//完整
//                "B0110_CODE":"0094",
//                "E0122_CODE":"",
//                "E01A1_CODE":"",
//                "SYS_FLAG":"1",
//                "SDATE":"2024-12-24 15:06:37",
//                "FLAG":"1",
//                "E01A1":"光学器件测试",
//                "B0110":"94",
//                "A011I":"94A204:四室",
//                "A0183":"4:劳务外包",
//                "A0184":"10:员工",
//                "A010N":"01:丰密",
//                "A011M":"4:技能岗",
//                "A0177":"132201199409096618",
//                "STATUS":"1",
//                "YUNWEI": "1",
//                "YUNWEI_SDATE": ""}],
}
