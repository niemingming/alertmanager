package com.haier.alertmanager.container;

import com.google.gson.Gson;
import com.haier.alertmanager.configuration.AlertConfigurationProp;
import com.haier.alertmanager.model.AlertDictionary;
import com.haier.alertmanager.model.AlertRecord;
import com.haier.alertmanager.template.SimpleTemplate;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

/**
 * @description 告警信息基础数据字典，包含了提示信息和原因分析，告警级别以及告警触发类型等信息。
 * @date 2017/11/16
 * @author Niemingming
 */
@Component
public class AlertDictionaryContainer {
    /*数据字典表名*/
    @Autowired
    private AlertConfigurationProp alertConfigurationProp;
    /**告警信息基础表*/
    private Map<String,AlertDictionary> alertDictionaryMap;
    @Autowired
    private MongoTemplate mongoTemplate;
    /*模板处理类*/
    @Autowired
    private SimpleTemplate simpleTemplate;

    /**
     * @description 初始化方法，从数据库中加载基础配置信息
     * @date 2017/11/16
     * @author Niemingming
     */
    @PostConstruct
    public void init(){
        Map alertDictionaryMap = new HashMap<String, AlertDictionary>();
        //从表中读取所有数据字典
        DBCursor cursor = mongoTemplate.getCollection(alertConfigurationProp.alertDictionaryTableName).find();
        while (cursor.hasNext()){
            DBObject dict = cursor.next();
            AlertDictionary dictionary = new AlertDictionary(dict);
            alertDictionaryMap.put(dictionary.getAlertname(),dictionary);
        }
        this.alertDictionaryMap = alertDictionaryMap;
    }

    /**
     * @description 根据告警名称，获取要发送的告警信息
     * @date 2017/11/16
     * @author Niemingming
     */
    public String getNotifyMessage(AlertRecord record){
        return getNotifyMessage(record,true);
    }
    /**
     * @description 根据告警名称获取要发送的告警信息，并指定是否附加告警原因分析
     * @date 2017/11/16
     * @author Niemingming
     */
    public String getNotifyMessage(AlertRecord record, boolean appedReason) {
        StringBuilder templateStr = new StringBuilder();
        String alertname = record.getAlertname();
        AlertDictionary alertDictionary = alertDictionaryMap.get(alertname);
        //未查询到告警配置信息
        if (alertDictionary == null){
            System.out.println("未能查询到【" + alertname + "】的告警数据字典");
            return "";
        }
        templateStr.append(alertDictionary.getAlertsummary());
        if (appedReason){
            templateStr.append("\n").append(alertDictionary.getAlertreason());
        }
        return simpleTemplate.decodeTemplate(templateStr.toString(),record.toMap());
    }
    /**
     * @description 设置告警的级别
     * @date 2017/11/21
     * @author Niemingming
     */
    public void setAlertLevel(AlertRecord record) {
        String alertname = record.getAlertname();
        AlertDictionary alertDictionary = alertDictionaryMap.get(alertname);
        //未查询到告警配置信息
        if (alertDictionary == null){
            System.out.println("未能查询到【" + alertname + "】的告警数据字典");
            return;
        }
        record.setLevel(alertDictionary.getAlertlevel());
    }

    /**
     * @description 刷新配置信息
     * @date 2017/11/17
     * @author Niemingming
     */
    public void refresh(){
        init();
    }
}
