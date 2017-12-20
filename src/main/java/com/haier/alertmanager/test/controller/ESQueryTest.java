package com.haier.alertmanager.test.controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mongodb.BasicDBObject;
import com.mongodb.util.JSON;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @description elasticsearch客户端查询测试
 * @date 2017/11/20
 * @author Niemingming
 */
public class ESQueryTest {

    public static  void  main(String[] args) throws IOException {
        RestClient client = RestClient.builder(new HttpHost("localhost",9200)).build();
        //查询方法。
        /**
         * GET /_search
         * {
         *     query:{
         *         bool:{
         *             must:[
         *                  {"match":{"job":"tom"}}
         *             ],
         *             filter:[
         *                  range:{times:{gte:10}},
         *                  term:{monitor:"codelab"}
         *             ]
         *         }
         *     }
         * }
         */
        Map query = new HashMap();
        //构造查询条件
        Map multi = new HashMap();
        multi.put("query_string",new BasicDBObject("query","httpminotor").toMap());
//        multi.put("fields","_all");
        Map bool = new HashMap();

//        query.put("multi_match",multi);
        query.put("bool",bool);
        List must = new ArrayList();
        bool.put("must",must);
        Map match = new HashMap();
        must.add(match);
        Map job = new HashMap();
        match.put("match",job);
        job.put("job","tomcat");
        List filter = new ArrayList();
        bool.put("filter",filter);
        Map range = new HashMap();
        filter.add(range);
        filter.add(multi);
        Map times = new HashMap();
        range.put("range",times);
        Map time = new HashMap();
        times.put("times",time);
        time.put("gte",25);
        Map term = new HashMap();
        filter.add(term);
        Map monitor = new HashMap();
        term.put("term",monitor);
        monitor.put("monitor","codelab");
        Map con = new HashMap();
        con.put("query",query);
        con.put("from",0);
        con.put("size",10);

//        query.put("term",monitor);
        Gson gson = new Gson();
        System.out.println(gson.toJson(con));
        StringEntity se = new StringEntity(gson.toJson(con));
        Header header = new BasicHeader("content-type","application/json");

        Response response = client.performRequest("GET","/_search",new HashMap<String, String>(),se,header);
        System.out.println(EntityUtils.toString(response.getEntity()));
        client.close();


    }
}
