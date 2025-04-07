package com.spa.smart_gate_springboot.messaging.send_message.safaricom_sdp.safaricom.models;

import lombok.Data;

import java.util.List;

@Data
public class SendBulkSafReq {

    private String timeStamp;

    private List<SafBulkDataSet> dataSet;


}
