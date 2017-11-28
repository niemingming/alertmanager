package com.haier.alertmanager.notifyhandlers;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.haier.alertmanager.configuration.AlertConfigurationProp;
import com.haier.alertmanager.container.AlertDictionaryContainer;
import com.haier.alertmanager.container.AlertRecordContainer;
import com.haier.alertmanager.container.MessageReceiverContainer;
import com.haier.alertmanager.model.AlertRecord;
import com.haier.alertmanager.model.MessageReceiverInfo;
import com.mongodb.DBObject;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @description 通用告警发送处理器，规则为接收到消息，判断是否已经发送过，如果没有立刻发送。不考虑其他逻辑
 * @date 2017/11/17
 * @author Niemingming
 */
@Order(999)//将该发送处理器设置为最低级别，便于客户规则覆盖
@Component
public class NotifySendCommonHandler implements INotifySendHanlder {
    @Autowired
    private AlertRecordContainer alertRecordContainer;
    @Autowired
    private AlertConfigurationProp alertConfigurationProp;
    /*消息接收者容器*/
    @Autowired
    private MessageReceiverContainer messageReceiverContainer;
    /*通知重发时间间隔，单位秒*/
    private long resendinterval;
    /*MongoDB数据库操作*/
    @Autowired
    private MongoTemplate mongoTemplate;
    /**数据字典*/
    @Autowired
    private AlertDictionaryContainer alertDictionaryContainer;

    /**
     * @description 初始化方法
     * @date 2017/11/20
     * @author Niemingming
     */
    @PostConstruct
    public  void  init(){
        String interval = alertConfigurationProp.resendinterval;
        if ("".equals(interval)){
            interval = "1h";//默认轮询间隔是1小时
        }
        char flag = interval.charAt(interval.length() - 1);
        resendinterval = 0l;
        //天数
        if (flag == 'd'){
            resendinterval = Long.parseLong(interval.substring(0,interval.length()-1));
            resendinterval *= 24*60*60;
        }else if (flag == 'h'){//小时
            resendinterval = Long.parseLong(interval.substring(0,interval.length()-1));
            resendinterval *= 60*60;
        }else if (flag == 'm'){//分钟
            resendinterval = Long.parseLong(interval.substring(0,interval.length()-1));
            resendinterval *= 60;
        }else if (flag == 's'){//秒
            resendinterval = Long.parseLong(interval.substring(0,interval.length()-1));
        }else {//默认单位是秒
            resendinterval = Long.parseLong(interval);
        }
    }
    @Override
    public boolean supportRule(AlertRecord record) {
        //总是返回true，表示可以处理所有的告警信息。
        return true;
    }

    @Override
    public synchronized void sendNotify(AlertRecord record) {
        //修改了线程同步的，进入后重新校验上次通知时间，防止因发送间隔过短造成的消息重复发送
        DBObject recordRes = mongoTemplate.getCollection(alertConfigurationProp.alertRecordTalbeName).findOne(record.toQuerySqlById());
        record = new AlertRecord(recordRes);
        long now = new Date().getTime()/1000;
        if (record.getLastNotifyTime() == 0
                || now - record.getLastNotifyTime() >= resendinterval
                || record.getEndsAt() > record.getLastNotifyTime()){
            String message = alertDictionaryContainer.getNotifyMessage(record);
            record.setMessage(message);

            //将发送规则下沉到具体规则实现中，这里规则是当没有发送过通知，或者发送通知达到重发间隔时发送消息通知，或者消息结束时间大于上次通知时间，表示还没发送过。
            List<MessageReceiverInfo> messageReceiverInfos = messageReceiverContainer.getReceiversByRecord(record);
            if (messageReceiverInfos.isEmpty()) {
                System.out.println("告警信息【" + record.getAlertname() + "】未配置消息接收者！");
            }else {
                //发送消息，先做成同步发送，看一下效果
                HttpClient client = HttpClients.createDefault();
                HttpPost post = new HttpPost(alertConfigurationProp.messageUri);
                //设置访问超时时间
                RequestConfig config = RequestConfig.custom().setConnectTimeout(10000)
                        .setSocketTimeout(10000).setConnectionRequestTimeout(10000).build();
                post.setConfig(config);
                //获取所有请求人员
               List targets = new ArrayList();
                for (MessageReceiverInfo messageReceiverInfo:messageReceiverInfos){
                    targets.add(messageReceiverInfo.getPersonId());
                }

                //构建参数
                notifyParam param = new notifyParam();
                param.subject = record.getProject() + "-" + record.getAlertname();
                param.content = record.getMessage();
                param.targets = targets;
                param.id = record.getAlertId();
                try {
                    //改为请求体格式
                    StringEntity entity = new StringEntity(new Gson().toJson(param),"utf-8");
                    entity.setContentType("application/json;charset=utf-8");
                    post.setEntity(entity);
                    //执行请求
                    HttpResponse response = client.execute(post);
                    String result = EntityUtils.toString(response.getEntity());
                    JsonObject res = new Gson().fromJson(result,JsonObject.class);
                    if (res.get("isSuccess").getAsBoolean()) {
                        record.setLastNotifyTime(new Date().getTime()/1000);
                        alertRecordContainer.updateRecord(record);
                        System.out.println("消息已经发送");
                    }else {
                        System.out.println("消息发送失败！ "+ res.get("msg").getAsString());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("调用消息服务失败！");
                }
            }
        }

    }
    /**
     * @description 告警通知参数类
     * @date 2017/11/27
     * @author Niemingming
     */
    public class notifyParam {
        /*通知主题*/
        public String subject;
        /*通知目标*/
        public List targets;
        /*通知内容*/
        public String content;
        /*通知类型1：既发送邮件又发送iHaier消息*/
        public int messageType = 1;
        /*告警id*/
        public String id;
        /**
         * @description 返回post请求参数
         * @date 2017/11/27
         * @author Niemingming
         */
        public List<NameValuePair> toParams(){
            List<NameValuePair> pairs = new ArrayList<NameValuePair>();
            NameValuePair subjectPair = new BasicNameValuePair("subject",subject);
            NameValuePair contentPair = new BasicNameValuePair("content",content);
            NameValuePair targetsPair = new BasicNameValuePair("targets",targets.toString());
            NameValuePair messageTypePair = new BasicNameValuePair("messageType",messageType+"");
            pairs.add(subjectPair);
            pairs.add(contentPair);
            pairs.add(targetsPair);
            pairs.add(messageTypePair);
            return pairs;
        }

    }
}
