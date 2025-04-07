package com.spa.smart_gate_springboot.user;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Getter
@AllArgsConstructor
@Builder
@Setter
@RequiredArgsConstructor
public class UserSecret {
    @NotNull(message = "Field Cannot be null")
    private String userSecret;
}
