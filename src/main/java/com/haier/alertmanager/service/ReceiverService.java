package com.haier.alertmanager.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.haier.alertmanager.container.AlertRecordContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @description 告警信息接收器，监听prometheus发送的告警信息，并记录和转发通知信息。
 * @date 2017/11/16
 * @author Niemingming
 */
@RestController
@RequestMapping("/receive")
public class ReceiverService {
    @Autowired
    private AlertRecordContainer alertRecordContainer;
    
    /**
     * @description 接收alertmanager的告警信息
     * @date 2017/11/16
     * @author Niemingming
     */
    @ResponseBody
    @RequestMapping("/api/v1/alerts")
    public void receiveAlert(HttpServletResponse response, HttpServletRequest request) throws IOException {
        System.out.println("接收告警信息开始。");
        Gson gson = new Gson();
        StringBuilder alerts = new StringBuilder();
        //获取请求输入流
        BufferedReader bufferedReader = request.getReader();
        String string = null;
        //读取请求体内容，获取告警信息
        while((string = bufferedReader.readLine()) != null){
            alerts.append(string);
        }
        //将告警信息转成json数据，并处理告警记录
        JsonArray labels = gson.fromJson(alerts.toString(),JsonArray.class);
        for (int i = 0; i < labels.size(); i++){
            JsonObject alertRecord = labels.get(i).getAsJsonObject();
            alertRecordContainer.addRecord(alertRecord);
        }
        //返回成功状态
        response.setHeader("Content-Type","application/json");
        Map result = new HashMap();
        result.put("status","success");
        response.getWriter().write(gson.toJson(result));
        response.getWriter().flush();
        System.out.println("接收告警信息完成。");
    }
}
