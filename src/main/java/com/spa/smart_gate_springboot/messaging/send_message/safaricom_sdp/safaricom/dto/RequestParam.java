package com.spa.smart_gate_springboot.messaging.send_message.safaricom_sdp.safaricom.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class RequestParam{

    private List<Datum> data;
}
