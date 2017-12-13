package com.haier.alertmanager.test.controller;

import com.haier.alertmanager.model.AlertRecord;
import com.haier.alertmanager.notifyhandlers.INotifyStorageHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Date;
import java.util.UUID;

/**
 * @description es转储压力测试
 * @date 2017/12/12
 * @author Niemingming
 */
@Controller
@RequestMapping("/test")
public class ESSaveTest {
    @Autowired
    private INotifyStorageHandler notifyStorageHandler;
    /**
     * @description 转储压力测试默认是1000次
     * @date 2017/12/12
     * @author Niemingming
     */
    @ResponseBody
    @RequestMapping("/testSave")
    public String testExSave(@RequestParam(name="cs",defaultValue = "1000")int cs){

        for (int i = 0; i < cs; i++){
            AlertRecord record = new AlertRecord();
            record.setId(UUID.randomUUID().toString());
            record.setAlertname("ylcs");
            record.setTimes(11);
            record.setReason("??");
            record.setAlertCategory("app");
            record.setUnit("%");
            record.setSuggest("无");
            record.setDescription("wu");
            record.setProject("hlht");
            record.setLevel("info");
            record.setLastNotifyTime(new Date().getTime());
            record.setLastReceiveTime(new Date().getTime());
            record.setStatus("firing");
            record.setStartsAt(new Date().getTime());
            record.setEndsAt(new Date().getTime());
            notifyStorageHandler.saveRecord(record);
        }
        return "success";
    }
}
