package com.haier.alertmanager.test.controller;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/testmongo")
public class MongoDBTest {
    @Autowired
    private MongoTemplate mongoTemplate;
    /**
     * @description 测试javamongo新增操作，尝试自己赋值id
     * @date 2017/11/16
     * @author Niemingming
     */
    @ResponseBody
    @RequestMapping("/insert/{colname}/{name}/{age}")
    public String insert (@PathVariable String colname,@PathVariable String name,@PathVariable int age){
        DBObject object = new BasicDBObject();
        object.put("_id",10);
        object.put("name",name);
        object.put("age",age);
//        mongoTemplate.insert(object,"test");
        DBObject query = new BasicDBObject();
        query.put("_id",10);
        mongoTemplate.getCollection(colname).update(query,object,true,false);
        mongoTemplate.getCollection(colname).count();
        return "success";
    }
}
