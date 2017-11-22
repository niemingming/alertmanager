package com.haier.alertmanager.model;
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
    private String hint;
    /*总数*/
    private long total;

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

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public String getHint() {
        return hint;
    }

    public void setHint(String hint) {
        this.hint = hint;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }
}
