package com.haier.alertmanager.notifyhandlers;

import com.haier.alertmanager.model.AlertRecord;

/**
 * @description 历史告警信息转储接口
 * @date 2017/11/17
 * @author Niemingming
 */
public interface INotifyStorageHandler {
    /**
     * @description 判断是否需要由该处理器处理转储事情
     * @date 2017/11/17
     * @author Niemingming
     */
    public boolean shouldResolve(AlertRecord record);
    /**
     * @description 转储处理器
     * @date 2017/11/17
     * @author Niemingming
     */
    public void saveRecord(AlertRecord record);

}
