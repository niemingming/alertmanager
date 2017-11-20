package com.haier.alertmanager.service;

import com.google.gson.Gson;
import com.haier.alertmanager.configuration.AlertConfigurationProp;
import com.haier.alertmanager.model.AlertRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @description 日志处理服务
 * @date 2017/11/17
 * @author Niemingming
 */
@Component
public class LogService {
    @Autowired
    private AlertConfigurationProp alertConfigurationProp;
    /**
     * @description 保存失败向日志文件中写入记录。
     * 日志格式为
     * {"index":{"_index":"name","_type":"type","_id":"id"}}
     * {field1:value1}
     * @date 2017/11/17
     * @author Niemingming
     */
    public synchronized void writeLog(AlertRecord record, String index, String id, String filename){
        FileWriter fileWriter = null;
        try {
            fileWriter = new FileWriter(filename,true);
            //按照ES要求格式构造数据
            Map indexMap = new HashMap();
            indexMap.put("_index",index);
            indexMap.put("_type",alertConfigurationProp.esType);
            indexMap.put("_id",id);
            Map first = new HashMap();
            first.put("index",indexMap);
            Gson gson = new Gson();
            String firstStr = gson.toJson(first) + "\r\n";
            Map bodymap = record.toSql().toMap();
            bodymap.remove("_id");
            String secondStr = gson.toJson(bodymap) + "\r\n";
            fileWriter.write(firstStr);
            fileWriter.write(secondStr);
        } catch (IOException e) {
            System.out.println("写入日志文件失败！");
            e.printStackTrace();
        }finally {
            if (fileWriter != null){
                try {
                    fileWriter.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }
}
