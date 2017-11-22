package com.haier.alertmanager.model;

import com.haier.alertmanager.configuration.AlertConstVariable;
import com.sun.org.apache.xerces.internal.impl.dv.util.HexBin;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @description 信息接收人实体
 * @date 2017/11/21
 * @author Niemingming
 */
public class MessageReceiverInfo {
    /*匹配结果id*/
    private String id;
    /*人员id*/
    private String personId;
    /*人员名称*/
    private String personName;
    /*过滤规则*/
    private Map<String,Object> filters;

    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
    public String getPersonId() {
        return personId;
    }
    public void setPersonId(String personId) {
        this.personId = personId;
    }
    public String getPersonName() {
        return personName;
    }
    public void setPersonName(String personName) {
        this.personName = personName;
    }
    public Map getFilters() {
        return filters;
    }
    public void setFilters(Map filters) {
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MessageReceiverInfo that = (MessageReceiverInfo) o;

        return id != null ? id.equals(that.id) : that.id == null;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "MessageReceiverInfo{" +
                "id='" + id + '\'' +
                ", personId='" + personId + '\'' +
                ", personName='" + personName + '\'' +
                ", filters=" + filters +
                '}';
    }
}
