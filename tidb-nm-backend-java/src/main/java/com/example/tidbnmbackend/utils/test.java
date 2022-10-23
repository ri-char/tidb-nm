package com.example.tidbnmbackend.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.example.tidbnmbackend.utils.HttpRequest;

import java.sql.*;
import java.util.Date;

public class test    {


    public static void main(String[] args) throws Exception {
        SearchTikv searchTikv = new SearchTikv();
        JSONObject resp = searchTikv.SearchFromTikv("192.168.113.208:20181","eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJleHAiOjE2NjY1MzU4MTUsIm9yaWdfaWF0IjoxNjY2NDQ5NDE1LCJwIjoiS2p1cGlCWEhEUjB5QWZXZlJwc0F3eDhxSkQwY0xzNHF3MHpGQWVJV2MwLzJkZGNOSlVpTGRDSi90UkduUzVVTmNXbThMYkd5U21RTnZDcVZ5eEd5TDI5Vk5KaFNzUEw4cjRINFh3UDl0MXZlUmJQRmpjYVpaWDZET0VxbDlJMitPTXFHWTc4Yk0yLzg2aXpnRE1HUVlUNHMxS0FWWnpjTE51YXhobzUzVDlxYTUwdUppQVRXQmRBZWJ4L1B4VzMrMWs3dzNWekRVQnVxNnlzOWpuckpzeTUvNDVZTXVNRUNZT3VhdS9EMnAzS1U5anpjbFRlQ1ByQkttZ1RrK3dxbkhRRXVjMEEyazdZSzdoNGcxZm89In0.YxNan0Vpu1KDhoQeh3TT93XeVWDYOA_aeSQznXGsxxY");
    }

}