package com.haier.alertmanager.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;

/**
 * @description 告警通知应用所有的配置信息
 * @date 2017/11/17
 * @author Niemingming
 */
@Component
public class AlertConfigurationProp {
    /*监控记录表表名*/
    @Value("${alertmanager.alertrecord.tablename}")
    public String alertRecordTalbeName;
    /*监控白名单表表名*/
    @Value("${alertmanager.alertexcluse.tablename}")
    public String alertExcluseTableName;
    /*告警规则表表名*/
    @Value("${alertmanager.alertdictionary.tablename}")
    public String alertDictionaryTableName;
    /*转储ES配置信息*/
    public List<String> esHostNames;
    /*转储ES的索引日期格式*/
    public String esDataPattern;
    /*转储ES的类型名称*/
    public String esType;
    /*告警信息重发间隔设置*/
    @Value("${alertmanager.resendinterval}")
    public String resendinterval;
    /*index的前缀*/
    public String indexpre;
    /*ES配置文件地址*/
    public String esTemplateAddress;
    /*消息通信地址*/
    @Value("${alertmanager.notifyurl}")
    public String messageUri;

    @Autowired
    private ElasticsearchConfiguration elasticsearchConfiguration;
    @PostConstruct
    public void init(){
        this.esHostNames = elasticsearchConfiguration.getHostnames();
        this.esDataPattern = elasticsearchConfiguration.getDatepattern();
        this.esType = elasticsearchConfiguration.getType();
        this.indexpre = elasticsearchConfiguration.getIndexpre();
        this.esTemplateAddress = elasticsearchConfiguration.getTemplate();
    }

}
