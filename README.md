# alertmanager
## 目录
* [1.数据查询](#1)
  * [1.1.当前告警查询](#1.1)
    * [1.1.1.告警列表查询](#1.1.1)
    * [1.1.2.告警详情查询](#1.1.2)
  * [1.2.历史告警查询](#1.2)
    * [1.2.1.历史告警列表查询](#1.2.1)
    * [1.2.2.历史告警详情查询](#1.2.2)
    * [1.2.3.历史告警搜索](#1.2.3)
* [2.系统配置](#2)
  * [2.1.配置信息刷新](#2.1)
  
<h3 id="1">1.数据查询</h3>
数据查询底层是http协议，采用rest风格url作为服务提供。主要包括当前正在告警的数据查询和历史告警数据查询。
<h4 id="1.1">1.1当前告警查询</h4>
提供当前正在发生的告警记录查询接口。
<h5 id="1.1.1">1.1.1.告警列表查询</h5>
查询当前告警列表数据，如果不传分页信息，则查询当前所有的告警记录，否则查询当页告警记录。具体格式如下：
</br>查询格式

```
POST /api/queryAlertingList
{
     pageinfo:{//分页信息如果不传，表示不分页
         from:10, //从第几条开始，默认是0
         size:10  //查询多少条，默认是10，
     },
     query:{//查询条件，遵循mongo的查询格式
          alertname:"testalert",
          "labels.job":"tomcat",
           times:{$gte:"10"}
      }
 }
 ```
 返回数据格式
 
 ```
 {
      success:bool(true/false),//表示是否成功
      code:0/1 ,//执行结果编码，目前只有0成功，1失败
      total:long,//表示查询到的记录数
      data:[], //表示返回的记录列表详情
      hint:string //服务器返回的提示信息，一般在访问失败时给出。
 }
 ```
 我们用httpClient来模拟post请求，尝试查询project=project1的数据。具体请求代码如下：
 
 ```
 HttpClient client = HttpClients.createDefault();
 HttpPost post = new HttpPost("http://localhost:8081/api/queryAlertingList");
 //不分页查询，列表查询为POST请求方式，条件为project=
 StringBuilder stringBuilder = new StringBuilder();
 stringBuilder.append("{")
              .append("  query:{")
              .append("   \"labels.project\":\"project1\"")
              .append("  }")
              .append("}");
 StringEntity stringEntity = new StringEntity(stringBuilder.toString());
 post.setEntity(stringEntity);
 HttpResponse response = client.execute(post);
 HttpEntity res = response.getEntity();
 System.out.println(EntityUtils.toString(res));
 ```
 
上面例子,就是查询project为project1的数据，等价于:

```
POST /api/queryAlertingList
{
  query:{
    "labels.project":"project1"
  }
}
```
返回的数据格式为：

```
{
 "success":true,
 "code":0,
 "total":1,
 "data": [
  {
   "_id":"247D78214DCCD7FE830EC039F2B310C4",
   "startsAt":1511320572,
   "endsAt":-62135798400,
   "lastNotifyTime":1511321273,
   "lastReceiveTime":1511321307,
   "times":8,
   "status":"firing",
     "labels":{
       "alertname":"mymetric11",
       "group":"my1",
       "instance":"localhost:8080",
       "job":"tomcat",
       "monitor":"codelab-monitor",
       "project":"project1"
     }
   }
  ]
}
```
<h5 id="1.1.2">1.1.2.告警详情查询</h5>
根据记录id查询当前告警详情。具体格式如下：
</br>查询格式

```
GET /api/queryAlertingById/{id}//id为记录id
```
 返回数据格式
 
 ```
 {
      success:bool(true/false),//表示是否成功
      code:0/1 ,//执行结果编码，目前只有0成功，1失败
      total:long,//表示查询到的记录数
      data:{}, //表示返回的记录详情信息
      hint:string //服务器返回的提示信息，一般在访问失败时给出。
 }
 ```
我们用httpClient来模拟GET请求，查询具体某一条告警信息详情,我们查询id为：247D78214DCCD7FE830EC039F2B310C4，代码如下：

```
HttpClient client = HttpClients.createDefault();
HttpGet get = new HttpGet("http://localhost:8081/api/queryAlertingById/247D78214DCCD7FE830EC039F2B310C4");
HttpResponse response = client.execute(get);
HttpEntity res = response.getEntity();
System.out.println(EntityUtils.toString(res));
```
上面代码等价于：

```
GET /api/queryAlertingById/247D78214DCCD7FE830EC039F2B310C4
```
返回的数据格式：

```
{
 "success":true,
 "code":0,
 "total":1,
 "data": {
  "_id":"247D78214DCCD7FE830EC039F2B310C4",
  "startsAt":1511320572,
  "endsAt":-62135798400,
  "lastNotifyTime":1511322357,
  "lastReceiveTime":1511322362,
  "times":219,
  "status":"firing",
  "labels":{
   "alertname":"mymetric11",
   "group":"my1",
   "instance":"localhost:8080",
   "job":"tomcat",
   "monitor":"codelab-monitor",
   "project":"project1"
  }
 }
}
```

<h4 id="1.2">1.2历史告警查询</h4>
历史告警信息查询，按照统一性原则，与当前告警查询格式一致，只不过数据来源有区别。当前告警数据来源于MongoDB数据库，历史告警数据来源于ElasticSearch。
<h5 id="1.2.1">1.2.1.历史告警列表查询</h5>
查询格式

```
POST /api/queryAlertList
{
     pageinfo:{//分页信息如果不传，表示不分页
         from:10, //从第几条开始，默认是0
         size:10  //查询多少条，默认是10，
     },
     query:{//查询条件，遵循mongo的查询格式
          alertname:"testalert",
          "labels.job":"tomcat",
           times:{$gte:"10"}
      }
 }
 ```
 返回数据格式
 
 ```
 {
      success:bool(true/false),//表示是否成功
      code:0/1 ,//执行结果编码，目前只有0成功，1失败
      total:long,//表示查询到的记录数
      data:[], //表示返回的记录列表详情
      hint:string //服务器返回的提示信息，一般在访问失败时给出。
 }
 ```
 以下示例使用httpClient实现，查询times>10的历史记录，采用分页查询，查第一页数据。代码如下：
 
 ```
 HttpClient client = HttpClients.createDefault();
 HttpPost post = new HttpPost("http://localhost:8081/api/queryHistoryList");
 //不分页查询，列表查询为POST请求方式，条件为project=
 StringBuilder stringBuilder = new StringBuilder();
 stringBuilder.append("{")
              .append("   pageinfo:{from:0,size:1},")
              .append("  query:{")
              .append("   times:{$gt:10}")
              .append("  }")
              .append("}");
 StringEntity stringEntity = new StringEntity(stringBuilder.toString());
 post.setEntity(stringEntity);
 HttpResponse response = client.execute(post);
 HttpEntity res = response.getEntity();
 System.out.println(EntityUtils.toString(res));
 ```
 以上代码等价于如下请求体：
 
 ```
 POST /api/queryHistoryList
 {
  pageinfo:{
   from:0,
   size:1
  },
  query:{
   times:{$gt:10}
  }
 }
 ```
 返回结果为：
 
 ```
 {
  "success":true,
  "code":0,
  "total":27,
  "data": [
   {
    "startsAt":1.511320572E9,
    "endsAt":1.511330962E9,
    "lastNotifyTime":1.511330952E9,
    "lastReceiveTime":1.511330962E9,
    "times":196.0,
    "status":"resolve",
    "labels":{
      "alertname":"mymetric11",
      "group":"my1",
      "instance":"localhost:8080",
      "job":"tomcat",
      "monitor":"codelab-monitor",
      "project":"project1"
     },
    "_index":"alert-201711",
    "_id":"247D78214DCCD7FE830EC039F2B310C4-1511320572"
   }
 ]
}
 ```
<h5 id="1.2.2">1.2.2.历史告警详情查询</h5>
查询格式

```
GET /queryAlertingById/{id}//id为记录id
```
 返回数据格式
 
 ```
 {
      success:bool(true/false),//表示是否成功
      code:0/1 ,//执行结果编码，目前只有0成功，1失败
      total:long,//表示查询到的记录数
      data:{}, //表示返回的记录详情信息
      hint:string //服务器返回的提示信息，一般在访问失败时给出。
 }
 ```
 我们使用httpclient模拟GET请求，查询指定id的历史数据详情，代码如下：
 
 ```
  HttpClient client = HttpClients.createDefault();
  HttpGet get = 
     new HttpGet("http://localhost:8081/api/queryHistoryById/alert-201711/247D78214DCCD7FE830EC039F2B310C4-1511320572");
  HttpResponse response = client.execute(get);
  HttpEntity res = response.getEntity();
  System.out.println(EntityUtils.toString(res));
 ```
 以上代码等价于：
 
 ```
 GET /api/queryHistoryById/alert-201711/247D78214DCCD7FE830EC039F2B310C4-1511320572
 ```
 返回的数据格式为：
 
 ```
 {
  "success":true,
  "code":0,
  "total":1,
  "data":{
   "startsAt":1.511320572E9,
   "endsAt":1.511330962E9,
   "lastNotifyTime":1.511330952E9,
   "lastReceiveTime":1.511330962E9,
   "times":196.0,
   "status":"resolve",
   "labels":{
     "alertname":"mymetric11",
     "group":"my1",
     "instance":"localhost:8080",
     "job":"tomcat",
     "monitor":"codelab-monitor",
     "project":"project1"
   },
  "_index":"alert-201711",
  "_id":"247D78214DCCD7FE830EC039F2B310C4-1511320572"
 }
}
 ```
 <h5 id="1.2.3">1.2.3.历史告警搜索</h5>
 历史告警搜索是指，根据输入的关键字查询与之相关的历史告警数据，返回符合条件的前10条记录。访问的数据格式为：
 
 ```
 GET /api/searchHistoryList/{searchstr}
 ```
 返回的数据格式为：
 
 ```
 {
      success:bool(true/false),//表示是否成功
      code:0/1 ,//执行结果编码，目前只有0成功，1失败
      total:long,//表示查询到的记录数
      data:[], //表示返回的记录列表详情
      hint:string //服务器返回的提示信息，一般在访问失败时给出。
 }
 ```
 我们用HTTPClient来模拟GET请求，查询包含tomcat字符串的记录：
 
 ```
 HttpClient client = HttpClients.createDefault();
 HttpGet get = new HttpGet("http://localhost:8081/api/searchHistoryList/tomcat");
 HttpResponse response = client.execute(get);
 HttpEntity res = response.getEntity();
 System.out.println(EntityUtils.toString(res));
 ```
 以上代码等价于：
 
 ```
 GET /api/searchHistoryList/tomcat
 ```
 返回的数据格式为：
 
 ```
 {
  "success":true,
  "code":0,
  "total":42,
  "data":[
   {
    "startsAt":1.511320572E9,
    "endsAt":1.511330962E9,
    "lastNotifyTime":1.511330952E9,
    "lastReceiveTime":1.511330962E9,
    "times":196.0,
    "status":"resolve",
    "labels":{
      "alertname":"mymetric11",
      "group":"my1",
      "instance":"localhost:8080",
      "job":"tomcat",
      "monitor":"codelab-monitor",
      "project":"project1"
    },
   "_index":"alert-201711",
   "_id":"247D78214DCCD7FE830EC039F2B310C4-1511320572"
  },
  ···
 ]
}
```
<h3 id="2">2.系统配置</h3>
<h4 id="2.1">2.1配置信息刷新</h4>
