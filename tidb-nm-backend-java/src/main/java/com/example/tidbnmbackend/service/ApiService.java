package com.example.tidbnmbackend.service;

import com.alibaba.fastjson.JSONObject;
import com.example.tidbnmbackend.pojo.R;

import java.sql.SQLException;

public interface ApiService {
    R getAllFlow(String token) throws Exception;
    JSONObject getAllFlowFromTidb1(String ip) throws SQLException, ClassNotFoundException;
    R getAllFlowFromTidb(String ip) throws SQLException, ClassNotFoundException;
    JSONObject getAllFlowFromTikv(String ip, String token) ;
}
