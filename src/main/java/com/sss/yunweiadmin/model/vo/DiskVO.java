package com.sss.yunweiadmin.model.vo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class DiskVO {

    private Integer id;

    private Integer hostAsId;

    private String sn;

    private String model;//mode

    private String state;//status

    private String miji;

    private Integer price;//capacity

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate made_date;//createTime
   // private String  flag;


}
