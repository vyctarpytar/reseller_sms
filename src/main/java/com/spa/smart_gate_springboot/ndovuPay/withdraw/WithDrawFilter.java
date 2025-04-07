package com.spa.smart_gate_springboot.ndovuPay.withdraw;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.Date;
import java.util.UUID;

@Builder
@Data
@AllArgsConstructor
public class WithDrawFilter {
    private String withDrawStatus;
    @JsonIgnore
    private UUID withDrawCreatedBy;
    private String withDrawCreatedByName;
    private UUID withDrawAccId;
    private Date withDrawCreatedDate;
    private Date withDrawDateFrom;
    private Date withDrawDateTo;
    private String withDrawSubmobileNo;
    private UUID withDrawResellerId;
    private int start;
    private int limit;
    private String sortColumn;

}
