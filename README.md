# alertmanager
## 目录
* [1.数据查询](#1)
  * [1.1.当前告警查询](#1.1)
      * [1.1.1.告警列表查询](#1.1.1)
      * [1.1.2.告警详情查询](#1.1.2)
      * [1.1.3.告警数据统计](#1.1.3)
  * [1.2.历史告警查询](#1.2)
      * [1.2.1.历史告警列表查询](#1.2.1)
      * [1.2.2.历史告警详情查询](#1.2.2)
      * [1.2.3.历史告警搜索](#1.2.3)
  * [1.3.公共编码查询](#1.3)
      * [1.3.1.告警级别编码查询](#1.3.1)
      * [1.3.2.告警分类编码查询](#1.3.2)
      * [1.3.3.告警类型编码查询](#1.3.3)
      * [1.3.4.告警编码查询](#1.3.4)
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
         currentPage:10, //当前页码，从1开始
         pageSize:10  //每页显示多少条，默认10条，
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
      data:{
        page:{
         total:4,
         currentPage:1
        },
        list:[
         {
          startsAt:...
         }
        ]
      }, //表示返回的记录列表详情
      msg:string //服务器返回的提示信息，一般在访问失败时给出。
 }
 ```
 我们用httpClient来模拟post请求，尝试查询project=project1的数据。具体请求代码如下：
 
 ```
 HttpClient client = HttpClients.createDefault();
 HttpPost post = new HttpPost("http://localhost:8081/api/queryAlertingList");
 //不分页查询，列表查询为POST请求方式，条件为project=
 StringBuilder stringBuilder = new StringBuilder();
 stringBuilder.append("{")
              .append("   pageinfo:{currentPage:1,pageSize:1},")
              .append("  query:{")
              .append(" \"project\":[\"project1\",\"project2\"],") //支持in查询
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
 pageinfo:{currentPage:1,pageSize:1}
 query:{
    "project":["project1","project2"]
  }
}
```
返回的数据格式为：

```
{
 "success":true,
 "code":0,
 "data":{
  "page":{
    "total":2,
    "currentPage":1
   },
  "list":[{
   "_id":"FC6EF20EED02AA884745283049CDE2B2",
   "startsAt":1511778207,
   "endsAt":-62135798400,
   "lastNotifyTime":1511778214,
   "lastReceiveTime":1511778292,
   "times":18,
   "status":"firing",
   "level":"warning",
   "alertId":"FC6EF20EED02AA884745283049CDE2B2-1511778207",
   "project":"project2",
   "alertname":"mymetric11",
   "labels":{
    "instance":"localhost:8080",
    "monitor":"codelab-monitor",
    "job":"tomcat",
    "group":"my1"
    }
   }
  ]
 }
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
      msg:string //服务器返回的提示信息，一般在访问失败时给出。
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
 "data":{
  "_id":"247D78214DCCD7FE830EC039F2B310C4",
  "startsAt":1511778207,
  "endsAt":-62135798400,
  "lastNotifyTime":1511778350,
  "lastReceiveTime":1511778427,
  "times":44,
  "status":"firing",
  "level":"warning",
  "alertId":"247D78214DCCD7FE830EC039F2B310C4-1511778207",
  "project":"project1",
  "alertname":"mymetric11",
  "labels":{
   "instance":"localhost:8080",
   "monitor":"codelab-monitor",
   "job":"tomcat",
   "group":"my1"
  }
 }
}
```
<h5 id="1.1.3">1.1.3.告警数据统计</h5>
查询条件和统计项，统计当前告警信息。具体格式如下：
</br>查询格式

```
* POST /api/queryAlertingByGroup
     * {
     *     query:{//查询条件，遵循mongo的查询格式，如果为空，则查询全部数据
     *          alertname:"testalert",
     *          "labels.job":"tomcat",
     *          times:{$gte:"10"}
     *     },
     *     group:["level","project"]//可以按照多个字段，也可以按照一个字段
     * }
     
```
返回数据格式：

```
*{
     *     success:bool(true/false),//表示是否成功
     *     code:0/1 ,//执行结果编码，目前只有0成功，1失败
     *     data:{
     *         list:[]
     *     }, //表示返回的记录列表详情
```
以下示例使用httpclient实现，查询project的name为"project1","project2"的记录，并按照"project"分组。代码如下：

```
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
```
以上代码等价于：

```
POST /api/queryAlertingByGroup
{
 "query":{"project":["project1","project2"]},
 group:["project"]
}
```
返回数据为：

```
{
 "success":true,
 "code":0,
 "data":{
  "list":[{
   "count":1,
   "project":"project1"
  },{
   "count":1,
   "project":"project2"
  }
 ]
}
}
```
<h4 id="1.2">1.2历史告警查询</h4>
历史告警信息查询，按照统一性原则，与当前告警查询格式一致，只不过数据来源有区别。当前告警数据来源于MongoDB数据库，历史告警数据来源于ElasticSearch。
<h5 id="1.2.1">1.2.1.历史告警列表查询</h5>
查询格式

```
POST /api/queryHistoryList
{
     pageinfo:{//分页信息如果不传，表示不分页
         currentPage:10, //当前页码，从1开始
         pageSize:10  //每页显示多少条，默认10条
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
      data:{
       page:{
        total:4,
        currentPage:1
       },
       list:[]
      }, //表示返回的记录列表详情
      msg:string //服务器返回的提示信息，一般在访问失败时给出。
 }
 ```
 以下示例使用httpClient实现，查询times>10的历史记录，采用分页查询，查第一页数据。代码如下：
 
 ```
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
   currentPage:1,
   pageSize:1
  },
  query:{
  "project":["project1","project2"],
   times:{$gt:10}
  }
 }
 ```
 返回结果为：
 
 ```
 {
  "success":true,
  "code":0,
  "data":{
   "page":{
    "total":4,
    "currentPage":1
   },
   "list":[{
    "startsAt":1511772777,
    "endsAt":1511778097,
    "lastNotifyTime":1511774254,
    "lastReceiveTime":1511778097,
    "times":473,
    "status":"resolve",
    "level":"warning",
    "project":"project1",
    "alertname":"mymetric11",
    "labels":{
     "instance":"localhost:8080",
     "monitor":"codelab-monitor",
     "job":"tomcat",
     "group":"my1"
    },
    "_index":"alert-201711",
    "_id":"247D78214DCCD7FE830EC039F2B310C4-1511772777"
   },{
    "startsAt":1511772777,
    "endsAt":1511778097,
    "lastNotifyTime":1511774239,
    "lastReceiveTime":1511778107,
    "times":470,
    "status":"resolve",
    "level":"warning",
    "project":"project2",
    "alertname":"mymetric11",
    "labels":{
     "instance":"localhost:8080",
     "monitor":"codelab-monitor",
     "job":"tomcat",
     "group":"my1"
    },
    "_index":"alert-201711",
    "_id":"FC6EF20EED02AA884745283049CDE2B2-1511772777"
   }
  ]
 }
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
      msg:string //服务器返回的提示信息，一般在访问失败时给出。
 }
 ```
 我们使用httpclient模拟GET请求，查询指定id的历史数据详情，代码如下：
 
 ```
  HttpClient client = HttpClients.createDefault();
  HttpGet get = 
     new HttpGet("http://localhost:8081/api/queryHistoryById/alert-201711/FC6EF20EED02AA884745283049CDE2B2-1511772777");
  HttpResponse response = client.execute(get);
  HttpEntity res = response.getEntity();
  System.out.println(EntityUtils.toString(res));
 ```
 以上代码等价于：
 
 ```
 GET /api/queryHistoryById/alert-201711/FC6EF20EED02AA884745283049CDE2B2-1511772777
 ```
 返回的数据格式为：
 
 ```
 {
  "success":true,
  "code":0,"data":{
   "startsAt":1511772777,
   "endsAt":1511778097,
   "lastNotifyTime":1511774239,
   "lastReceiveTime":1511778107,
   "times":470,
   "status":"resolve",
   "level":"warning",
   "project":"project2",
   "alertname":"mymetric11",
   "labels":{
    "instance":"localhost:8080",
    "monitor":"codelab-monitor",
    "job":"tomcat",
    "group":"my1"
   },
   "_index":"alert-201711",
   "_id":"FC6EF20EED02AA884745283049CDE2B2-1511772777"
  }
 }
 ```
 <h5 id="1.2.3">1.2.3.历史告警搜索</h5>
 历史告警搜索是指，根据输入的关键字查询与之相关的历史告警数据，返回符合条件的前10条记录。访问的数据格式为：
 
 ```
 GET /api/searchHistoryList/{searchstr}?currentPage=1&pageSize=10
 ```
 返回的数据格式为：
 
 ```
 {
      success:bool(true/false),//表示是否成功
      code:0/1 ,//执行结果编码，目前只有0成功，1失败
      data:{
       page:{
        total:4,
        currentPage:1
       } ,
       list:[]
      }, //表示返回的记录列表详情
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
 //如果不传currentPage和pageSize，系统会默认currentPage=1，pageSize=10
 GET /api/searchHistoryList/tomcat?currentPage=1&pageSize=10
 ```
 返回的数据格式为：
 
 ```
 {
  "success":true,
  "code":0,
  "data":{
   "page":{
     "total":32,
     "currentPage":1
   },
```
<h4 id="1.3">1.3公共编码查询</h4>
查询告警板中用到的公共编码。
<h5 id="1.3.1">1.3.1告警级别编码查询</h5>
查询告警板配置的公共编码
访问格式为：

```
GET /api/queryAlertLevels
```
返回的数据格式为：

```
{
     *      success:true,
     *      code:0/1,
     *      data:{
     *          error:"紧急",
     *          warn:"严重",
     *          info:"一般",
     *          debug:"提示"
     *      }
     * }
```
下面我们使用httpClient模拟访问操作如下：

```
        HttpClient client = HttpClients.createDefault();
        HttpGet get = new HttpGet("http://localhost:8081/api/queryAlertLevels");
        HttpResponse response = client.execute(get);
        HttpEntity res = response.getEntity();
        System.out.println(EntityUtils.toString(res));
```
返回的数据为：

```
{
 "success":true,
 "code":0,
 "data":{
  "page":{
    "total":44
   },
   "list":[{
    "startsAt":1511772777,
    "endsAt":1511778097,
    "lastNotifyTime":1511774254,
    "lastReceiveTime":1511778097,
    "times":473,
    "status":"resolve",
    "level":"warning",
    "project":"project1",
    "alertname":"mymetric11",
    "labels":{
     "instance":"localhost:8080",
     "monitor":"codelab-monitor",
     "job":"tomcat",
     "group":"my1"
    },
    "_index":"alert-201711",
    "_id":"247D78214DCCD7FE830EC039F2B310C4-1511772777"
   },
   ···
  ]
 }
}
```
<h5 id="1.3.2">1.3.2告警分类编码查询</h5>
查询告警板配置的告警分类编码
访问格式为：

```
GET /api/queryAlertCategories
```
返回数据为：

```
{
 "success":true,
 "code":0,
 "data":{
  "machine":"机器",
  "app":"应用"
 }
}
```
<h5 id="1.3.3">1.3.3告警类型编码查询</h5>
查询告警板配置的告警类型编码
访问格式为：

```
GET /api/queryAlertTypes
```
返回数据为：

```
{
 "success":true,
 "code":0,
 "data":{
  "cockpit_schedule_task_exit":"容器实例退出",
  "mysql_status_handlers_read_rnd":"mysql索引不合理",
  "service_down":"服务不可用",
  "node_reboot":"系统重启",
  "node_cpu_pct_threshold_exceeded":"节点CPU使用率过高",
  "node_mem_threshold_exceeded":"节点剩余内存不足",
  "node_mem_pct_threshold_exceeded":"节点内存使用率过高",
  "node_fs_pct_threshold_exceeded":"节点文件系统使用率过高",
  "node_tcp_conn_toomuch":"节点TCP连接数过高",
  "node_disk_io_util_threshold_exceeded":"节点磁盘IO过高",
  "redis_service_down":"redis服务不可用",
  "redis_mem_pct_threshold_exceeded":"Redis内存使用率过高",
  "redis_mem_threshold_exceeded":"Redis内存不足",
  "redis_toomany_command_executed":"Redis命令执行频繁",
  "redis_dangerous_command_executed":"Redis执行危险命令"
 }
}
```
<h5 id="1.3.4">1.3.4告警编码查询</h5>
查询告警板配置的所有告警编码
访问格式为：

```
GET /api/queryAlertCode
```
返回的数据格式为：

```
{
 "success":true,
 "code":0,
 "data":{
  "alertType":{
   "cockpit_schedule_task_exit":"容器实例退出",
   "mysql_status_handlers_read_rnd":"mysql索引不合理",
   "service_down":"服务不可用",
   "node_reboot":"系统重启",
   "node_cpu_pct_threshold_exceeded":"节点CPU使用率过高",
   "node_mem_threshold_exceeded":"节点剩余内存不足",
   "node_mem_pct_threshold_exceeded":"节点内存使用率过高",
   "node_fs_pct_threshold_exceeded":"节点文件系统使用率过高",
   "node_tcp_conn_toomuch":"节点TCP连接数过高",
   "node_disk_io_util_threshold_exceeded":"节点磁盘IO过高",
   "redis_service_down":"redis服务不可用",
   "redis_mem_pct_threshold_exceeded":"Redis内存使用率过高",
   "redis_mem_threshold_exceeded":"Redis内存不足",
   "redis_toomany_command_executed":"Redis命令执行频繁",
   "redis_dangerous_command_executed":"Redis执行危险命令"
  },
  "alertCategory":{
   "machine":"机器",
   "app":"应用"
  },
  "alertLevel":{
   "error":"紧急",
   "warn":"严重",
   "info":"一般",
   "debug":"提示"
  }
 }
}
```
<h3 id="2">2.系统配置</h3>
<h4 id="2.1">2.1配置信息刷新</h4>
