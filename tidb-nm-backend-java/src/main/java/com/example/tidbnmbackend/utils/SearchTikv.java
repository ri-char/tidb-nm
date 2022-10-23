package com.example.tidbnmbackend.utils;

import com.alibaba.fastjson.JSONObject;

public class SearchTikv {
    HttpRequest httpRequest = new HttpRequest();
    public JSONObject SearchFromTikv(String ip,String token) throws Exception {
        String url = "http://"+ip+"/net";
        String resp = httpRequest.sendGet(url,token);
        return JSONObject.parseObject(resp);
    }
}
