package com.haier.alertmanager.service;

import com.haier.alertmanager.model.AlertRecord;
import com.haier.alertmanager.notifyhandlers.INotifyStorageHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @description 转储历史消息
 * @date 2017/11/16
 * @author Niemingming
 */
@Service
public class HistoryStoreService {
    @Autowired
    private MongoTemplate mongoTemplate;
    //历史记录处理类
    @Autowired
    private List<INotifyStorageHandler> notifyStorageHandlers;

    public boolean storeHistoryRecord(AlertRecord record){
        //转存之后删除历史记录
        for (INotifyStorageHandler notifyStorageHandler:notifyStorageHandlers){
            if (notifyStorageHandler.shouldResolve(record)){
                notifyStorageHandler.saveRecord(record);
                return true;
            }
        }
        return false;
    }
}
