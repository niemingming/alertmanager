package com.haier.alertmanager.container;

import com.haier.alertmanager.configuration.AlertConfigurationProp;
import com.haier.alertmanager.configuration.AlertConstVariable;
import com.haier.alertmanager.model.AlertRecord;
import com.haier.alertmanager.service.HistoryStoreService;
import com.haier.alertmanager.service.NotifySendService;
import com.mongodb.DBCursor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Date;
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
    /**
     * @description 初始化方法，完成告警记录的加载
     * @date 2017/11/16
     * @author Niemingming
     */
    @PostConstruct
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
    public AlertRecord addRecord(Map record){
        AlertRecord record1 = new AlertRecord(record);
        AlertRecord record2 = records.get(record1.getId());
        //判断缓存中是否存在，如果存在则更新接收次数和上次接收时间字段。
        if (record2 != null){
            if (record1.getEndsAt() > record2.getStartsAt()//有了结束时间，表示告警已经结束可以消缺了。
                    ||record2.getEndsAt() > record2.getStartsAt()){//处理遗留数据
                record2.setEndsAt(record1.getEndsAt());
                record2.setStatus(AlertConstVariable.ALERT_STATUS_RESLOVE);
                record2.setTimes(record2.getTimes() + 1);
                record2.setLastReceiveTime(new Date().getTime()/1000);
            }
            record1 = record2;
            record1.setTimes(record1.getTimes() + 1);
            record1.setLastReceiveTime(new Date().getTime()/1000);
        }else{
            records.put(record1.getId(),record1);
        }
        //更新或者插入告警记录
        mongoTemplate.getCollection(alertConfigurationProp.alertRecordTalbeName).update(record1.toQuerySqlById(),record1.toSql(),true,false);
        //出发消息发送事件
        notifySendService.sendNotify(record1);
        //判断是否结束，如果结束，需要放到历史数据中
        if (AlertConstVariable.ALERT_STATUS_RESLOVE.equals(record1.getStatus())
                && historyStoreService.storeHistoryRecord(record1)){
           records.remove(record1.getId());//删除缓存中记录
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
}
