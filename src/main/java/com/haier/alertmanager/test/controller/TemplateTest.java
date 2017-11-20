package com.haier.alertmanager.test.controller;

import com.haier.alertmanager.template.SimpleTemplate;

import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TemplateTest {

    public static void main(String[]args) throws ParseException, IOException {
        String str = "我们走在${labels}asdfa上，${labels.name}天涯${fff}";
//        Pattern pattern = Pattern.compile("\\$\\{\\w+(\\.\\w+)*\\}");
//        System.out.println(pattern.pattern());
//        String [] strs = pattern.split(str);
//        Matcher matcher = pattern.matcher(str);
//        while (matcher.find()){
//            matcher.start();
//            String str1 = matcher.group();
//            str1 = str1.substring(2,str1.length()-1);
//            matcher.end();
//
//            System.out.println(str1);
//            System.out.println(str1.indexOf("."));
//        }
//        Map map = new HashMap();
//        Map labels = new HashMap();
//        map.put("labels",labels);
//        labels.put("name","王五");
//        SimpleTemplate template = new SimpleTemplate();
//        String res = template.decodeTemplate(str,map);
//        System.out.println(str);
//        System.out.println(res);
//
//        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
//        str = "2017-11-09 23:24:42.806";
//        sdf.parse(str);

        FileWriter fw = new FileWriter("/errorrecord.log",true);
        fw.write("第二行");
        fw.write("\r\n");
        fw.close();
    }
}
