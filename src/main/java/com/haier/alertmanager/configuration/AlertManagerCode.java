package com.haier.alertmanager.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * @description 编码注入
 * @date 2017/11/27
 * @author Niemingming
 */
@Component
@ConfigurationProperties(prefix = "alertmanager.code")
public class AlertManagerCode {

    private Map<String,String> alertlevel;

    public Map<String, String> getAlertlevel() {
        return alertlevel;
    }

    public void setAlertlevel(Map<String, String> alertlevel) {
        this.alertlevel = alertlevel;
    }
}
