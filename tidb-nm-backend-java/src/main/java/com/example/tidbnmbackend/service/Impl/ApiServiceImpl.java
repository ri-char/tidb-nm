package com.example.tidbnmbackend.service.Impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.example.tidbnmbackend.pojo.Data;
import com.example.tidbnmbackend.pojo.R;
import com.example.tidbnmbackend.service.ApiService;
import com.example.tidbnmbackend.utils.HttpRequest;
import com.example.tidbnmbackend.utils.SearchTidb;
import com.example.tidbnmbackend.utils.SearchTikv;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.*;

import static com.example.tidbnmbackend.TidbNmBackendApplication.IP;

@Service
public class ApiServiceImpl implements ApiService {
    R r = new R();
    Data data = new Data();
    @Override
    public R getAllFlow(String token) throws Exception {
        ArrayList <JSONObject> data_list = new ArrayList<>();
        HttpRequest httpRequest = new HttpRequest();
        Map <String, String> ip_map = new HashMap<>();
        ip_map.put(IP, "dashboard");
        ArrayList <String> ipList = new ArrayList<>();
        String resp = httpRequest.sendGet("http://"+IP+"/dashboard/api/topology/tidb", token);
        JSONArray jsonArray = JSONArray.parseArray(resp);
        int tidb_i=0;
        for (Object o : jsonArray) {
            JSONObject jsonObject = (JSONObject) o;
            if((int) jsonObject.get("status") != 0){
                ipList.add((String) jsonObject.get("ip")+":"+(int) jsonObject.get("port"));
                ip_map.put((String) jsonObject.get("ip")+":"+(int) jsonObject.get("port"),"tidb-"+tidb_i);
                ip_map.put((String) jsonObject.get("ip")+":"+(int) jsonObject.get("status_port"),"tidb-"+tidb_i);
                tidb_i++;
            }
        }
        for (String i :
                ipList) {
            data_list.add((getAllFlowFromTidb1(i)));
        }

        ipList.clear();
        resp = httpRequest.sendGet("http://"+IP+"/dashboard/api/topology/store",token);
        jsonArray = JSONObject.parseObject(resp).getJSONArray("tikv");
        int tikv_i = 0;
        for (Object o : jsonArray) {
            JSONObject jsonObject = (JSONObject) o;
            if((int) jsonObject.get("status") != 0){
                ip_map.put((String) jsonObject.get("ip")+":"+(int) jsonObject.get("port"),"tikv-"+tikv_i);
                ip_map.put((String) jsonObject.get("ip")+":"+(int) jsonObject.get("status_port"),"tikv-"+tikv_i);
                tikv_i ++;
                ipList.add((String) jsonObject.get("ip")+":"+(int) jsonObject.get("status_port"));
            }
        }
        jsonArray = JSONObject.parseObject(resp).getJSONArray("tiflash");
        int tiflash_i = 0;
        for (Object o : jsonArray) {
            JSONObject jsonObject = (JSONObject) o;
            if((int) jsonObject.get("status") != 0){
                ip_map.put((String) jsonObject.get("ip")+":"+(int) jsonObject.get("port"),"tiflash-"+ tiflash_i);
                ip_map.put((String) jsonObject.get("ip")+":"+(int) jsonObject.get("status_port"),"tiflash-"+ tiflash_i);
                tiflash_i++;
            }
        }
        for (String i :
                ipList) {
            data_list.add((getAllFlowFromTikv(i,token)));
        }
        int other_i=0;
        for (JSONObject o:
             data_list) {
            String replace_ip = ip_map.get((String) o.get("current_ip"));
            if(replace_ip!=null){
                o.put("current_ip", replace_ip);
            }
            List data_ip_list = (List) o.get("ip_list");
            for(Object ip_item:data_ip_list){
                List data_ip_list_detail = (List) ((JSONObject)ip_item).get("detail");
                if(data_ip_list_detail==null){
                    continue;
                }
                final ArrayList<Integer> rx_tx=new ArrayList<>();
                rx_tx.add(0);
                rx_tx.add(0);
                data_ip_list_detail.removeIf((detail_item)->{
                    String replace_ip_ = ip_map.get((String) ((JSONObject)detail_item).get("ip"));
                    if(replace_ip_!=null){
                        ((JSONObject)detail_item).put("ip", replace_ip_);
                        return false;
                    }else{
                        rx_tx.set(0, rx_tx.get(0)+(int)((JSONObject)detail_item).get("rx"));
                        rx_tx.set(1, rx_tx.get(1)+(int)((JSONObject)detail_item).get("tx"));
                        return true;
                    }
                });
                if(rx_tx.get(0)!=0||rx_tx.get(1)!=0) {
                    JSONObject other = new JSONObject();
                    other.put("ip", "other-" + other_i);
                    other.put("rx", rx_tx.get(0));
                    other.put("tx", rx_tx.get(1));
                    data_ip_list_detail.add(other);
                    other_i++;
                }
            }
        }
        System.out.println(ipList);
        data.setMsg("success");
        data.setCode(0);
        data.setData(data_list);
        r.setStatus(200);
        r.setData(data);
        return r;
    }

    @Override
    public JSONObject getAllFlowFromTidb1(String ip) throws SQLException, ClassNotFoundException {
        try {
            JSONObject db_resp = SearchTidb.SearchFromTidb(ip);
            JSONObject simple_resp = new JSONObject(new TreeMap<String, Object>());
            ArrayList<String> tmp = new ArrayList<>();
            ArrayList<JSONObject> ip_list = new ArrayList<>();
            ArrayList<JSONObject> detail_list = new ArrayList<>();
            for (Map.Entry<String, Object> i : db_resp.entrySet()) {
                tmp.add(i.getKey());
            }
            tmp.sort(Comparator.naturalOrder());
            String pre = tmp.get(0);
            for (String i :
                    tmp) {
                JSONObject jsonObject_tmp = new JSONObject(new TreeMap<String, Object>());
                if (Objects.equals(i.split(":")[0], "127.0.0.1")) {
                    i = IP;
                }

                jsonObject_tmp.put("ip", i);
                jsonObject_tmp.put("tx",JSONObject.parseObject(String.valueOf(db_resp.get(i))).get("tx"));
                jsonObject_tmp.put("rx",JSONObject.parseObject(String.valueOf(db_resp.get(i))).get("rx"));
                if (Objects.equals(pre.split(":")[0], i.split(":")[0])){
                    detail_list.add(jsonObject_tmp);
                    simple_resp.put("detail", detail_list);
                }
                else{
                    ip_list.add(simple_resp);
                    simple_resp = new JSONObject(new TreeMap<String, Object>());
                    detail_list =  new ArrayList<>();
                    detail_list.add(jsonObject_tmp);
                }
                pre = i;
            }
            if (null != simple_resp.get("detail")) {
                ip_list.add(simple_resp);
            }

            JSONObject resp = new JSONObject(new TreeMap<String, Object>());
            resp.put("current_ip",ip);
            resp.put("type","tidb");
            resp.put("ip_list",ip_list);
            return resp;
        }
        catch (Exception e) {
            return null;
        }


    }

    @Override
    public R getAllFlowFromTidb(String ip) throws SQLException, ClassNotFoundException {
        try {
            JSONObject db_resp = SearchTidb.SearchFromTidb(ip);
            JSONObject simple_resp = new JSONObject(new TreeMap<String, Object>());
            ArrayList<String> tmp = new ArrayList<>();
            ArrayList<JSONObject> ip_list = new ArrayList<>();
            ArrayList<JSONObject> detail_list = new ArrayList<>();
            for (Map.Entry<String, Object> i : db_resp.entrySet()) {
                tmp.add(i.getKey());
            }
            tmp.sort(Comparator.naturalOrder());
            String pre = tmp.get(0);
            for (String i :
                    tmp) {
                JSONObject jsonObject_tmp = new JSONObject(new TreeMap<String, Object>());
                jsonObject_tmp.put("ip",i);
                jsonObject_tmp.put("type","tidb");
                jsonObject_tmp.put("tx",JSONObject.parseObject(String.valueOf(db_resp.get(i))).get("tx"));
                jsonObject_tmp.put("rx",JSONObject.parseObject(String.valueOf(db_resp.get(i))).get("rx"));

                if (Objects.equals(pre.split(":")[0], i.split(":")[0])){
                    detail_list.add(jsonObject_tmp);
                    simple_resp.put("detail", detail_list);
                }
                else{
                    ip_list.add(simple_resp);
                    simple_resp = new JSONObject(new TreeMap<String, Object>());
                    detail_list =  new ArrayList<>();
                    detail_list.add(jsonObject_tmp);
                }
                pre = i;
            }
            ip_list.add(simple_resp);
            JSONObject resp = new JSONObject(new TreeMap<String, Object>());
            resp.put("current_ip",ip);
            resp.put("ip_list",ip_list);
            data.setCode(0);
            data.setMsg("success");
            data.setData(resp);
            r.setStatus(200);
            r.setData(data);
        }
        catch (Exception e) {
            data.setCode(9999);
            data.setMsg("error");
            data.setData(null);
            r.setStatus(200);
            r.setData(data);
        }
        return r;
    }

    @Override
    public JSONObject getAllFlowFromTikv(String ip, String token) {
        try {
            SearchTikv searchTikv = new SearchTikv();
            JSONObject db_resp = searchTikv.SearchFromTikv(ip,token);
            JSONObject simple_resp = new JSONObject(new TreeMap<String, Object>());
            ArrayList<String> tmp = new ArrayList<>();
            ArrayList<JSONObject> ip_list = new ArrayList<>();
            ArrayList<JSONObject> detail_list = new ArrayList<>();
            for (Map.Entry<String, Object> i : db_resp.entrySet()) {
                tmp.add(i.getKey());
            }
            tmp.sort(Comparator.naturalOrder());
            String pre = tmp.get(0);
            for (String i :
                    tmp) {
                JSONObject jsonObject_tmp = new JSONObject(new TreeMap<String, Object>());
                if (Objects.equals(i.split(":")[0], "127.0.0.1")) {
                    i = IP;
                }
                jsonObject_tmp.put("ip",i);

                jsonObject_tmp.put("tx",JSONObject.parseObject(String.valueOf(db_resp.get(i))).get("tx"));
                jsonObject_tmp.put("rx",JSONObject.parseObject(String.valueOf(db_resp.get(i))).get("rx"));

                if (Objects.equals(pre.split(":")[0], i.split(":")[0])){
                    detail_list.add(jsonObject_tmp);
                    simple_resp.put("detail", detail_list);
                }
                else{
                    if(simple_resp.size() != 0) {
                        ip_list.add(simple_resp);
                    }
                    simple_resp = new JSONObject(new TreeMap<String, Object>());
                    detail_list =  new ArrayList<>();
                    detail_list.add(jsonObject_tmp);
                }
                pre = i;
            }
            if(simple_resp.size() != 0) {
                ip_list.add(simple_resp);
            }
            JSONObject resp = new JSONObject(new TreeMap<String, Object>());
            resp.put("current_ip",ip);
            resp.put("type","tikv");

            resp.put("ip_list",ip_list);
            return resp;
        }
        catch (Exception e) {
            return null;
        }
    }
}

