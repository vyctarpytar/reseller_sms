package com.spa.smart_gate_springboot.account_setup.account.dtos;

import lombok.*;

import java.math.BigDecimal;
import java.util.Date;
import java.util.UUID;

@Data
public class AcDisableDto {
    private Date accDisableDate;
    private int accToDiableTimer;
}

