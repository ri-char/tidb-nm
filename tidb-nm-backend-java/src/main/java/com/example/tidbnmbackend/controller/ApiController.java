package com.example.tidbnmbackend.controller;

import com.alibaba.fastjson.JSONObject;
import com.example.tidbnmbackend.pojo.Data;
import com.example.tidbnmbackend.pojo.R;
import com.example.tidbnmbackend.service.Impl.ApiServiceImpl;
import com.example.tidbnmbackend.utils.HttpRequest;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Map;

import static com.example.tidbnmbackend.TidbNmBackendApplication.IP;

@RestController
@CrossOrigin
@RequestMapping("/api")
public class ApiController {
    private final ApiServiceImpl apiService;
    R r = new R();
    Data data = new Data();
    public ApiController(ApiServiceImpl apiService) {
        this.apiService = apiService;
    }

    @RequestMapping("/login")
    public R Login(@RequestParam Map<String, Object> map, HttpServletRequest request) throws Exception {
        HttpSession httpSession = request.getSession();
        HttpRequest httpRequest = new HttpRequest();
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("username",map.get("username"));
        jsonObject.put("password",map.get("password"));
        jsonObject.put("type",0);
        try {
            String resp = httpRequest.sendPost2("http://"+IP+"/dashboard/api/user/login",jsonObject);
            data.setData(null);
            data.setCode(0);
            data.setMsg("登录成功");
            r.setStatus(200);
            r.setData(data);
            httpSession.setAttribute("is_login","true");
            httpSession.setAttribute("token",JSONObject.parseObject(resp).get("token"));
        }
        catch (Exception e){
            e.printStackTrace();
            data.setData(null);
            data.setCode(1000);
            data.setMsg("error");
            r.setStatus(200);
            r.setData(data);
        }
        return r;
    }
    @RequestMapping("/getAllFlow")
    public R getAllFlow(@RequestParam Map<String, Object> map, HttpServletRequest request){
        HttpSession httpSession = request.getSession();

        if(null==httpSession.getAttribute("is_login")){
            data.setData(null);
            data.setCode(1001);
            data.setMsg("请先登录");
            r.setStatus(200);
            r.setData(data);
            return r;
        }

        try{
            r = apiService.getAllFlow((String) httpSession.getAttribute("token"));
        }
        catch (Exception e){
            e.printStackTrace();
            r.setStatus(200);
            data.setCode(9999);
            data.setMsg("系统异常");
            data.setData(new JSONObject());
            r.setData(data);
        }
        return r;
    }
    @RequestMapping("/getAllFlowFromTidb")
    public R getAllFlowFromTidb(@RequestParam Map<String, Object> map, HttpServletRequest request){
        HttpSession httpSession = request.getSession();
        if(null==httpSession.getAttribute("is_login")){
            data.setData(null);
            data.setCode(1001);
            data.setMsg("请先登录");
            r.setStatus(200);
            r.setData(data);
            return r;
        }
        String ip = (String)map.get("ip");
        try{
            r = apiService.getAllFlowFromTidb(ip);
        }
        catch (Exception e){

            r.setStatus(200);
            data.setCode(9999);
            data.setMsg("系统异常");
            data.setData(new JSONObject());
            r.setData(data);
        }
        return r;
    }
}
