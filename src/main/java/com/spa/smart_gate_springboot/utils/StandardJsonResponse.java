package com.spa.smart_gate_springboot.utils;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Data
public class StandardJsonResponse  extends ResponseHelper{
    private boolean success = true;
    private Map<String, Object> messages = new HashMap<>();
    private Map<String, Object> data = new HashMap<>();
    private int total = 0;
    private int status = 200;

}
