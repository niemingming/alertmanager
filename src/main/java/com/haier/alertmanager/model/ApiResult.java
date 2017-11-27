package com.haier.alertmanager.model;

import java.util.HashMap;
import java.util.Map;

/**
 * @description api调用结果实体抽象
 * @date 2017/11/21
 * @author Niemingming
 */
public class ApiResult {
    /*是否成功*/
    private boolean success = false;
    /*返回编码，成功为0，失败为非0*/
    private int code = 1;
    /*返回数据*/
    private Object data;
    /*提示信息*/
    private String msg;

    public ApiResult(){
        data = new HashMap();
        Map page = new HashMap();
        ((Map)data).put("page",page);
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public Object getData() {
        return data;

    }
    public void setData(Object data) {
        this.data = data;
    }

    public void setTotal(long total) {
        Map page = (Map) ((Map)data).get("page");
        page.put("total",total);
    }
    public void setCurrentPage(long currentPage) {
        Map page = (Map) ((Map)data).get("page");
        page.put("currentPage",currentPage);
    }
}
