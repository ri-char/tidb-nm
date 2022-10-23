package com.example.tidbnmbackend.pojo;

import com.alibaba.fastjson.JSONObject;

import java.util.Objects;

public class Data {
    private int code;
    private String msg;
    private Object data;

    public Object getData() {
        return data;
    }

    public String getMsg() {
        return msg;
    }

    public int getCode() {
        return code;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public void setCode(int code) {
        this.code = code;
    }

}
