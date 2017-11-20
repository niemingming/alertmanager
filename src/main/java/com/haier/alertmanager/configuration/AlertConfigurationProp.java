package com.haier.alertmanager.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

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
    @Value("${alertmanager.elasticsearch.hostname}")
    public String esHostName;
    /*转储ES端口*/
    @Value("${alertmanager.elasticsearch.port}")
    public int esPort;
    /*转储ES的索引日期格式*/
    @Value("${alertmanager.elasticsearch.datepattern}")
    public String esDataPattern;
    /*转储ES的类型名称*/
    @Value("${alertmanager.elasticsearch.type}")
    public String esType;

}
