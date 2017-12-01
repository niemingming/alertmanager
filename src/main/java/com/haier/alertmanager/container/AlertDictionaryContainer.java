package com.haier.alertmanager.container;

import com.haier.alertmanager.configuration.AlertConfigurationProp;
import com.haier.alertmanager.model.AlertDictionary;
import com.haier.alertmanager.model.AlertRecord;
import com.haier.alertmanager.template.SimpleTemplate;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Date;
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
     * 告警提示信息格式。
     * 【level】系统：project 告警，描述：description 建议：suggest
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
        //同时复制告警级别
        setAlertLevel(record,alertDictionary);
        String levelStr = alertConfigurationProp.alertlevel.get(record.getLevel());
        if (record.getEndsAt() > record.getStartsAt()){//如果告警恢复，就不直接传入恢复级别
            levelStr = "恢复";
        }
        //拼装消息头
        templateStr.append("【").append(levelStr).append("】 系统：").append(record.getProject()).append(" 告警，");
        templateStr.append("描述：").append(alertDictionary.getDescription());
        if (appedReason){
            templateStr.append("\n建议：").append(alertDictionary.getSuggest());
        }
        return simpleTemplate.decodeTemplate(templateStr.toString(),record.toMap());
    }
    /**
     * @description 设置告警的数据字典字段
     * @date 2017/11/21
     * @author Niemingming
     */
    public void setAlertLevel(AlertRecord record, AlertDictionary alertDictionary) {
        record.setLevel(alertDictionary.getAlertlevel());
        //设置告警分类
        record.setAlertCategory(alertDictionary.getAlertCategory());
        //获取告警描述
        record.setDescription(simpleTemplate.decodeTemplate(alertDictionary.getDescription(),record.toMap()));
        //获取告警建议
        record.setSuggest(simpleTemplate.decodeTemplate(alertDictionary.getSuggest(),record.toMap()));

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
