package com.github.mstepan.template;

import com.github.mstepan.template.ds.RobinHoodHashMap;

public class AppMain {

    static void main() throws Exception {

        RobinHoodHashMap<String, Integer> map = new RobinHoodHashMap<>();

        map.put("one", 1);
        map.put("two", 2);
        map.put("three", 3);
        map.put("four", 4);
        map.put("five", 5);
        map.put("six", 6);
        map.put("seven", 7);
        map.put("eight", 8);
        map.put("nine", 9);
        map.put("ten", 10);

        //        System.out.println(map.get("one"));

        System.out.printf("Java version: %s. Main done...%n", System.getProperty("java.version"));
    }
}
