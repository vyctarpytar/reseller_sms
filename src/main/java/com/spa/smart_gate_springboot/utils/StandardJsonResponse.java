package com.spa.smart_gate_springboot.utils;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;

@Data
public class StandardJsonResponse  extends ResponseHelper{
    private boolean success = true;
    private HashMap<String, Object> messages = new HashMap<>();
    private HashMap<String, Object> data = new HashMap<>();
    private int total = 0;
    private String targetUrl;
    private Object token;
    private int status = 200;

}
