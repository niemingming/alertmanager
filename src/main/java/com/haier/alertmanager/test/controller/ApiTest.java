package com.haier.alertmanager.test.controller;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;

/**
 * @description api服务调用测试
 * @date 2017/11/22
 * @author Niemingming
 */
public class ApiTest {

    public static void main(String[]args) throws IOException {
//        queryList();
//        queryById();
//        queryHistoryList();
//        queryHistoryById();
        searchHistoryList();
//        queryGroup();
//        testgson();
//        queryLevelCode();
    }


    public  static  void queryList() throws IOException {
        HttpClient client = HttpClients.createDefault();
        HttpPost post = new HttpPost("http://localhost:8081/api/queryAlertingList");
        //不分页查询，列表查询为POST请求方式，条件为project=
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("{")
                .append("   pageinfo:{currentPage:1,pageSize:1},")
                .append("  query:{")
                .append(" \"project\":[\"project1\",\"project2\"],")
                .append("   \"times\":{$gt:1}")
                .append("  }")
                .append("}");
        StringEntity stringEntity = new StringEntity(stringBuilder.toString());
        post.setEntity(stringEntity);
        HttpResponse response = client.execute(post);
        HttpEntity res = response.getEntity();
        System.out.println(EntityUtils.toString(res));
    }

    public static void queryById() throws IOException {
        HttpClient client = HttpClients.createDefault();
        HttpGet get = new HttpGet("http://localhost:8081/api/queryAlertingById/247D78214DCCD7FE830EC039F2B310C4");
        HttpResponse response = client.execute(get);
        HttpEntity res = response.getEntity();
        System.out.println(EntityUtils.toString(res));
    }

    public static void queryHistoryList() throws IOException {
        HttpClient client = HttpClients.createDefault();
        HttpPost post = new HttpPost("http://localhost:8081/api/queryHistoryList");
        //不分页查询，列表查询为POST请求方式，条件为project=
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("{")
                .append("   pageinfo:{currentPage:1,pageSize:2},")
                .append("  query:{")
                .append("  \"project\":[\"project1\",\"project2\"],")
                .append("   times:{$gt:10}")
                .append("  }")
                .append("}");
        StringEntity stringEntity = new StringEntity(stringBuilder.toString(),"UTF-8");
        post.setEntity(stringEntity);
        HttpResponse response = client.execute(post);
        HttpEntity res = response.getEntity();
        System.out.println(EntityUtils.toString(res));
    }

    public static void queryHistoryById() throws IOException {
        HttpClient client = HttpClients.createDefault();
        HttpGet get = new HttpGet("http://localhost:8081/api/queryHistoryById/alert-201711/FC6EF20EED02AA884745283049CDE2B2-1511772777");
        HttpResponse response = client.execute(get);
        HttpEntity res = response.getEntity();
        System.out.println(EntityUtils.toString(res));
    }

    public static void  searchHistoryList() throws IOException {
        HttpClient client = HttpClients.createDefault();
        HttpGet get = new HttpGet("http://localhost:8081/api/searchHistoryList/tomcat");
        HttpResponse response = client.execute(get);
        HttpEntity res = response.getEntity();
        System.out.println(EntityUtils.toString(res));
    }

    public  static  void queryGroup() throws IOException {
        HttpClient client = HttpClients.createDefault();
        HttpPost post = new HttpPost("http://localhost:8081/api/queryAlertingByGroup");
        //不分页查询，列表查询为POST请求方式，条件为project=
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("{")
                .append("  query:{")
                .append(" \"project\":[\"project1\",\"project2\"]")
                .append("  },")
                .append(" group:[\"project\"]")
                .append("}");
        StringEntity stringEntity = new StringEntity(stringBuilder.toString());
        post.setEntity(stringEntity);
        HttpResponse response = client.execute(post);
        HttpEntity res = response.getEntity();
        System.out.println(EntityUtils.toString(res));
    }

    public static void queryLevelCode() throws IOException {
        HttpClient client = HttpClients.createDefault();
        HttpGet get = new HttpGet("http://10.138.16.192:8080/api/queryAlertLevels");
        HttpResponse response = client.execute(get);
        HttpEntity res = response.getEntity();
        System.out.println(EntityUtils.toString(res));
    }

    public static  void testgson(){
        Gson gson = new Gson();
        JsonArray list= gson.fromJson("[1,2,3]", JsonArray.class);
        System.out.println(list);
    }
}
