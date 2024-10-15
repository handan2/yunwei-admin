package com.sss.yunweiadmin.common.utils;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

//mysql关键字 检查
public class MysqlKeyUtil {
    public static final String keys = "add,all,alteranalyze,and,asasc,asensitive,beforebetween,bigint,binaryblob,both,bycall,cascade,casechange,char,charactercheck,collate,columncondition,connection,constraintcontinue,convert,createcross,current_date,current_timecurrent_timestamp,current_user,cursordatabase,databases,day_hourday_microsecond,day_minute,day_seconddec,decimal,declaredefault,delayed,deletedesc,describe,deterministicdistinct,distinctrow,divdouble,drop,dualeach,else,elseifenclosed,escaped,existsexit,explain,falsefetch,float,float4float8,for,forceforeign,from,fulltextgoto,grant,grouphaving,high_priority,hour_microsecondhour_minute,hour_second,ifignore,in,indexinfile,inner,inoutinsensitive,insert,intint1,int2,int3int4,int8,integerinterval,into,isiterate,join,keykeys,kill,labelleading,leave,leftlike,limit,linearlines,load,localtimelocaltimestamp,lock,longlongblob,longtext,looplow_priority,match,mediumblobmediumint,mediumtext,middleintminute_microsecond,minute_second,modmodifies,natural,notno_write_to_binlog,null,numericon,optimize,optionoptionally,or,orderout,outer,outfileprecision,primary,procedurepurge,raid0,rangeread,reads,realreferences,regexp,releaserename,repeat,replacerequire,restrict,returnrevoke,right,rlikeschema,schemas,second_microsecondselect,sensitive,separatorset,show,smallintspatial,specific,sqlsqlexception,sqlstate,sqlwarningsql_big_result,sql_calc_found_rows,sql_small_resultssl,starting,straight_jointable,terminated,thentinyblob,tinyint,tinytextto,trailing,triggertrue,undo,unionunique,unlock,unsignedupdate,usage,useusing,utc_date,utc_timeutc_timestamp,values,varbinaryvarchar,varcharacter,varyingwhen,where,whilewith,write,x509xor,year_month,zerofill";

    public static String check(String str) {
        StringBuilder sb = new StringBuilder();
        Set<String> set = Arrays.stream(keys.split(",")).collect(Collectors.toSet());
        for (String tmp : str.split(",")) {
            if (set.contains(tmp)) {
                sb.append(tmp);
            }
        }
        return sb.toString();
    }

    public static void main(String[] args) {
        System.out.println(check("id, as_id, certificate_name, certificate_sn, certificate_date"));
    }
}
