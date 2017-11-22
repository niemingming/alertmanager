# alertmanager
## 目录
* [1.数据查询](#1)
  * [1.1.当前告警查询](#1.1)
    * [1.1.1.告警列表查询](#1.1.1)
    * [1.1.2.告警详情查询](#1.1.2)
  * [1.2.历史告警查询](#1.2)
    * [1.2.1.历史告警列表查询](#1.2.1)
    * [1.2.2.历史告警详情查询](#1.2.2)
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
 示例（待补充）
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
 示例（待补充）
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
 示例（待补充）
<h3 id="2">2.系统配置</h3>
<h4 id="2.1">2.1配置信息刷新</h4>
