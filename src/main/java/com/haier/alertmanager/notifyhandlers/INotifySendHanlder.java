package com.haier.alertmanager.notifyhandlers;

import com.haier.alertmanager.model.AlertRecord;

/**
 * @description 通知发送处理接口，用于后续扩展
 * @date 2017/11/17
 * @author Niemingming
 */
public interface INotifySendHanlder {
    /**
     * @description 判断是否由该处理器，处理该告警。每条告警只能有一个处理器处理。
     * @date 2017/11/17
     * @author Niemingming
     */
    public boolean supportRule(AlertRecord record);
    /**
     * @description 发送消息方法。
     * @date 2017/11/17
     * @author Niemingming
     */
    public void sendNotify(AlertRecord record);

}
