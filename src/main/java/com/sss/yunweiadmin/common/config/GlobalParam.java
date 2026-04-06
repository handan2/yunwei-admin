package com.sss.yunweiadmin.common.config;

import org.springframework.context.annotation.Configuration;


public class GlobalParam {
    public  static  int orgId = 0;
    public  static  int devRootID = 1;
    public  static  int devSubRootRootIDForSoftware = 2;
    public  static  int devSubRootRootIDForOthers = 3;

    public  static  int typeIDForCMP = 4;
    public  static  int typeIDForFWQ = 29;
    public  static  int typeIDForNET = 5;
    public  static  int typeIDForAff = 6;
    public  static  int typeIDForSafe = 7;
    public  static  int typeIDForPrint = 14;
    public  static  int typeIDForYingyong = 19;
    public  static  int typeIDForOthers = 24;
    public  static  int typeIDForApp = 58;
    public  static  int typeIDForDisk = 30;
    public  static  int typeIDForStor = 31;
    public  static  int typeIDForHdisk = 32;
    public  static  int typeIDForUdisk = 36;
    public  static  int typeIDForDrive = 38;//光驱

    public  static  int depRootID = 1;
    public  static  int depSubRootID = 2;//代表三十三所
    public  static  int deptIDForXXH = 16;
    public  static  int deptIDForBMC = 17;
    public  static  int deptIDForKBC = 23;
//    public  static  int deptIDForSMY = 15;
//    public  static  int deptIDForGPY = 16;
//    public  static  int deptIDForSMQ = 17;

    public  static  int deptIDForS5 = 110;//惯性五室，所内用不上
    public  static  int deptIDForSp5 = 117;//惯性五室分部
    public  static  int cusTblIDForCMP = 16;//16代表自定义“计算机信息表”ID
    public  static  int cusTblIDForStor = 30;//30代表自定义“存储介质信息表”ID
    public  static  int cusTblIDForDisk = 39;//



    public  static  int roleIdForAverage = 11;//20241206
	public  static  int roleIdForDeptManager = 12;
	public  static  int roleIdForSafeManager = 7;
	public  static  int roleIdForDeptCheifManager = 126;//惯性公司部门正职，所内用不上

    public  static  int roleIdForAssist = 8;



}
