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
    /*告警描述，可以使用变量占位符${labels.name},${startsAt}*/
    private String description;
    /*告警建议 */
    private String suggest;
    /*告警类型：机器、应用、中间件等*/
    private String alertCategory;

    public AlertDictionary() {
    }
    public AlertDictionary(DBObject dict) {
        alertname = dict.get("alertname") + "";
        alertlevel = dict.get("level")+"";
        description = dict.get("description")+"";
        suggest = dict.get("suggest")+"";
        alertCategory = dict.get("alertCategory")+"";
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSuggest() {
        return suggest;
    }

    public void setSuggest(String suggest) {
        this.suggest = suggest;
    }

    public String getAlertCategory() {
        return alertCategory;
    }

    public void setAlertCategory(String alertCategory) {
        this.alertCategory = alertCategory;
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
                ", description='" + description + '\'' +
                ", suggest='" + suggest + '\'' +
                ", alertCategory='" + alertCategory + '\'' +
                '}';
    }
}
