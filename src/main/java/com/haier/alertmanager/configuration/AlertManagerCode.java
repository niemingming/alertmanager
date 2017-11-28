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
    /*告警级别*/
    private Map<String,String> alertlevel;
    /*告警分类*/
    private Map<String,String> alertCategory;
    /*告警类型*/
    private Map<String,String> alertType;

    public Map<String, String> getAlertlevel() {
        return alertlevel;
    }
    public void setAlertlevel(Map<String, String> alertlevel) {
        this.alertlevel = alertlevel;
    }

    public Map<String, String> getAlertCategory() {
        return alertCategory;
    }

    public void setAlertCategory(Map<String, String> alertCategory) {
        this.alertCategory = alertCategory;
    }

    public Map<String, String> getAlertType() {
        return alertType;
    }

    public void setAlertType(Map<String, String> alertType) {
        this.alertType = alertType;
    }
}
