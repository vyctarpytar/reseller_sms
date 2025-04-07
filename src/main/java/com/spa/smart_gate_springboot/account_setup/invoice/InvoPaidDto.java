package com.spa.smart_gate_springboot.account_setup.invoice;

import com.spa.smart_gate_springboot.dto.Layers;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;

@Getter
@Setter
@RequiredArgsConstructor
public class InvoPaidDto {
    private String invoMarkedPaidReference;
    private Date invoMarkedPaidValueDate;
    private BigDecimal invoMarkedPaidAmount;
    private String invoStatus;
}
