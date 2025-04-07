package com.spa.smart_gate_springboot.account_setup.invoice;

import lombok.Data;

import java.util.Date;
import java.util.UUID;

@Data
public class InvoiceFilter{
    private String invoCode;
    private UUID invoAccId;
    private UUID invoResellerId;
    private String invoPayerMobileNumber;
    private String invoStatus;
    private Date invoDate;
    private int start;
    private int limit;
    private String sortColumn;
}
