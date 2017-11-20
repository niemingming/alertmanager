package com.haier.alertmanager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * @description alertmanager的启动类
 * @date 2017/11/16
 * @author Niemingming
 */
@SpringBootApplication
@ComponentScan("com.haier.alertmanager")
public class AlertManagerWebApplication {

    public static void main(String[] args){
        //应用程序启动类。
        SpringApplication.run(AlertManagerWebApplication.class,args);
    }
}
