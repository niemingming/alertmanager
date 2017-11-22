package com.haier.alertmanager.model;

import com.haier.alertmanager.configuration.AlertConstVariable;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.sun.org.apache.xerces.internal.impl.dv.util.HexBin;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @description 告警记录实体
 * @date 2017/11/16
 * @author Niemingming
 */
public class AlertRecord {
    /*系统编码，根据labels生成的md5码，也是告警信息的唯一标识*/
    private String id;
    /*告警id与ESId相对应*/
    private String alertId;
    /*告警名称*/
    private String alertname;
    /*告警开始时间*/
    private long startsAt;
    /*告警结束时间*/
    private long endsAt;
    /*上次通知发出时间*/
    private long lastNotifyTime;
    /*最后一次接收到告警时间*/
    private long lastReceiveTime;
    /*告警信息的所有labels*/
    private Map labels;
    /*排序后的labes字符串*/
    private String sortLabels;
    /**告警状态*/
    private String status;
    /*告警级别*/
    private String level;
    /**接收通知次数*/
    private int times = 0;
    /**通知消息*/
    private String message;
    /**
     * @description 空函数的初始化操作，用于该类的一些方法
     * @date 2017/11/16
     * @author Niemingming
     */
    public AlertRecord(){
    }
    /**
     * @description 接收到告警信息时的初始化操作
     * @date 2017/11/16
     * @author Niemingming
     */
    public AlertRecord(Map record){
        //处理告警开始时间、结束时间和接收时间
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(AlertConstVariable.ALERT_TIME_PATTERN);
        startsAt = getAlertTime(record,"startsAt",simpleDateFormat);
        endsAt = getAlertTime(record,"endsAt",simpleDateFormat);
        lastReceiveTime = new Date().getTime()/1000;//精确到秒
        times = 1;
        status = AlertConstVariable.ALERT_STATUS_FIRING;//告警状态为触发状态
        Map labels = record.get("labels") == null ? null : (Map) record.get("labels");
        if (labels == null){
            System.out.println("未找到告警信息的【lables】标识，无法初始化");
        }else {
            String alertname = labels.get("alertname") + "";
            setAlertname(alertname);
            setLabels(labels);
        }
    }
    /**
     * @description 获取指定的告警时间
     * @date 2017/11/16
     * @author Niemingming
     */
    private long getAlertTime(Map record, String key, SimpleDateFormat simpleDateFormat) {
        Object start = record.get(key);
        if (start != null) {
            String startstr = start.toString().replace("T"," ").substring(0,Math.min(19,start.toString().length()));
            try {
                return simpleDateFormat.parse(startstr).getTime()/1000;
            } catch (ParseException e) {
                System.out.println("告警时间格式不正确！");
                e.printStackTrace();
            }
        }
        return 0;
    }

    /**
     * @description 从mongo查询出来的结果初始化操作
     * @date 2017/11/16
     * @author Niemingming
     */
    public AlertRecord(DBObject record){
        //从数据库获取，提取关键时间字段即为labels
        Object alertname = record.get("alertname");
        setAlertname(alertname + "");
        //提取非labels字段，并赋值
        String mid = record.removeField("_id")+"";
        startsAt = record.get("startsAt") == null ? 0l :(Long) record.removeField("startsAt");
        endsAt = record.get("endsAt") == null ? 0l :(Long) record.removeField("endsAt");
        lastReceiveTime = record.get("lastReceiveTime") == null ? 0l :(Long) record.removeField("lastReceiveTime");
        lastNotifyTime = record.get("lastNotifyTime") == null ? 0l :(Long) record.removeField("lastNotifyTime");
        times = record.get("times") == null ? 0 :(Integer) record.removeField("times");
        status = record.removeField("status")+"";
        alertId = record.removeField("alertId")+"";
        //形成labels字段，并计算id
        setLabels(record.get("labels") == null ? new HashMap() : (Map) record.get("labels"));
        id = mid;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
        setAlertId(id + "-" + startsAt);
    }

    public String getAlertname() {
        return alertname;
    }

    public void setAlertname(String alertname) {
        this.alertname = alertname;
    }

    public long getStartsAt() {
        return startsAt;
    }

    public void setStartsAt(long startAt) {
        this.startsAt = startAt;
    }

    public long getEndsAt() {
        return endsAt;
    }

    public void setEndsAt(long endsAt) {
        this.endsAt = endsAt;
    }

    public long getLastNotifyTime() {
        return lastNotifyTime;
    }

    public void setLastNotifyTime(long lastNotifyTime) {
        this.lastNotifyTime = lastNotifyTime;
    }

    public long getLastReceiveTime() {
        return lastReceiveTime;
    }

    public void setLastReceiveTime(long lastReceiveTime) {
        this.lastReceiveTime = lastReceiveTime;
    }

    public Map getLabels() {
        return labels;
    }

    public void setLabels(Map labels) {
        this.labels = labels;
        List<String> labelstr = new ArrayList<String>();
        for (Object key : labels.keySet()){
            if (key == null){
                continue;//忽略null值对告警信息的判断
            }
            Object value = labels.get(key);
            labelstr.add(key+"="+value);
        }
        //将labels排序后形成字符串，为后面id计算准备
        Collections.sort(labelstr);
        setSortLabels(labelstr.toString());
    }

    public String getSortLabels() {
        return sortLabels;
    }
    /**
     * @description 设置排序后的整体字符串，同时计算id值
     * @date 2017/11/16
     * @author Niemingming
     */
    public void setSortLabels(String sortLabels) {
        this.sortLabels = sortLabels;
        //计算labels的md5码
        try {
            MessageDigest messageDigest = MessageDigest.getInstance(AlertConstVariable.ALERT_ID_ALG);
            //ES不支持大写字母，统一双方字符
            setId(HexBin.encode(messageDigest.digest(sortLabels.getBytes("UTF-8"))));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            System.out.println("系统不支持配置的计算算法");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            System.out.println("不支持的字符串编码格式");
        }
    }
    public int getTimes() {
        return times;
    }

    public void setTimes(int times) {
        this.times = times;
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AlertRecord that = (AlertRecord) o;

        return id.equals(that.id);
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getAlertId() {
        return alertId;
    }

    public void setAlertId(String alertId) {
        this.alertId = alertId;
    }

    /**
     * @description 将属性值转为插入数据
     * @date 2017/11/16
     * @author Niemingming
     */
    public DBObject toSql(){
        DBObject object = new BasicDBObject();
        object.put("_id",id);
        object.put("startsAt",startsAt);
        object.put("endsAt",endsAt);
        object.put("lastNotifyTime",lastNotifyTime);
        object.put("lastReceiveTime",lastReceiveTime);
        object.put("times",times);
        object.put("status",status);
        object.put("level",level);
        object.put("alertId",alertId);
        object.put("labels",this.labels);
        return object;
    }
    /**
     * @description 生成模板替换需要的数据
     * @date 2017/11/16
     * @author Niemingming
     */
    public Map toMap(){
        DBObject object = toSql();
        object.put("startsAt",new Date(this.startsAt*1000));
        object.put("endsAt",new Date(this.endsAt*1000));
        object.put("lastNotifyTime",new Date(this.lastNotifyTime*1000));
        object.put("lastReceiveTime",new Date(this.lastReceiveTime*1000));
        return  object.toMap();
    }
    /**
     * @description 输出以id为查询条件的mongo对象
     * @date 2017/11/16
     * @author Niemingming
     */
    public DBObject toQuerySqlById(){
        DBObject object = new BasicDBObject();
        object.put("_id",id);
        return object;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "AlertRecord{" +
                "id='" + id + '\'' +
                ", alertname='" + alertname + '\'' +
                ", startAt=" + startsAt +
                ", endsAt=" + endsAt +
                ", lastNotifyTime=" + lastNotifyTime +
                ", lastReceiveTime=" + lastReceiveTime +
                ", labels=" + labels +
                ",level=" + level +
                ",alertId=" + alertId +
                '}';
    }
}
