package com.spa.smart_gate_springboot.messaging.send_message.airtel;

import lombok.Data;

import java.util.ArrayList;

@Data
public class SMSReport{
    public ArrayList<Response> responses;
}
