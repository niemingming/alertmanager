package com.haier.alertmanager.model;

import com.mongodb.DBObject;

/**
 * @description 告警转发的数据字典。
 * @date 2017/11/16
 * @author Niemingming
 */
public class AlertDictionary {
    /*告警名称*/
    private String alertname;
    /*告警级别*/
    private String alertlevel;
    /*告警提示信息，可以使用变量占位符${labels.name},${startsAt}*/
    private String alertsummary;
    /*告警原因*/
    private String alertreason;
    /*告警类型：机器、应用、中间件等*/
    private String alerttype;

    public AlertDictionary() {
    }
    public AlertDictionary(DBObject dict) {
        alertname = dict.get("alertname") + "";
        alertlevel = dict.get("level")+"";
        alertsummary = dict.get("summary")+"";
        alertreason = dict.get("reason")+"";
        alerttype = dict.get("type")+"";
    }

    public String getAlertname() {
        return alertname;
    }

    public void setAlertname(String alertname) {
        this.alertname = alertname;
    }

    public String getAlertlevel() {
        return alertlevel;
    }

    public void setAlertlevel(String alertlevel) {
        this.alertlevel = alertlevel;
    }

    public String getAlertsummary() {
        return alertsummary;
    }

    public void setAlertsummary(String alertsummary) {
        this.alertsummary = alertsummary;
    }

    public String getAlertreason() {
        return alertreason;
    }

    public void setAlertreason(String alertreason) {
        this.alertreason = alertreason;
    }

    public String getAlerttype() {
        return alerttype;
    }

    public void setAlerttype(String alerttype) {
        this.alerttype = alerttype;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AlertDictionary that = (AlertDictionary) o;

        return alertname != null ? alertname.equals(that.alertname) : that.alertname == null;
    }

    @Override
    public int hashCode() {
        return alertname != null ? alertname.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "AlertDictionary{" +
                "alertname='" + alertname + '\'' +
                ", alertlevel='" + alertlevel + '\'' +
                ", alertsummary='" + alertsummary + '\'' +
                ", alertreason='" + alertreason + '\'' +
                ", alerttype='" + alerttype + '\'' +
                '}';
    }
}
