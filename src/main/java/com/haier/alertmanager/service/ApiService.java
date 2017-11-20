package com.haier.alertmanager.service;

import com.haier.alertmanager.container.AlertDictionaryContainer;
import com.haier.alertmanager.container.AlertExcluseContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

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

    @ResponseBody
    @RequestMapping("/refresh/{flag}")
    public String refreshCache(@PathVariable String flag){
        if ("excluse".equals(flag)){
            //指刷新白名单
            alertExcluseContainer.refresh();
        }else if("dict".equals(flag)){
            //指刷新配置信息
            alertDictionaryContainer.refresh();
        }else{
            alertExcluseContainer.refresh();
            alertDictionaryContainer.refresh();
        }
        return "success";
    }

}
