package com.spa.smart_gate_springboot.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Builder
@AllArgsConstructor
@Data
public class StatusDto {
    private String usrStatus;
}
