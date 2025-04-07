package com.spa.smart_gate_springboot.account_setup.account.dtos;

import lombok.*;

import java.util.Date;
import java.util.UUID;

@Data

public class AcFilterDto {
    private String accName;
    private String accStatus;
    private UUID accAccId;
    private Date accCreatedDate;
    private Date accDateFrom;
    private Date accDateTo;
    private String accOfficeMobile;
    private int start;
    private int limit;
    private String sortColumn;
}

