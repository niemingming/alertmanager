package com.haier.alertmanager.notifyhandlers;

import com.google.gson.Gson;
import com.haier.alertmanager.configuration.AlertConfigurationProp;
import com.haier.alertmanager.model.AlertRecord;
import com.haier.alertmanager.service.LogService;
import org.apache.http.HttpHost;
import org.apache.http.entity.StringEntity;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.ResponseListener;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @description ES转储处理器，将告警历史数据转储到ES中，将ES转储作为默认方式，级别设置为最低
 * @date 2017/11/17
 * @author Niemingming
 */
@Order(999)
@Component
public class ESNotifyStorageHandler implements INotifyStorageHandler {
    @Autowired
    private AlertConfigurationProp alertConfigurationProp;
    @Autowired
    private MongoTemplate mongoTemplate;

    /*日志处理服务*/
    @Autowired
    private LogService logService;
    /**
     * @description 默认ES实现，支持所有记录
     * @date 2017/11/17
     * @author Niemingming
     */
    @Override
    public boolean shouldResolve(AlertRecord record) {
        return true;
    }
    /**
     * @description 通过Http转储到ES中，索引为recordId_yyyyMM
     * 类型为alertmanager,可通过配置
     * @date 2017/11/17
     * @author Niemingming
     */
    @Override
    public void saveRecord(AlertRecord record) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(alertConfigurationProp.esDataPattern);
        HttpHost[] hosts = new HttpHost[alertConfigurationProp.esHostNames.size()];
        for (int i = 0; i < hosts.length; i++ ){
            hosts[i] = HttpHost.create(alertConfigurationProp.esHostNames.get(i));
        }
        //创建ES请求客户端
        RestClient restClient = RestClient.builder(hosts).build();
        //请求参数
        Map params = new HashMap();
        String dateformat = simpleDateFormat.format(new Date());
        //计算ES数据索引,ESid必须是小写字母
        String index = alertConfigurationProp.indexpre + dateformat;
        Gson requestBody = new Gson();
        //ES数据内容，id作为ES关键字不能再数据体中出现
        Map bodymap = record.toSql().toMap();
        bodymap.remove("_id");
        bodymap.remove("alertId");//删除告警id，该值作为ESid使用

        String body = requestBody.toJson(bodymap);
        //获取ES数据id
        String id = record.getAlertId();
        //获取ES保存接口endpoint
        String endpoint = "/" + index + "/" + alertConfigurationProp.esType + "/" + id;
        try {
            restClient.performRequestAsync("PUT",endpoint,params,new StringEntity(body),new EsResponseLisnter(record,index,id,dateformat));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        //错误信息存在日志文件中，我们需要删除记录
        mongoTemplate.getCollection(alertConfigurationProp.alertRecordTalbeName).remove(record.toQuerySqlById());

    }
    /**
     * @description 异步请求回调函数，防止因网络请求阻塞程序运行
     * @date 2017/11/17
     * @author Niemingming
     */
    public class EsResponseLisnter implements ResponseListener {

        private AlertRecord record;
        private String index;
        private String id;
        private String dateformat;
        public EsResponseLisnter(AlertRecord record,String index,String id,String dateformat){
            this.record = record;
            this.index = index;
            this.id = id;
            this.dateformat = dateformat;
        }

        @Override
        public void onSuccess(Response response) {
            int code = response.getStatusLine().getStatusCode();
            if (code >= 200 && code < 400){
                System.out.println("【" + record.getAlertname() + "】转储ES成功");
            }else {
                System.out.println("告警信息转储失败，转储信息如下：");
                System.out.println(record.toString());
                logService.writeLog(record,index,id,dateformat + ".log");
            }
        }

        @Override
        public void onFailure(Exception e) {
            System.out.println("转储告警信息过程出现异常，信息如下：");
            System.out.println(record);
            e.printStackTrace();
            logService.writeLog(record,index,id,dateformat + ".log");
        }
    }


}
