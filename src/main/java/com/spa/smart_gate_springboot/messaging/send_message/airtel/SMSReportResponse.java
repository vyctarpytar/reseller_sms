package com.spa.smart_gate_springboot.messaging.send_message.airtel;

import lombok.Data;

import java.util.List;

@Data
public class SMSReportResponse {
    private List<SMSReport> results;
}
