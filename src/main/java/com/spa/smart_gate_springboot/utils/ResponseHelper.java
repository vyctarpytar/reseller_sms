package com.spa.smart_gate_springboot.utils;

import java.util.HashMap;

public class ResponseHelper {


    public void setData(String key, Object value, StandardJsonResponse response) {
        HashMap<String, Object> map = new HashMap<>();
        map.put(key, value);
        response.setData(map);
    }

    public void setMessage(String key, Object value, StandardJsonResponse response) {
        HashMap<String, Object> map = new HashMap<>();
        map.put("message", value);
        response.setMessages(map);
    }
}
