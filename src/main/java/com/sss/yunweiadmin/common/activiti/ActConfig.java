package com.sss.yunweiadmin.common.activiti;

import com.google.common.collect.Lists;
import org.activiti.engine.delegate.event.ActivitiEventListener;
import org.activiti.spring.SpringProcessEngineConfiguration;
import org.activiti.spring.boot.ProcessEngineConfigurationConfigurer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ActConfig implements ProcessEngineConfigurationConfigurer {
    @Autowired
    ActivitiEventListener activitiEventListener;

    @Override
    public void configure(SpringProcessEngineConfiguration springProcessEngineConfiguration) {
        springProcessEngineConfiguration.setEventListeners(Lists.newArrayList(activitiEventListener));
    }
}
