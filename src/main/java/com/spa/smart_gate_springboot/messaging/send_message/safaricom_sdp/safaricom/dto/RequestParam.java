package com.spa.smart_gate_springboot.messaging.send_message.safaricom_sdp.safaricom.dto;

import lombok.Data;

import java.util.ArrayList;

@Data
public class RequestParam{
    public ArrayList<Datum> data;
}
