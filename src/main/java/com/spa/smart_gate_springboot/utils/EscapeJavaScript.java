package com.spa.smart_gate_springboot.utils;

import org.springframework.stereotype.Service;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.net.URLDecoder;

@Service
public class EscapeJavaScript {
//    static ScriptEngineManager manager = new ScriptEngineManager();
//    static ScriptEngine engine = manager.getEngineByName("nashorn");


//    public static String escape2(String input) {
//        if (engine == null) {
//            throw new RuntimeException("Script engine not found");
//        }
//        String escaped = input;
//        try {
//
//            Object result = engine.eval(input);
//            escaped = result.toString().replaceAll("%20", "")
//                    .replaceAll("%3A", ":").replaceAll("%2F", "/").replaceAll("%3B", ";").replaceAll("%40", "@")
//                    .replaceAll("%3C", "<").replaceAll("%3E", ">").replaceAll("%3D", "=").replaceAll("%26", "&")
//                    .replaceAll("%25", "%").replaceAll("%24", "$").replaceAll("#", "%23").replaceAll("%2B", "+")
//                    .replaceAll("%2C", ",").replaceAll("%3F", "?");
//
//        } catch (Exception e) {
//            // e.printStackTrace();
//        }
//        return escaped;
//    }

    public static String escape2(String input) {
        String escaped = input;
        try {
            escaped = URLDecoder.decode(input, "UTF-8");
        } catch (Exception e) {
            // Handle or log the exception
            e.printStackTrace();
        }
        return escaped;
    }

}