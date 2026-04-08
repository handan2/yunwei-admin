package com.sss.yunweiadmin.model.excel;

import com.alibaba.excel.annotation.ExcelIgnore;
import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.format.DateTimeFormat;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Data;

/**  小U增加
 * 流程实例导出Excel
 * 包含流程实例基本信息 + ProcessFormValue1.value 的JSON解析字段
 */
@Data
public class ProcessInstanceExcel {

    @ExcelProperty("*流程名称")
    private String processName;

    @ExcelProperty("*流程状态")
    private String processStatus;

    @ExcelProperty("*当前步骤")
    private String displayCurrentStep;

    @ExcelProperty("*工单编号")
    private String orderNum;

    @ExcelProperty("*提交人")
    private String displayName;

    @ExcelProperty("*提交部门")
    private String deptName;

    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    @ExcelProperty("*提交时间")
    private String startDatetime;

    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    @ExcelProperty("*结束时间")
    private String endDatetime;

    @ExcelProperty("*评分")
    private Integer score;

    // ==================== JSON解析字段 ====================
    // 以下字段对应 ProcessFormValue1.value 中存储的JSON的key
    // 根据实际JSON结构动态解析，以下为示例字段

    @ExcelProperty("备注")
    private String remark;

    @ExcelProperty("附件ID")
    private String attachmentIds;

    // 兼容ProcessFormValue1.value中可能包含的各种自定义字段
    @ExcelIgnore
    private String jsonValue;

    // 动态追加JSON字段用，key=字段名，value=字段值
    @ExcelIgnore
    private java.util.Map<String, String> extraFields;
}
