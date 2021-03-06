package com.haier.alertmanager.service;

import com.haier.alertmanager.container.AlertExcluseContainer;
import com.haier.alertmanager.model.AlertRecord;
import com.haier.alertmanager.notifyhandlers.INotifySendHanlder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @description 通知发送调用服务
 * @date 2017/11/16
 * @author Niemingming
 */
@Service
public class NotifySendService {

    /*白名单列表*/
    @Autowired
    private AlertExcluseContainer alertExcluseContainer;
    /*消息发送处理器*/
    @Autowired
    private List<INotifySendHanlder> notifySendHanlders;
    /**
     * @description 发送通知消息
     * @date 2017/11/16
     * @author Niemingming
     */
    public  void sendNotify(AlertRecord record){

        //如果不在白名单列表中，发送通知。
        if (!alertExcluseContainer.inExcluseList(record)){

            for (INotifySendHanlder notifySendHanlder:notifySendHanlders){
                //如果处理器可以处理消息发送，就有该处理器处理，并且排序靠前的处理器享有较高的处理优先级
                if (notifySendHanlder.supportRule(record)){
                    notifySendHanlder.sendNotify(record);
                    break;
                }
            }
        }
    }
}
