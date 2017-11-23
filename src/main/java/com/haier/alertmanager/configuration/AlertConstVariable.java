package com.haier.alertmanager.configuration;
/**
 * @description 整个应用的常量池
 * @date 2017/11/16
 * @author Niemingming
 */
public interface AlertConstVariable {
    /**告警信息的id计算算法*/
    public static final String ALERT_ID_ALG ="MD5";
    /**告警信息传过来的*/
    public static final String ALERT_TIME_PATTERN="yyyy-MM-dd HH:mm:ss";
    /**告警状态*/
    public static final String ALERT_STATUS_FIRING = "firing";//触发状态
    public static final String ALERT_STATUS_SURE = "sure";//确认状态
    public static final String ALERT_STATUS_RESLOVE = "resolve";//解决状态
    /*人员配置路径*/
    public static final String MESSAGE_RECEIVER_LIST = "classpath:notifysend/personlist.list";
    /*ESmapping文件路径*/
    public static final String ES_MAPPING_FILE = "classpath:notifysend/esmapping.json";
}
