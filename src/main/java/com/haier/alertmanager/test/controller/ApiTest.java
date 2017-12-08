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

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

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
//        searchHistoryList();
//        queryGroup();
//        testgson();
//        queryLevelCode();
//        queryCode("queryAlertLevels");
//        queryCode("queryAlertCategories");
//        queryCode("queryAlertTypes");
//        queryCode("queryAlertCode");
        sendPostNotify("http://10.138.16.192:8080/api/queryAlertingList","{pageinfo:{currentPage:1,pageSize:10}}");
    }


    public  static  void queryList() throws IOException {
        HttpClient client = HttpClients.createDefault();
        HttpPost post = new HttpPost("http://10.138.16.192:8080/api/queryAlertingList");
        //不分页查询，列表查询为POST请求方式，条件为project=
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("{")
                .append("   pageinfo:{currentPage:1,pageSize:1},")
                .append("  query:{")
//                .append(" \"project\":[\"project1\",\"project2\"],")
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

    public static  void queryCode(String uri) throws IOException {
        HttpClient client = HttpClients.createDefault();
        HttpGet get = new HttpGet("http://localhost:8081/api/" + uri);
        HttpResponse response = client.execute(get);
        HttpEntity res = response.getEntity();
        System.out.println(EntityUtils.toString(res));
    }

    public static  void testgson(){
        Gson gson = new Gson();
        JsonArray list= gson.fromJson("[1,2,3]", JsonArray.class);
        System.out.println(list);
    }

    public static void sendPostNotify(String strURL, String params) {
        System.out.println(strURL);
        System.out.println(params);
//        Response result=new Response();
        try {
            URL url = new URL(strURL);// 创建连接
            HttpURLConnection connection = (HttpURLConnection) url
                    .openConnection();
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setUseCaches(false);
            connection.setInstanceFollowRedirects(true);
            connection.setRequestMethod("POST"); // 设置请求方式
            connection.setRequestProperty("Accept", "application/json"); // 设置接收数据的格式
            connection.setRequestProperty("Content-Type", "application/json"); // 设置发送数据的格式
            connection.connect();
            OutputStreamWriter out = new OutputStreamWriter(
                    connection.getOutputStream(), "UTF-8"); // utf-8编码
            out.append(params);
            out.flush();
            out.close();
            // 读取响应
            int length = (int) connection.getContentLength();// 获取长度
            InputStream is = connection.getInputStream();
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] temp = new byte[1024];
            int readLen = 0;
            int destPos = 0;
            while ((readLen = is.read(temp)) > 0) {
//                System.arraycopy(temp, 0, data, destPos, readLen);
//                destPos += readLen;
                bos.write(temp,0,readLen);
            }
//                result=result.ok(new String(data, "UTF-8"));
            System.out.println(new String(bos.toByteArray()));
            if (length != -1) {
            }
        } catch (IOException e) {
//            log.error("【NotifyServiceImpl.sendPostNotify】调用方法报错"+e);
//            result=result.fail(Constant.CODE_ONE, "查询告警信息失败！");
//            return result;
        }
//        return result;

    }
}
