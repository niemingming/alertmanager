package com.haier.alertmanager.container;

import com.google.gson.JsonObject;
import com.haier.alertmanager.configuration.AlertConfigurationProp;
import com.haier.alertmanager.configuration.AlertConstVariable;
import com.haier.alertmanager.model.AlertRecord;
import com.haier.alertmanager.service.HistoryStoreService;
import com.haier.alertmanager.service.NotifySendService;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.WriteResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * @description 告警记录的内存容器，用于缓存告警记录，同时保持与数据库的一致状态
 * @date 2017/11/16
 * @author Niemingming
 */
@Component
public class AlertRecordContainer {
    @Autowired
    private MongoTemplate mongoTemplate;
    //所有当前发生的告警
    private Map<String,AlertRecord> records;
    //监控记录表明
    @Autowired
    private AlertConfigurationProp alertConfigurationProp;
    /**发送通知消息服务*/
    @Autowired
    private NotifySendService notifySendService;
    /**历史信息转储服务*/
    @Autowired
    private HistoryStoreService historyStoreService;
    /**数据字典*/
    @Autowired
    private AlertDictionaryContainer alertDictionaryContainer;
    /**
     * @description 初始化方法，完成告警记录的加载
     * @date 2017/11/16
     * @author Niemingming
     */
//    @PostConstruct 不在读取缓存
    public void initMethod(){
        records = new HashMap<String, AlertRecord>();
        DBCursor cursor = mongoTemplate.getCollection(alertConfigurationProp.alertRecordTalbeName).find();
        while (cursor.hasNext()){
            AlertRecord record = new AlertRecord(cursor.next());
            records.put(record.getId(),record);
        }
    }
    /**
     * @description 添加一条告警记录，并发送告警通知
     * @date 2017/11/16
     * @author Niemingming
     */
    public AlertRecord addRecord(JsonObject record){
        AlertRecord record1 = new AlertRecord(record);
        DBObject object = mongoTemplate.getCollection(alertConfigurationProp.alertRecordTalbeName).findOne(record1.toQuerySqlById());
        boolean shouldexec = true;
        //结束时间和1970年比较，如果比1970年晚，认为是已经结束了，否则未结束。
        if (record1.getEndsAt() > 0){
            //如果有结束时间，则设置状态为已解决
            record1.setStatus(AlertConstVariable.ALERT_STATUS_RESLOVE);
            //如果数据库中有记录，表示第一次接收到消除请求，需要更新，且删除记录；否则表示已经处理过，后续不在处理。
            shouldexec = object != null;
        }
        if (object != null){
            //如果数据库不为空，将通用属性赋值到新记录中。
            AlertRecord tmp = new AlertRecord(object);
            record1.setLastNotifyTime(tmp.getLastNotifyTime());
            record1.setTimes(tmp.getTimes()+1);
            record1.setLevel(tmp.getLevel());
            record1.setMessage(tmp.getMessage());
            record1.setSuggest(tmp.getSuggest());
            record1.setDescription(tmp.getDescription());
            record1.setAlertCategory(tmp.getAlertCategory());
            record1.setReason(tmp.getReason());
            record1.setUnit(tmp.getUnit());
        }else {//数据库中没有的时候，先初始化提示信息
            String message = alertDictionaryContainer.getNotifyMessage(record1);
            record1.setMessage(message);
        }
        //如果没有结束时间，或者有但是需要更新时，执行更新操作。
        if (shouldexec){
            DBObject obj = record1.toSql();
            //更新或者插入告警记录
            WriteResult writeResult = mongoTemplate.getCollection(alertConfigurationProp.alertRecordTalbeName).update(record1.toQuerySqlById(),obj,true,false);
            //发送消息通知
            notifySendService.sendNotify(record1);
        }
        //如果需要更新操作，且状态是解决，name就是第一次获取到解决数据，需要转储
        if (shouldexec&&AlertConstVariable.ALERT_STATUS_RESLOVE.equals(record1.getStatus())){
            historyStoreService.storeHistoryRecord(record1);//删除缓存中记录
        }
        return record1;
    }
    /**
     * @description 更新告警信息
     * @date 2017/11/17
     * @author Niemingming
     */
    public void updateRecord(AlertRecord record){
        mongoTemplate.getCollection(alertConfigurationProp.alertRecordTalbeName).update(record.toQuerySqlById(),record.toSql(),true,false);
    }
    /**
     * @description 获取所有当前告警记录
     * @date 2017/11/20
     * @author Niemingming
     */
    public Map<String,AlertRecord> getRecords(){
        return this.records;
    }
}
