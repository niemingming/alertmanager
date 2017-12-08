package com.haier.alertmanager.template;

import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @description 提示信息的简单模板处理类，用于转换模板信息，模板格式${object.name}形式，模板会自动替换里面值
 * @date 2017/11/16
 * @author Niemingming
 */
@Component
public class SimpleTemplate {
    //匹配规则，用于匹配${object.name}格式的字符串
    private Pattern pattern = Pattern.compile("\\$\\{\\w+(\\.\\w+)*\\}");

    /**
     * @description 分析模板格式，将占位符找出，并返回。
     * @date 2017/11/16
     * @author Niemingming
     */
    public Set<String> compileTemplate(String templateStr){
        Set<String> vars = new HashSet<String>();
        Matcher matcher = pattern.matcher(templateStr);
        while(matcher.find()){
            matcher.start();
            vars.add(matcher.group());
            matcher.end();
        }
        return vars;
    }
    /**
     * @description 分析模板数据，并用实际业务数据替换占位符
     * @date 2017/11/16
     * @author Niemingming
     */
    public String decodeTemplate(String source,Map values){
        //处理空字符串
        if (source == null ||"".equals(source) || "null".equals(source)){
            return "";
        }
        Set<String> keys = compileTemplate(source);
        for (String key : keys){
            String[] fields = key.substring(2,key.length()-1).split("\\.");
            Object value = values;
            for (String field:fields){
                if (value instanceof Map){
                    value = ((Map)value).get(field);
                    //如果没有值，用空字符串替换
                    if (value == null ){
                        System.out.println("传入值，未能找到属性：" + field + "");
                        value = "";
                        break;
                    }
                }else {
                    System.out.println("传入值，未能找到属性：" + field + "");
                    value = "";
                    break;
                }
            }
            source = source.replace(key,value.toString());
        }
        return source;
    }
}
