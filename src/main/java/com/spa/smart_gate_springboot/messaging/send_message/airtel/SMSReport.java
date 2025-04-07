package com.spa.smart_gate_springboot.messaging.send_message.airtel;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class SMSReport {
    private Date doneAt;
    private Integer smsCount;
    private String messageId;
    private Date sentAt;
//    private Error error;
    private String bulkId;
    private String mccMnc;
    private Price price;
    private String callbackData;
    private String from;
    private String to;
    private String text;
    private Status status;
}

