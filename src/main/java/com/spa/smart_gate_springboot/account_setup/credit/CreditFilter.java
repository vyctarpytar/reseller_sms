package com.spa.smart_gate_springboot.account_setup.credit;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@RequiredArgsConstructor
public class CreditFilter{
    private String smsResellerName;
    private String smsAccountName;
    private String  smsCreatedByName;
    private LocalDateTime smsCreatedDate;
    private String  smsApprovedByName;
    private LocalDateTime smsApprovedDate;
    private String crStatus;
    private UUID accId;
    private UUID resellerId;
    private UUID saleUserId;
    private int start;
    private int limit;
    private String sortColumn;
}
