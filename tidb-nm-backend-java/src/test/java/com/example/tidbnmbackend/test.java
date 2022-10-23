package com.example.tidbnmbackend;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.Feature;
import com.example.tidbnmbackend.utils.HttpRequest;

import java.sql.*;
import java.util.*;
import java.util.Date;

public class test{
    public static void main(String[] args) throws Exception {

        HttpRequest httpRequest = new HttpRequest();
        String res = httpRequest.sendPost2("http://10.71.205.155:2379/dashboard/api/user/login",JSONObject.parseObject("{username: \"root\", password: \"\", type: 0}"));
        System.out.println(res);
//        String url = "{\"198.19.0.5:30\":{\"tx\":0,\"rx\":509},\"198.19.0.4:40\":{\"tx\":0,\"rx\":509},\"198.19.0.5:59414\":{\"tx\":0,\"rx\":509},\"198.19.0.5:52550\":{\"tx\":0,\"rx\":8736},\"198.19.0.5:41430\":{\"tx\":0,\"rx\":3659},\"198.19.0.5:59786\":{\"tx\":0,\"rx\":1435},\"198.19.0.5:54880\":{\"tx\":0,\"rx\":305},\"198.19.0.5:39656\":{\"tx\":0,\"rx\":349},\"198.19.0.5:52560\":{\"tx\":0,\"rx\":931}}";
//        JSONObject db_resp = JSONObject.parseObject(url);
//        JSONObject simple_resp = new JSONObject(new TreeMap<String, Object>());
//        ArrayList<String> tmp = new ArrayList<>();
//        ArrayList<JSONObject> ip_list = new ArrayList<>();
//        ArrayList<JSONObject> detail_list = new ArrayList<>();
//        for (Map.Entry<String, Object> i : db_resp.entrySet()) {
//            tmp.add(i.getKey());
//        }
//        tmp.sort(Comparator.naturalOrder());
//        String pre = tmp.get(0);
//        for (String i :
//                tmp) {
//            JSONObject jsonObject_tmp = new JSONObject(new TreeMap<String, Object>());
//            jsonObject_tmp.put("ip",i);
//            jsonObject_tmp.put("tx",JSONObject.parseObject(String.valueOf(db_resp.get(i))).get("tx"));
//            jsonObject_tmp.put("rx",JSONObject.parseObject(String.valueOf(db_resp.get(i))).get("rx"));
//
//            if (Objects.equals(pre.split(":")[0], i.split(":")[0])){
//                detail_list.add(jsonObject_tmp);
//                simple_resp.put("detail", detail_list);
//            }
//            else{
//                ip_list.add(simple_resp);
//                simple_resp = new JSONObject(new TreeMap<String, Object>());
//                detail_list =  new ArrayList<>();
//                detail_list.add(jsonObject_tmp);
//            }
//            pre = i;
//        }
//
//        ip_list.add(simple_resp);
//        JSONObject resp = new JSONObject(new TreeMap<String, Object>());
//        resp.put("ip_list",ip_list);
//        System.out.println(JSONObject.toJSONString(resp));
    }
}