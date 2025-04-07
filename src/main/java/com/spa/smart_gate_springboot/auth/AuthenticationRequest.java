package com.spa.smart_gate_springboot.auth;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthenticationRequest {
    @NotNull(message = "Field Cannot be null")
    String password;
    @NotNull(message = "Field Cannot be null")
    private String email;
}
