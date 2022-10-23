package com.example.tidbnmbackend.utils;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import java.sql.*;
public class SearchTidb {
    public static JSONObject SearchFromTidb(String ip) throws ClassNotFoundException, SQLException{
        Class.forName("com.mysql.cj.jdbc.Driver");
        String url = "jdbc:mysql://"+ip+"/test?useUnicode=true&character=utf8&useSSL=false&serverTimezone=UTC";
        String username = "root";
        String password = "";
        Connection connection = DriverManager.getConnection(url,username,password);
        Statement statement = connection.createStatement();
        String sql = "select @@test1;";
        ResultSet resultSet = statement.executeQuery(sql);
        StringBuilder res = new StringBuilder();
        while (resultSet.next()){
            res.append(resultSet.getString(1));
        }
        resultSet.close();
        statement.close();
        connection.close();
        return JSON.parseObject(String.valueOf(res));
    }
}
