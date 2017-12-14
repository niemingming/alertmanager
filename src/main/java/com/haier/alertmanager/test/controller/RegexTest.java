package com.haier.alertmanager.test.controller;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexTest {

    public static void main(String[] args){
        String pp = "fw*";
        pp = pp.replace("*","\\w*");
        Pattern p = Pattern.compile( pp);
        String str = "fooos";
        Matcher m = p.matcher(str);
        System.out.println(m.matches());
    }
}
