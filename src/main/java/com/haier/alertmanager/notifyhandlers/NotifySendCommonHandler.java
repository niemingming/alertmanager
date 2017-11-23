package com.haier.alertmanager.notifyhandlers;

import com.haier.alertmanager.configuration.AlertConfigurationProp;
import com.haier.alertmanager.container.AlertRecordContainer;
import com.haier.alertmanager.container.MessageReceiverContainer;
import com.haier.alertmanager.model.AlertRecord;
import com.haier.alertmanager.model.MessageReceiverInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Date;
import java.util.List;

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
    @Autowired
    private AlertConfigurationProp alertConfigurationProp;
    /*消息接收者容器*/
    @Autowired
    private MessageReceiverContainer messageReceiverContainer;
    /*通知重发时间间隔，单位秒*/
    private long resendinterval;

    /**
     * @description 初始化方法
     * @date 2017/11/20
     * @author Niemingming
     */
    @PostConstruct
    public  void  init(){
        String interval = alertConfigurationProp.resendinterval;
        if ("".equals(interval)){
            interval = "1h";//默认轮询间隔是1小时
        }
        char flag = interval.charAt(interval.length() - 1);
        resendinterval = 0l;
        //天数
        if (flag == 'd'){
            resendinterval = Long.parseLong(interval.substring(0,interval.length()-1));
            resendinterval *= 24*60*60;
        }else if (flag == 'h'){//小时
            resendinterval = Long.parseLong(interval.substring(0,interval.length()-1));
            resendinterval *= 60*60;
        }else if (flag == 'm'){//分钟
            resendinterval = Long.parseLong(interval.substring(0,interval.length()-1));
            resendinterval *= 60;
        }else if (flag == 's'){//秒
            resendinterval = Long.parseLong(interval.substring(0,interval.length()-1));
        }else {//默认单位是秒
            resendinterval = Long.parseLong(interval);
        }
    }
    @Override
    public boolean supportRule(AlertRecord record) {
        //总是返回true，表示可以处理所有的告警信息。
        return true;
    }

    @Override
    public void sendNotify(AlertRecord record) {
        long now = new Date().getTime()/1000;
        if (record.getLastNotifyTime() == 0
                || now - record.getLastNotifyTime() >= resendinterval
                || record.getEndsAt() > record.getLastNotifyTime()){
            //将发送规则下沉到具体规则实现中，这里规则是当没有发送过通知，或者发送通知达到重发间隔时发送消息通知，或者消息结束时间大于上次通知时间，表示还没发送过。
            //TODO 调用消息发送服务
            List<MessageReceiverInfo> messageReceiverInfos = messageReceiverContainer.getReceiversByRecord(record);
            if (messageReceiverInfos.isEmpty()) {
                System.out.println("告警信息【" + record.getAlertname() + "】未配置消息接收者！");
            }else {
                record.setLastNotifyTime(new Date().getTime()/1000);
                alertRecordContainer.updateRecord(record);
                System.out.println("服务已经发送");
            }
        }

    }
}
