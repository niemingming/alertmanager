package com.haier.alertmanager.notifyhandlers;

import com.haier.alertmanager.container.AlertRecordContainer;
import com.haier.alertmanager.model.AlertRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * @description 通用告警发送处理器，规则为接收到消息，判断是否已经发送过，如果没有立刻发送。不考虑其他逻辑
 * @date 2017/11/17
 * @author Niemingming
 */
@Order(999)//将该发送处理器设置为最低级别，便于客户规则覆盖
@Component
public class NotifySendCommonHandler implements INotifySendHanlder {
    @Autowired
    private AlertRecordContainer alertRecordContainer;

    @Override
    public boolean supportRule(AlertRecord record) {
        //总是返回true，表示可以处理所有的告警信息。
        return true;
    }

    @Override
    public void sendNotify(AlertRecord record) {
        //如果没有发送过告警消息，这里就直接发送，这里不考虑重发机制
        if (record.getLastNotifyTime() == 0){
            //TODO 调用消息发送服务
            record.setLastNotifyTime(new Date().getTime()/1000);
            alertRecordContainer.updateRecord(record);
            System.out.println("服务已经发送");
        }
    }
}
