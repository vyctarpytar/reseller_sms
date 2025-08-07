package com.spa.smart_gate_springboot.messaging.send_message.dtos;

import com.spa.smart_gate_springboot.messaging.send_message.safaricom_sdp.safaricom.dto.Datum;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class RequestParam {
    private List<Datum> data;
}
