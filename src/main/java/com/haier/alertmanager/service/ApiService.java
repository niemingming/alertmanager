package com.haier.alertmanager.service;

import com.google.gson.*;
import com.haier.alertmanager.configuration.AlertConfigurationProp;
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
import org.springframework.core.io.Resource;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.*;

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
     *         currentPage:10, //当前第几页，从1开始
     *         pageSize:10  //查询多少条，默认是10，
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
     *     data:{
     *         page:{
     *             total:4,
     *             currentPage:1
     *         },
     *         list:[]
     *     }, //表示返回的记录列表详情
     *     hint:string //服务器返回的提示信息，一般在访问失败时给出。
     * }
     * @date 2017/11/21
     * @author Niemingming
     */
    @ResponseBody
    @RequestMapping(value = "/queryAlertingList",method= RequestMethod.POST)
    public void queryAlertingList(HttpServletRequest request, HttpServletResponse response) {
        //调用结果信息
        ApiResult result = new ApiResult();
        Gson gson = new Gson();
        try {
            DBObject qcon = readQueryCondition(request,result);
            //出现异常后，退出，返回提示信息。
            if (result.getMsg() != null){
                writeOutData(response,result);
                return;
            }
            //增加级别的非空查询
            qcon.put("level",new BasicDBObject("$ne","null"));
            //按照告警开始时间倒序
            DBObject sort = new BasicDBObject("startsAt",-1);
            long total = 0l;
            //执行查询
            DBCursor cursor = mongoTemplate.getCollection(alertConfigurationProp.alertRecordTalbeName).find(qcon).sort(sort);
            //需要分页查询
            long currentPage = 0;
            JsonObject page = qcon.get("page") == null ? null : (JsonObject) qcon.removeField("page");
            if (page != null){
                total = mongoTemplate.getCollection(alertConfigurationProp.alertRecordTalbeName).count(qcon);
                long size = page.get("pageSize") == null ? 10 : page.get("pageSize").getAsLong();
                //根据当前第几页计算from
                currentPage = page.get("currentPage") == null ? 0 : page.get("currentPage").getAsLong();
                long from = (currentPage - 1)*size;
                cursor.skip((int) from).limit((int) size);

            }else {
                total = cursor.size();
            }
            //搜集查询结果数据
            List<Map> records = new ArrayList<Map>();
            while (cursor.hasNext()){
                records.add(cursor.next().toMap());
            }
            ((Map)result.getData()).put("list",records);
            result.setSuccess(true);
            result.setCode(0);
            result.setTotal(total);
            result.setCurrentPage(currentPage);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("查询当前告警失败！");
            result.setMsg("查询当前告警异常！");
        }
       writeOutData(response,result);
    }

    private DBObject readQueryCondition(HttpServletRequest request, ApiResult result) {
        //读取请求体中的查询条件。
        BufferedReader reader = null;
        DBObject qcon = new BasicDBObject();
        StringBuilder queryCon = new StringBuilder();
        Gson gson = new Gson();
        try {
            reader = request.getReader();
            String str = null;
            while ((str = reader.readLine()) != null){
                queryCon.append(str);
            }
            JsonObject page = null;
            JsonObject filter = new JsonObject();
            JsonElement group = null;
            //获取分页条件和过滤条件，增加分组条件。
            if (!"".equals(queryCon.toString())){
                JsonObject query = gson.fromJson(queryCon.toString(),JsonObject.class);
                page = query.get("pageinfo") == null ? null : (JsonObject) query.get("pageinfo");
                qcon.put("page",page);
                filter = query.get("query") == null ? new JsonObject() : (JsonObject) query.get("query");
                group = query.get("group") == null ? null: query.get("group");
                if (group != null){
                    //如果是数组，表示按照多个字段分组
                    Map groupFields = new HashMap();
                    if (group.isJsonArray()){
                        JsonArray gp = group.getAsJsonArray();
                        for (int i = 0 ;i < gp.size(); i++){
                            String fieldName = gp.get(i).getAsString();
                            groupFields.put(fieldName.replace(".","-"),"$" + fieldName);
                        }
                    }else {//按照单个字段分组
                        String fieldName = group.getAsString();
                        groupFields.put(fieldName,"$" + fieldName);
                    }
                    //放入_id字段中
                    Map _id = new HashMap();
                    _id.put("_id",groupFields);
                    //放入查询条件中返回。
                    qcon.put("group",_id);
                }
            }
            for (Map.Entry<String,JsonElement> entry:filter.entrySet()){
                if (entry.getValue().isJsonObject()) {
                    DBObject rangeObj = new BasicDBObject();//有可能有多个属性
                    for (Map.Entry<String,JsonElement> range:entry.getValue().getAsJsonObject().entrySet()){
                        JsonPrimitive jsonPrimitive = (JsonPrimitive) range.getValue();
                        if (jsonPrimitive.isNumber()) {
                            rangeObj.put(range.getKey(),range.getValue().getAsDouble());
                        }else {
                            rangeObj.put(range.getKey(),range.getValue().getAsString());
                        }
                    }
                    qcon.put(entry.getKey(),rangeObj);
                }else if (entry.getValue().isJsonArray()){
                    //如果是数组，表示in查询匹配多个
                    Map containsCon = new HashMap();
                    List values = new ArrayList();
                    JsonArray jsonArray = entry.getValue().getAsJsonArray();
                    for (int i = 0; i < jsonArray.size(); i++ ){
                        JsonPrimitive jsonPrimitive = jsonArray.get(i).getAsJsonPrimitive();
                        if (jsonPrimitive.isNumber()){
                            values.add(jsonPrimitive.getAsDouble());
                        }else {
                            values.add(jsonPrimitive.getAsString());
                        }
                    }
                    containsCon.put("$in",values);
                    qcon.put(entry.getKey(),containsCon);
                }else{
                    qcon.put(entry.getKey(),entry.getValue().getAsString());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("读取查询条件失败！");
            result.setMsg("读取查询条件异常！");
        }
        return qcon;
    }

    /**
     * @description 根据记录id查询告警详情
     * 请求方式：
     * GET /api/queryAlertingById/id
     * 返回结果：
     * {
     *     success:bool(true/false),//表示是否成功
     *     code:0/1 ,//执行结果编码，目前只有0成功，1失败
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
            result.setMsg("未找到id为【" + id + "】的告警记录！,请确定是否还在当前告警中！");
        }else {
            result.setSuccess(true);
            result.setCode(0);
            result.setData(queryRes.toMap());
        }
        writeOutData(response,result);
    }

    /**
     * @description 根据查询条件，查询历史记录，POST请求方式
     *POST /api/queryHistoryList
     * {
     *     pageinfo:{//分页信息如果不传，表示不分页
     *         currentPage:10, //当前第几页，从1开始计数
     *         pageSize:10  //查询多少条，默认是10，
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
     *     data:{
     *         page:{
     *             total:4,
     *             currentPage:1
     *         },
     *         list:[]
     *     }, //表示返回的记录列表详情
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
            long currentPage = 0;
            if (!"".equals(queryCon.toString())) {
                JsonObject query = gson.fromJson(queryCon.toString().replace("$",""), JsonObject.class);
                page = query.get("pageinfo") == null ? null : (JsonObject) query.get("pageinfo");
                filter = query.get("query") == null ? new JsonObject() : (JsonObject) query.get("query");
            }
            if (page != null){//带有分页条件时，设置分页数据
                long size = page.get("pageSize") == null ? 10000: page.get("pageSize").getAsLong();
                currentPage = page.get("currentPage") == null ? 0 : page.get("currentPage").getAsLong();
                long from = (currentPage-1)*size;
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
            Response queryResult = client.performRequest("POST","/alert-*/_search",new HashMap<String,String>(),queryBody,header);
            result.setCurrentPage(currentPage);
            dealWithHistoryList(result,queryResult);
        }catch (Exception e){
            e.printStackTrace();
            result.setMsg("查询历史数据出现异常！");
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
                result.setCode(0);
            }else if (resultJson.keySet().contains("error")){
                result.setMsg("传入的index【" + index + "】不存在！");
            }else {
                //未能从ES中查到
                result.setMsg("未能查询到id为【" + id + "】的历史告警数据！");
            }
        } catch (IOException e) {
            e.printStackTrace();
            result.setMsg("查询历史数据出现异常！");
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
    public void searchHistoryList(@PathVariable String searchstr,@RequestParam(value = "currentPage",defaultValue = "1") int currentPage,
                                  @RequestParam(value = "pageSize",defaultValue = "10") int pageSize, HttpServletResponse response) {
        int from = (currentPage - 1)*pageSize ;
        int size = pageSize;
        //按照结束时间倒序排列
        String endpoint = "/alert-*/_search?q=" + searchstr + "&sort=endsAt:desc&from=" + from + "&size=" + size;
        RestClient restClient = getRestClient();
        //创建返回结果信息对象
        ApiResult result = new ApiResult();
        //根据id从ES中查询告警详情
        Header header = new BasicHeader("content-type","application/json");
        try {
            Response queryResult = restClient.performRequest("GET",endpoint,header);
            result.setCurrentPage(currentPage);
            dealWithHistoryList(result,queryResult);
        } catch (IOException e) {
            e.printStackTrace();
            result.setMsg("查询历史数据出现异常！");
        }
        //输出返回结果
        writeOutData(response,result);
    }
    /**
     * @description 提供按照某个指定字段分组查询功能POST请求
     * POST /api/queryAlertingByGroup
     * {
     *     query:{//查询条件，遵循mongo的查询格式
     *          alertname:"testalert",
     *          "labels.job":"tomcat",
     *          times:{$gte:"10"}
     *     },
     *     group:["level","labels.project"]//可以按照多个字段，也可以按照一个字段
     * }
     * 返回数据格式为：
     * {
     *     success:bool(true/false),//表示是否成功
     *     code:0/1 ,//执行结果编码，目前只有0成功，1失败
     *     data:{
     *         page:{
     *             total:4,
     *             currentPage:1
     *         },
     *         list:[]
     *     }, //表示返回的记录列表详情
     * @date 2017/11/26
     * @author Niemingming
     */
    @ResponseBody
    @RequestMapping(value="/queryAlertingByGroup",method = RequestMethod.POST)
    public void queryAlertingByGroup(HttpServletRequest request,HttpServletResponse response) {
        //调用结果信息
        ApiResult result = new ApiResult();
        Gson gson = new Gson();
        try {
            DBObject qcon = readQueryCondition(request, result);
            //出现异常后，退出，返回提示信息。
            if (result.getMsg() != null) {
                writeOutData(response, result);
                return;
            }
            Map group = qcon.get("group") == null ? null : (Map) qcon.removeField("group");
            List aggre = new ArrayList();
            if (group == null){
                result.setMsg("请传入要分组的字段名称！");
            }else{
                //增加统计项
                group.put("count",new BasicDBObject("$sum",1));
                qcon.removeField("page");//去掉分页信息
                //有查询条件
                if (qcon.keySet().size() > 0){
                    aggre.add(new BasicDBObject("$match",qcon));
                }
                aggre.add(new BasicDBObject("$group",group));
            }
            //执行统计查询
            Iterable<DBObject> datas = mongoTemplate.getCollection(alertConfigurationProp.alertRecordTalbeName).aggregate(aggre).results();
            List aggreData = new ArrayList();
            Iterator<DBObject> iterator = datas.iterator();
            while (iterator.hasNext()) {
                DBObject record = iterator.next();
                DBObject _id = (DBObject) record.get("_id");
                Map row= new HashMap();
                for (Object key:_id.toMap().keySet()){
                    row.put(key,_id.get(key.toString()));
                }
                row.put("count",record.get("count"));
                aggreData.add(row);
            }
            ((Map)result.getData()).clear();
            ((Map)result.getData()).put("list",aggreData);
            result.setCode(0);
            result.setSuccess(true);
        }catch (Exception e){
            e.printStackTrace();
            result.setMsg("统计出现异常！");
        }
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
            result.setMsg("查询ES数据出现异常！");
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
            //修改列表的数据格式。
            ((Map)result.getData()).put("list",data);
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
        String endPoint = "/_template/alert_template";
        DefaultResourceLoader loader = new DefaultResourceLoader();
        try {
            Resource resource = loader.getResource(alertConfigurationProp.esTemplateAddress);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(resource.getInputStream()));
            StringBuilder mapping = new StringBuilder();
            String str = null;
            while ((str = bufferedReader.readLine()) != null){
                //忽略注释
                if (str.startsWith("//")){
                    continue;
                }
                mapping.append(str);
            }
            StringEntity entity = new StringEntity(mapping.toString(),"UTF-8");
            entity.setContentType("application/json;charset=UTF-8");
            Header header = new BasicHeader("content-type","application/json");
            Response response = restClient.performRequest("PUT",endPoint,new HashMap<String, String>(),entity,header);
            String result = EntityUtils.toString(response.getEntity());
            System.out.println(result);
        } catch (IOException e) {
            e.printStackTrace();
            return "failure";
        }
        return "success";
    }
    /**
     * @description 查询公共编码中的告警级别编码
     * GET /api/queryAlertLevels
     * 返回数据：
     * {
     *      success:true,
     *      code:0/1,
     *      data:{
     *          error:"紧急",
     *          warn:"严重",
     *          info:"一般",
     *          debug:"提示"
     *      }
     * }
     * @date 2017/11/27
     * @author Niemingming
     */
    @ResponseBody
    @RequestMapping("/queryAlertLevels")
    public void queryAlertLevels(HttpServletRequest request,HttpServletResponse response) {
        Cookie [] cookies = request.getCookies();
        if (cookies != null){
            for (Cookie cookie:cookies){
                System.out.println(cookie.getName()+":" + cookie.getValue()+":" + cookie.getDomain());
            }
        }
        writeCodeResult(alertConfigurationProp.alertlevel,response);
    }

    private void writeCodeResult(Object data,HttpServletResponse response){
        ApiResult result = new ApiResult();
        result.setData(data);
        result.setSuccess(true);
        result.setCode(0);
        writeOutData(response,result);
    }
    /**
     * @description 查询告警分类
     * GET /api/queryAlertCategories
     * 返回数据：
     * {
     *      success:true,
     *      code:0/1,
     *      data:{
     *          machine:"机器",
     *          app:"应用"
     *      }
     * }
     * @date 2017/11/28
     * @author Niemingming
     */
    @ResponseBody
    @RequestMapping("/queryAlertCategories")
    public void queryAlertCategoris(HttpServletResponse response){
        writeCodeResult(alertConfigurationProp.alertCategory,response);
    }
    /**
     * @description 查询告警类型
     * GET /api/queryAlertTypes
     * 返回数据：
     * {
     *      success:true,
     *      code:0/1,
     *      data:{
     *         cockpit_schedule_task_exit: 容器实例退出,
     *         mysql_status_handlers_read_rnd: mysql索引不合理,
     *         service_down: 服务不可用,
     *         node_reboot: 系统重启,
     *         node_cpu_pct_threshold_exceeded: 节点CPU使用率过高,
     *         node_mem_threshold_exceeded: 节点剩余内存不足,
     *         node_mem_pct_threshold_exceeded: 节点内存使用率过高,
     *         node_fs_pct_threshold_exceeded: 节点文件系统使用率过高,
     *         node_tcp_conn_toomuch: 节点TCP连接数过高,
     *         node_disk_io_util_threshold_exceeded: 节点磁盘IO过高,
     *         redis_service_down: redis服务不可用,
     *         redis_mem_pct_threshold_exceeded: Redis内存使用率过高,
     *         redis_mem_threshold_exceeded: Redis内存不足,
     *         redis_toomany_command_executed: Redis命令执行频繁,
     *         redis_dangerous_command_executed: Redis执行危险命令
     *      }
     * }
     * @date 2017/11/28
     * @author Niemingming
     */
    @ResponseBody
    @RequestMapping("/queryAlertTypes")
    public void queryAlertTypes(HttpServletResponse response){
        writeCodeResult(alertConfigurationProp.alertType,response);
    }

    /**
     * @description 查询告警所有公共编码
     * GET /api/queryAlertCode
     * 返回数据：
     * {
     *      success:true,
     *      code:0/1,
     *      data:{
     *      alertLevel:{
     *          error:"紧急",
     *          warn:"严重",
     *          info:"一般",
     *          debug:"提示"
     *      },
     *      alertCategory:{
     *          machine:"机器",
     *          app:"应用"
     *      },
     *      alertType:{
     *         cockpit_schedule_task_exit: 容器实例退出,
     *         mysql_status_handlers_read_rnd: mysql索引不合理,
     *         service_down: 服务不可用,
     *         node_reboot: 系统重启,
     *         node_cpu_pct_threshold_exceeded: 节点CPU使用率过高,
     *         node_mem_threshold_exceeded: 节点剩余内存不足,
     *         node_mem_pct_threshold_exceeded: 节点内存使用率过高,
     *         node_fs_pct_threshold_exceeded: 节点文件系统使用率过高,
     *         node_tcp_conn_toomuch: 节点TCP连接数过高,
     *         node_disk_io_util_threshold_exceeded: 节点磁盘IO过高,
     *         redis_service_down: redis服务不可用,
     *         redis_mem_pct_threshold_exceeded: Redis内存使用率过高,
     *         redis_mem_threshold_exceeded: Redis内存不足,
     *         redis_toomany_command_executed: Redis命令执行频繁,
     *         redis_dangerous_command_executed: Redis执行危险命令
     *      }
     *      }
     * }
     * @date 2017/11/28
     * @author Niemingming
     */
    @ResponseBody
    @RequestMapping("/queryAlertCode")
    public void queryAlertCode(HttpServletResponse response){
        DBObject code = new BasicDBObject("alertType",alertConfigurationProp.alertType);
        code.put("alertCategory",alertConfigurationProp.alertCategory);
        code.put("alertLevel",alertConfigurationProp.alertlevel);
        writeCodeResult(code.toMap(),response);
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
                    DBObject rangeObj = new BasicDBObject();
                    for (Map.Entry<String,JsonElement> entry: filter.entrySet()){
                        JsonPrimitive obj = (JsonPrimitive) entry.getValue();
                        rangeObj.put(entry.getKey(),obj);
                    }
                    fieldr.put(field,rangeObj);
                    Map range = new HashMap();
                    range.put("range",fieldr);
                    filters.add(range);
                }else if (value instanceof JsonArray){
                    //格式为{field:[value1,value2]}表示多个的值，为或的关系
                    List terms = new ArrayList();
                    JsonArray values = (JsonArray) value;
                    for (int i = 0; i < values.size(); i++){
                        JsonPrimitive jsonPrimitive = values.get(i).getAsJsonPrimitive();
                        if (jsonPrimitive.isNumber()){
                            terms.add(jsonPrimitive.getAsDouble());
                        }else {
                            terms.add(jsonPrimitive.getAsString());
                        }
                    }
//                    String  mstr = terms.toString();
                    fieldr.put(field,terms);
                    filters.add(new BasicDBObject("terms",fieldr).toMap());
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
