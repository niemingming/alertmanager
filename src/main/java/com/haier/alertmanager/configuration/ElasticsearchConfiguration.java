package com.haier.alertmanager.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @description ES配置信息
 * @date 2017/11/20
 * @author Niemingming
 */
@Component
@ConfigurationProperties(prefix = "alertmanager.elasticsearch")
public class ElasticsearchConfiguration {
    /*es主机地址*/
    private List<String> hostnames;
    /*转储ES的索引日期格式*/
    private String datepattern;
    /*转储ES的类型名称*/
    private String type;
    /*index的前缀*/
    private String indexpre;
    /*模板配置文件地址*/
    private String template;

    public List<String> getHostnames() {
        return hostnames;
    }
    public void setHostnames(List<String> hostnames) {
        this.hostnames = hostnames;
    }

    public String getDatepattern() {
        return datepattern;
    }

    public void setDatepattern(String datepattern) {
        this.datepattern = datepattern;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getIndexpre() {
        return indexpre;
    }

    public void setIndexpre(String indexpre) {
        this.indexpre = indexpre;
    }

    public String getTemplate() {
        return template;
    }

    public void setTemplate(String template) {
        this.template = template;
    }
}
