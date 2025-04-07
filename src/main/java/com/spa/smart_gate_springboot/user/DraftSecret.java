package com.spa.smart_gate_springboot.user;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Getter
@AllArgsConstructor
@Builder
@Setter
@RequiredArgsConstructor
public class DraftSecret {
    @NotNull(message = "Field Cannot be null")
    private UUID usrId;
    @NotNull(message = "Field Cannot be null")
    private String usrSecret;
}
