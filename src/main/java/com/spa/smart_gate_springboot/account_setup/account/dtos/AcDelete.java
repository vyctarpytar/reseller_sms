package com.spa.smart_gate_springboot.account_setup.account.dtos;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class AcDelete {
    private String acDeleteReason;
}
