package com.haier.alertmanager.service;

import com.google.gson.*;
import com.haier.alertmanager.configuration.AlertConfigurationProp;
import com.haier.alertmanager.configuration.AlertConstVariable;
import com.haier.alertmanager.container.AlertDictionaryContainer;
import com.haier.alertmanager.container.AlertExcluseContainer;
import com.haier.alertmanager.container.MessageReceiverContainer;
import com.haier.alertmanager.model.ApiResult;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @description 对外提供调用的api服务
 * @date 2017/11/16
 * @author Niemingming
 */
@RestController
@RequestMapping("/api")
public class ApiService {

    @Autowired
    private AlertDictionaryContainer alertDictionaryContainer;
    @Autowired
    private AlertExcluseContainer alertExcluseContainer;
    /*接收人员容器*/
    @Autowired
    private MessageReceiverContainer messageReceiverContainer;
    @Autowired
    private MongoTemplate mongoTemplate;
    /*配置信息*/
    @Autowired
    private AlertConfigurationProp alertConfigurationProp;
    /**
     * @description 刷新系统缓存，主要刷新白名单和数据字典。
     * @date 2017/11/20
     * @author Niemingming
     */
    @ResponseBody
    @RequestMapping("/refresh/{flag}")
    public String refreshCache(@PathVariable String flag){
        if ("excluse".equals(flag)){
            //指刷新白名单
            alertExcluseContainer.refresh();
        }else if("dict".equals(flag)){
            //指刷新配置信息
            alertDictionaryContainer.refresh();
        }else if("receive".equals(flag)){
            messageReceiverContainer.refresh();
        }else{
            alertExcluseContainer.refresh();
            alertDictionaryContainer.refresh();
            messageReceiverContainer.refresh();
        }
        return "success";
    }
    /**
     * @description 查询当前告警列表,POST请求
     * POST /api/queryAlertingList
     * {
     *     pageinfo:{//分页信息如果不传，表示不分页
     *         from:10, //从第几条开始，默认是0
     *         size:10  //查询多少条，默认是10，
     *     },
     *     query:{//查询条件，遵循mongo的查询格式
     *          alertname:"testalert",
     *          "labels.job":"tomcat",
     *          times:{$gte:"10"}
     *     }
     * }
     * 返回数据格式为：
     * {
     *     success:bool(true/false),//表示是否成功
     *     code:0/1 ,//执行结果编码，目前只有0成功，1失败
     *     total:long,//表示查询到的记录数
     *     data:[], //表示返回的记录列表详情
     *     hint:string //服务器返回的提示信息，一般在访问失败时给出。
     * }
     * @date 2017/11/21
     * @author Niemingming
     */
    @ResponseBody
    @RequestMapping(value = "/queryAlertingList",method= RequestMethod.POST)
    public void queryAlertingList(HttpServletRequest request, HttpServletResponse response) {
        StringBuilder queryCon = new StringBuilder();
        //调用结果信息
        ApiResult result = new ApiResult();
        Gson gson = new Gson();
        try {
            //读取请求体中的查询条件。
            BufferedReader reader = request.getReader();
            String str = null;
            while ((str = reader.readLine()) != null){
                queryCon.append(str);
            }
            JsonObject page = null;
            JsonObject filter = new JsonObject();
            //获取分页条件和过滤条件
            if (!"".equals(queryCon.toString())){
                JsonObject query = gson.fromJson(queryCon.toString(),JsonObject.class);
                page = query.get("pageinfo") == null ? null : (JsonObject) query.get("pageinfo");
                filter = query.get("query") == null ? new JsonObject() : (JsonObject) query.get("query");
            }
            DBObject qcon = new BasicDBObject();
            for (Map.Entry<String,JsonElement> entry:filter.entrySet()){
                if (entry.getValue().isJsonObject()) {
                    for (Map.Entry<String,JsonElement> range:entry.getValue().getAsJsonObject().entrySet()){
                        JsonPrimitive jsonPrimitive = (JsonPrimitive) range.getValue();
                        if (jsonPrimitive.isNumber()) {
                            qcon.put(entry.getKey(),new BasicDBObject(range.getKey(),range.getValue().getAsDouble()));
                        }else {
                            qcon.put(entry.getKey(),new BasicDBObject(range.getKey(),range.getValue().getAsString()));
                        }
                    }
                }else {
                    qcon.put(entry.getKey(),entry.getValue().getAsString());
                }
            }
            //按照告警开始时间倒序
            DBObject sort = new BasicDBObject("startsAt",-1);
            long total = 0l;
            //执行查询
            DBCursor cursor = mongoTemplate.getCollection(alertConfigurationProp.alertRecordTalbeName).find(qcon).sort(sort);
            //需要分页查询
            if (page != null){
                total = mongoTemplate.getCollection(alertConfigurationProp.alertRecordTalbeName).count(qcon);
                long from = page.get("from") == null ? 0 : page.get("from").getAsLong();
                long size = page.get("size") == null ? 10 : page.get("size").getAsLong();
                cursor.skip((int) from).limit((int) size);
            }else {
                total = cursor.size();
            }
            //搜集查询结果数据
            List<Map> records = new ArrayList<Map>();
            while (cursor.hasNext()){
                records.add(cursor.next().toMap());
            }
            result.setData(records);
            result.setSuccess(true);
            result.setCode(0);
            result.setTotal(total);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("读取查询条件失败！");
            result.setHint("读取查询条件异常！");
        }
       writeOutData(response,result);
    }
    /**
     * @description 根据记录id查询告警详情
     * 请求方式：
     * GET /api/queryAlertingById/id
     * 返回结果：
     * {
     *     success:bool(true/false),//表示是否成功
     *     code:0/1 ,//执行结果编码，目前只有0成功，1失败
     *     total:1,//表示查询到的记录数
     *     data:{}, //表示返回的记录详情
     *     hint:string //服务器返回的提示信息，一般在访问失败时给出。
     * }
     * @date 2017/11/22
     * @author Niemingming
     */
    @ResponseBody
    @RequestMapping(value = "/queryAlertingById/{id}",method = RequestMethod.GET)
    public void queryAlertingById(@PathVariable String id,HttpServletResponse response) {
        //创建返回结果信息对象
        ApiResult result = new ApiResult();
        //根据id从当前告警表中查询告警详情
        DBObject queryRes = mongoTemplate.getCollection(alertConfigurationProp.alertRecordTalbeName).findOne(id);
        if (queryRes == null){
            result.setHint("未找到id为【" + id + "】的告警记录！,请确定是否还在当前告警中！");
        }else {
            result.setSuccess(true);
            result.setCode(0);
            result.setData(queryRes.toMap());
            result.setTotal(1l);
        }
        writeOutData(response,result);
    }

    /**
     * @description 根据查询条件，查询历史记录，POST请求方式
     *POST /api/queryHistoryList
     * {
     *     pageinfo:{//分页信息如果不传，表示不分页
     *         from:10, //从第几条开始，默认是0
     *         size:10  //查询多少条，默认是10，
     *     },
     *     query:{//查询条件，遵循mongo的查询格式
     *          alertname:"testalert",
     *          "labels.job":"tomcat",
     *          times:{$gte:"10"}
     *     }
     * }
     * 返回数据格式为：
     * {
     *     success:bool(true/false),//表示是否成功
     *     code:0/1 ,//执行结果编码，目前只有0成功，1失败
     *     total:long,//表示查询到的记录数
     *     data:[], //表示返回的记录列表详情
     *     hint:string //服务器返回的提示信息，一般在访问失败时给出。
     * }
     *
     * @date 2017/11/21
     * @author Niemingming
     */
    @ResponseBody
    @RequestMapping(value = "/queryHistoryList",method = RequestMethod.POST)
    public void queryHistoryList(HttpServletRequest request,HttpServletResponse response) {
        StringBuilder queryCon = new StringBuilder();
        //调用结果信息
        ApiResult result = new ApiResult();
        Gson gson = new Gson();
        try {
            //读取请求体中的查询条件。
            BufferedReader reader = request.getReader();
            String str = null;
            while ((str = reader.readLine()) != null) {
                queryCon.append(str);
            }
            //创建ES查询对象
            EsQueryObject esQueryObject = new EsQueryObject();
            JsonObject page = null;
            JsonObject filter = new JsonObject();
            //获取分页条件和过滤条件
            if (!"".equals(queryCon.toString())) {
                JsonObject query = gson.fromJson(queryCon.toString().replace("$",""), JsonObject.class);
                page = query.get("pageinfo") == null ? null : (JsonObject) query.get("pageinfo");
                filter = query.get("query") == null ? new JsonObject() : (JsonObject) query.get("query");
            }
            if (page != null){//带有分页条件时，设置分页数据
                long from = page.get("from") == null ? 0 : page.get("from").getAsLong();
                long size = page.get("size") == null ? 10000: page.get("size").getAsLong();
                esQueryObject.setFrom(from);
                esQueryObject.setSize(size);
            }
            //添加查询条件
            esQueryObject.addQueryCondition(filter);
            //这里采用同步请求方法
            RestClient client = getRestClient();
            StringEntity queryBody = new StringEntity(gson.toJson(esQueryObject),"UTF-8");
            queryBody.setContentType("application/json;charset=UTF-8");
            Header header = new BasicHeader("content-type","application/json");
            //请求ES查询历史数据
            Response queryResult = client.performRequest("GET","/_search",new HashMap<String,String>(),queryBody,header);
            dealWithHistoryList(result,queryResult);
        }catch (Exception e){
            e.printStackTrace();
            result.setHint("查询历史数据出现异常！");
        }
        writeOutData(response,result);
    }
    /**
     * @description 根据index和id查询历史详情
     * GET /api/queryHistoryById/{index}/{id}
     * 返回数据格式为：
     * {
     *     success:bool(true/false),//表示是否成功
     *     code:0/1 ,//执行结果编码，目前只有0成功，1失败
     *     total:long,//表示查询到的记录数
     *     data:{}, //表示返回的记录列表详情
     *     hint:string //服务器返回的提示信息，一般在访问失败时给出。
     * }
     *
     *
     * @date 2017/11/22
     * @author Niemingming
     */
    @ResponseBody
    @RequestMapping(value = "/queryHistoryById/{index}/{id}",method = RequestMethod.GET)
    public void queryHistoryById (@PathVariable String index,@PathVariable String id ,HttpServletResponse response) {
        String endpoint = "/" + index + "/" + alertConfigurationProp.esType + "/" + id;
        RestClient restClient = getRestClient();
        //创建返回结果信息对象
        ApiResult result = new ApiResult();
        Gson gson = new Gson();
        //根据id从ES中查询告警详情
        Header header = new BasicHeader("content-type","application/json;charset=UTF-8");
        try {
            Response detail = restClient.performRequest("GET",endpoint,header);
            JsonObject resultJson = gson.fromJson(EntityUtils.toString(detail.getEntity()),JsonObject.class);
            if (resultJson.get("found") != null &&resultJson.get("found").getAsBoolean()) {
                JsonObject source = resultJson.get("_source").getAsJsonObject();
                source.addProperty("_index",index);
                source.addProperty("_id",id);
                result.setData(source);
                result.setSuccess(true);
                result.setTotal(1);
                result.setCode(0);
            }else if (resultJson.keySet().contains("error")){
                result.setHint("传入的index【" + index + "】不存在！");
            }else {
                //未能从ES中查到
                result.setHint("未能查询到id为【" + id + "】的历史告警数据！");
            }
        } catch (IOException e) {
            e.printStackTrace();
            result.setHint("查询历史数据出现异常！");
        }
        writeOutData(response,result);
    }
    /**
     * @description 根据关键字查询历史告警记录
     * 查询格式
     * GET /api/searchHistoryList/{searchstr}
     * 返回数据格式：
     * {
     *     success:bool(true/false),//表示是否成功
     *     code:0/1 ,//执行结果编码，目前只有0成功，1失败
     *     total:long,//表示查询到的记录数
     *     data:[], //表示返回的记录列表详情
     *     hint:string //服务器返回的提示信息，一般在访问失败时给出。
     * }
     * @date 2017/11/22
     * @author Niemingming
     */
    @ResponseBody
    @RequestMapping("/searchHistoryList/{searchstr}")
    public void searchHistoryList(@PathVariable String searchstr,HttpServletResponse response) {
        //按照结束时间倒序排列
        String endpoint = "/_search?q=" + searchstr + "&sort=endsAt:desc";
        RestClient restClient = getRestClient();
        //创建返回结果信息对象
        ApiResult result = new ApiResult();
        //根据id从ES中查询告警详情
        Header header = new BasicHeader("content-type","application/json");
        try {
            Response queryResult = restClient.performRequest("GET",endpoint,header);
            dealWithHistoryList(result,queryResult);
        } catch (IOException e) {
            e.printStackTrace();
            result.setHint("查询历史数据出现异常！");
        }
        //输出返回结果
        writeOutData(response,result);
    }
    /**
     * @description 输出返回结果
     * @date 2017/11/22
     * @author Niemingming
     */
    private void writeOutData(HttpServletResponse response,ApiResult result){
        try {
            Gson gson = new Gson();
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Type","application/json");
            //将查询结果返回给调用者
            response.getWriter().write(gson.toJson(result));
            response.getWriter().flush();;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    /**
     * @description 处理历史列表数据查询结果
     * @date 2017/11/22
     * @author Niemingming
     */
    private void dealWithHistoryList(ApiResult result, Response queryResult) throws IOException {
        Gson gson = new Gson();
        //获取结果字符串
        String resultJson = EntityUtils.toString(queryResult.getEntity());
        JsonObject resultObj = gson.fromJson(resultJson,JsonObject.class);
        //分析查询结果,判断查询是否出错
        if (resultObj.get("error") != null) {
            result.setHint("查询ES数据出现异常！");
        }else{
            JsonObject hits =  resultObj.get("hits").getAsJsonObject();
            //获取总数
            result.setTotal(hits.get("total") == null ? 0l : hits.get("total").getAsLong());
            //将结果数据返回
            List data = new ArrayList();
            JsonArray datas = (JsonArray) hits.get("hits");
            for (int i = 0; i < datas.size(); i++){
                //获取单条记录
                JsonObject record = datas.get(i).getAsJsonObject();
                JsonObject source = record.get("_source").getAsJsonObject();
                source.addProperty("_index",record.get("_index").getAsString());
                source.addProperty("_id",record.get("_id").getAsString());
                //将结果添加进去
                data.add(source);
            }
            result.setSuccess(true);
            result.setCode(0);
            result.setData(data);
        }
    }

    /**
     * @description 获取Es的请求客户端
     * @date 2017/11/22
     * @author Niemingming
     */
    private RestClient getRestClient() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(alertConfigurationProp.esDataPattern);
        HttpHost[] hosts = new HttpHost[alertConfigurationProp.esHostNames.size()];
        for (int i = 0; i < hosts.length; i++ ){
            hosts[i] = HttpHost.create(alertConfigurationProp.esHostNames.get(i));
        }
        //创建ES请求客户端
        RestClient restClient = RestClient.builder(hosts).build();
        return restClient;
    }
    /**
     * @description 初始化ESmapping
     * PUT /alert-*
     * {
     *     mappgings:{
     *         alert_mapping:{//映射名称
     *             properties:{
     *                 startsAt:{type:"date",format:"epoch_second"},//日期格式默认为Unix秒
     *                 endsAt:{type:"date",format:"epoch_second"},
     *                 times:{type:"integer"},
     *                 lastNotifyTime:{type:"date",format:"epoch_second"},
     *                 lastReceiveTime:{type:"date",format:"epoch_second"},
     *                 status:{type:"keyword"},
     *                 message:{type:"text"}
     *             }
     *         }
     *     }
     * }
     * @date 2017/11/23
     * @author Niemingming
     */
    @ResponseBody
    @RequestMapping("/initEsMappging")
    public String initESMapping() {
        RestClient restClient = getRestClient();
        //采用通配符来设置映射
        String endPoint = "/" + alertConfigurationProp.indexpre + "*/HISTORY/_mapping";
        DefaultResourceLoader loader = new DefaultResourceLoader();
        try {
            File file = loader.getResource(AlertConstVariable.ES_MAPPING_FILE).getFile();
            BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
            StringBuilder mapping = new StringBuilder();
            String str = null;
            while ((str = bufferedReader.readLine()) != null){
                //忽略注释
                if (str.startsWith("//")){
                    continue;
                }
                mapping.append(str);
            }
            System.out.println(mapping.toString());
            StringEntity entity = new StringEntity(mapping.toString(),"UTF-8");
            entity.setContentType("application/json;charset=UTF-8");
            Header header = new BasicHeader("content-type","application/json");
            Response response = restClient.performRequest("POST",endPoint,new HashMap<String, String>(),entity,header);
            String result = EntityUtils.toString(response.getEntity());
            System.out.println(result);
        } catch (IOException e) {
            e.printStackTrace();
            return "failure";
        }
        return "success";
    }

    /**
     * @description ES查询对象，用于生成ES的查询json数据
     * @date 2017/11/22
     * @author Niemingming
     */
    public class EsQueryObject{
        /*分页查询起始位置，默认是0*/
        private long from = 0;
        private long size = 10000;//默认不分页，ES默认的最大查询结果是10000条
        private List sort;
        private Map query;

        /**
         * @description 构造函数，同时完成数据初始化操作
         * @date 2017/11/22
         * @author Niemingming
         */
        public EsQueryObject (){
            sort = new ArrayList();
            //默认按照结束时间倒序
            Map sortf = new HashMap();
            sortf.put("endsAt","desc");
            sort.add(sortf);
            query = new HashMap();
            Map bool = new HashMap();
            query.put("bool",bool);
            List filter = new ArrayList();
            bool.put("filter",filter);
        }

        public void addQueryCondition(JsonObject cons){
            List filters = (List) ((Map)query.get("bool")).get("filter");
            for (String field:cons.keySet()){
                Object value = cons.get(field);
                //范围查询，格式为{range:{field:{$ge:value}}}
                Map fieldr = new HashMap();
                fieldr.put(field,value.toString());
                if (value instanceof JsonObject){
                    JsonObject filter = (JsonObject) value;
                    for (Map.Entry<String,JsonElement> entry: filter.entrySet()){
                        JsonPrimitive obj = (JsonPrimitive) entry.getValue();
                        if (obj.isNumber()){
                            fieldr.put(field,new BasicDBObject(entry.getKey(),obj.getAsDouble()));
                        }else {
                            fieldr.put(field,new BasicDBObject(entry.getKey(),obj.getAsString()));
                        }
                    }
                    Map range = new HashMap();
                    range.put("range",fieldr);
                    filters.add(range);
                }else if (value instanceof JsonPrimitive){//如果其他类型，表示是字符串,格式为{term:{field:value}}
                    fieldr.put(field,((JsonPrimitive)value).getAsString());
                    Map term = new HashMap();
                    term.put("term",fieldr);
                    filters.add(term);
                }
            }
        }

        public long getFrom() {
            return from;
        }

        public void setFrom(long from) {
            this.from = from;
        }

        public long getSize() {
            return size;
        }

        public void setSize(long size) {
            this.size = size;
        }
    }
}
