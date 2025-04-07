package com.spa.smart_gate_springboot.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Builder
@Data
@RequiredArgsConstructor
@AllArgsConstructor
public class ThirdPartyResponse {
    private String resultCode;
    private String resultDesc;
}
