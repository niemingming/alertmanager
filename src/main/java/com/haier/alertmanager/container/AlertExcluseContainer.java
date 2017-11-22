package com.haier.alertmanager.container;

import com.haier.alertmanager.configuration.AlertConfigurationProp;
import com.haier.alertmanager.model.AlertExcluse;
import com.haier.alertmanager.model.AlertRecord;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @description 白名单容器，初始化系统配置的所有白名单
 * @date 2017/11/16
 * @author Niemingming
 */
@Component
public class AlertExcluseContainer {
    @Autowired
    private AlertConfigurationProp alertConfigurationProp;
    @Autowired
    private MongoTemplate mongoTemplate;
    private List<AlertExcluse> alertExcluses;

    /**
     * @description 初始化方法，加载所有白名单数据
     * @date 2017/11/16
     * @author Niemingming
     */
    @PostConstruct
    public void init(){
        List alertExcluses = new ArrayList<AlertExcluse>();
        //首先删除过时的记录
        long curr = new Date().getTime()/1000;
        DBObject query = new BasicDBObject();
        query.put("endsAt","{$lt:" + curr + "}");
        mongoTemplate.getCollection(alertConfigurationProp.alertExcluseTableName).remove(query);
        //加载所有为过时且已经生效的记录
        DBCursor cursor = mongoTemplate.getCollection(alertConfigurationProp.alertExcluseTableName).find();
        while (cursor.hasNext()){
            DBObject excluse = cursor.next();
            AlertExcluse alertExcluse = new AlertExcluse(excluse);
            alertExcluses.add(alertExcluse);
        }
        this.alertExcluses = alertExcluses;
    }
    /**
     * @description 刷新白名单列表
     * @date 2017/11/16
     * @author Niemingming
     */
    public  void refresh(){
        init();
    }
    /**
     * @description 判断是否在白名单列表中
     * @date 2017/11/16
     * @author Niemingming
     */
    public boolean inExcluseList(AlertRecord record){
        boolean flag = false;//不在白名单中
        Map map = record.getLabels();//通过labels中属性做出判断
        long now = new Date().getTime()/1000;
        for (AlertExcluse excluse:alertExcluses){
            //当前时间在白名单范围内且过滤条件吻合
            if (excluse.algExculseId(map).equals(excluse.getId())
                    &&  excluse.getStartsAt() <= now
                    && excluse.getEndsAt() >= now){
                flag = true;
                break;
            }
        }
        return flag;
    }
}
