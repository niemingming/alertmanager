package com.haier.alertmanager.model;

import com.haier.alertmanager.configuration.AlertConstVariable;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.sun.org.apache.xerces.internal.impl.dv.util.HexBin;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

/**
 * @description 告警通知的白名单对象，至少有startsAt和endsAt字段
 * @date 2017/11/16
 * @author Niemingming
 */
public class AlertExcluse {
    /*白名单生效时间*/
    private long startsAt;
    /*白名单失效时间*/
    private long endsAt;
    /*白名单id，有我们自己生成作为唯一标识，同样采用MD5算法生成*/
    private String id;
    /*白名单过滤字段，该字段不做限制*/
    private Map<String,Object> filters;
    private long max;

    public AlertExcluse() {
        //将最大值设置为1000年以后。用于表示无限大
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR,3000);
        max = cal.getTimeInMillis()/1000;
    }
    /**
     * @description 通过数据库获取的白名单，去掉id，startsAt和endsAt字段
     * @date 2017/11/16
     * @author Niemingming
     */
    public AlertExcluse(DBObject excluse) {
        this();
        //从查询结果中，删除折扇项内容
        id = excluse.removeField("_id")+"";
        startsAt = excluse.get("startsAt") == null ? 0l : (Long)excluse.removeField("startsAt");
        endsAt = excluse.get("endsAt") == null ? max : (Long)excluse.removeField("endsAt");
        //将剩余key值作为白名单过滤字段
        filters = excluse.toMap();
    }
    /**
     * @description 根据传入参数生成白名单对象
     * @date 2017/11/16
     * @author Niemingming
     */
    public AlertExcluse(Map excluse){
        this();
        startsAt = excluse.get("startsAt") == null ? 0l : (Long)excluse.remove("startsAt");
        endsAt = excluse.get("endsAt") == null ? max : (Long)excluse.remove("endsAt");
        //计算id
        setFilters(excluse);
    }

    public long getStartsAt() {
        return startsAt;
    }

    public void setStartsAt(long startsAt) {
        this.startsAt = startsAt;
    }

    public long getEndsAt() {
        return endsAt;
    }

    public void setEndsAt(long endsAt) {
        this.endsAt = endsAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Map<String, Object> getFilters() {
        return filters;
    }

    public void setFilters(Map<String, Object> filters) {
        this.filters = filters;
        setId(algExculseId(filters));
    }

    public String algExculseId(Map<String, Object> data) {
        //计算id
        List<String> algfields = new ArrayList<String>();
        for (String field : filters.keySet()){
            algfields.add(field+"="+data.get(field));
        }
        try {
            MessageDigest digest = MessageDigest.getInstance(AlertConstVariable.ALERT_ID_ALG);
            return HexBin.encode(digest.digest(algfields.toString().getBytes("UTF-8")));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            System.out.println("系统不支持配置的计算算法");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            System.out.println("不支持的字符串编码格式");
        }
        return "null";
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

        object.putAll(filters);
        return object;
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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AlertExcluse that = (AlertExcluse) o;

        return id != null ? id.equals(that.id) : that.id == null;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "AlertExcluse{" +
                "startsAt=" + startsAt +
                ", endsAt=" + endsAt +
                ", id='" + id + '\'' +
                ", filters=" + filters +
                '}';
    }
}
