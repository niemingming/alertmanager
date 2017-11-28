package com.haier.alertmanager.test.controller;

import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

@Component
public class Test {

    private List list = new ArrayList();
    private boolean flag = true;

    public Test(){
    }
//    @PostConstruct
    public void xh(){
        while(true){
//            list.remove(0);
            System.out.println(11);
            Thread tread = new Thread(new Runnable() {
                @Override
                public void run() {

                }
            });
        }
    }

    public void receive(){
        synchronized (Test.class){
            if (flag){
                xh();
                flag = false;
            }
        }
        list.add("11");
        return;
    }


}
