package com.haier.alertmanager.container;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.haier.alertmanager.configuration.AlertConstVariable;
import com.haier.alertmanager.model.AlertRecord;
import com.haier.alertmanager.model.MessageReceiverInfo;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @description 告警信息接收者容器
 * @date 2017/11/21
 * @author Niemingming
 */
@Component
public class MessageReceiverContainer {
    /*消息接收者集合*/
    private Map<String,List<MessageReceiverInfo>> messageRecevierMap;
    /**
     * @description 初始化方法，加载配置文件
     * @date 2017/11/21
     * @author Niemingming
     */
    @PostConstruct
    public void init(){
        messageRecevierMap = new HashMap<String, List<MessageReceiverInfo>>();
        //读取配置文件classpath:notifysend/personlist.list
        DefaultResourceLoader defaultResourceLoader = new DefaultResourceLoader();
        Resource resource = defaultResourceLoader.getResource(AlertConstVariable.MESSAGE_RECEIVER_LIST);
        if (resource != null) {
            try {
                BufferedReader bufferedReader = new BufferedReader(new FileReader(resource.getFile()));
                String line = null;
                Gson gson = new Gson();
                while ((line = bufferedReader.readLine()) != null) {
                    line = line.trim();
                    if ("".equals(line)){//如果是空行
                        continue;
                    }
                    //不处理注释行
                    if (line.charAt(0) == '#'){
                        continue;
                    }
                    String[] info = line.replaceAll(" +"," ").split(" ");
                    if (info.length < 3) {
                        continue;
                    }
                    //构建人员接收实体
                    MessageReceiverInfo messageReceiverInfo = new MessageReceiverInfo();
                    messageReceiverInfo.setPersonId(info[0]);
                    messageReceiverInfo.setPersonName(info[1]);
                    JsonObject filters = gson.fromJson(info[2],JsonObject.class);
                    Map filterMap = new HashMap();
                    for (Map.Entry<String,JsonElement> entry:filters.entrySet()){
                        filterMap.put(entry.getKey(),entry.getValue().getAsString());
                    }
                    messageReceiverInfo.setFilters(filterMap);
                    //判断是否已经存入缓存，如果没有就创建一个集合存入，如果有直接添加。
                    List<MessageReceiverInfo> messageReceiverInfos = messageRecevierMap.get(messageReceiverInfo.getId());
                    if (messageReceiverInfos == null) {
                        messageReceiverInfos = new ArrayList<MessageReceiverInfo>();
                        messageRecevierMap.put(messageReceiverInfo.getId(),messageReceiverInfos);
                    }
                    messageReceiverInfos.add(messageReceiverInfo);
                }
            } catch (IOException e) {
                System.out.println("读取人员配置信息失败！【" + AlertConstVariable.MESSAGE_RECEIVER_LIST+ "】");
                e.printStackTrace();
            }
        }
    }
    /**
     * @description 刷新方法
     * @date 2017/11/21
     * @author Niemingming
     */
    public synchronized void refresh(){
        init();
    }
    /**
     * @description 根据告警记录获取接收者
     * @date 2017/11/21
     * @author Niemingming
     */
    public List<MessageReceiverInfo> getReceiversByRecord(AlertRecord record){
        List<MessageReceiverInfo> messageReceiverInfos = new ArrayList<MessageReceiverInfo>();
        Map<String,Object> map = record.getLabels();
        //遍历判断有哪些需要发送消息服务
        for (Map.Entry<String,List<MessageReceiverInfo>> entry : messageRecevierMap.entrySet()){
            MessageReceiverInfo messageReceiverInfo = entry.getValue().get(0);
            //如果根据接收过滤关键属性得出的id值一致，那么这些用户都需要发送消息。
            if (messageReceiverInfo.algExculseId(map).equals(entry.getKey())){
                messageReceiverInfos.addAll(entry.getValue());
            }
        }
        return  messageReceiverInfos;
    }

}
