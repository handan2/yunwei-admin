package com.sss.yunweiadmin.model.vo;

import com.sss.yunweiadmin.model.entity.ProcessFormTemplate;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = false)
public class FormTemplateVO extends ProcessFormTemplate {
    private List<ProcessFormTemplate> children;
}
