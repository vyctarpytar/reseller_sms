package com.spa.smart_gate_springboot.messaging.templates;

import lombok.*;

import java.util.UUID;


@Builder
@Data
@Getter
@Setter
@AllArgsConstructor
@RequiredArgsConstructor
public class TempFilterDto {
    private UUID tmpAccId;
    private String tmpMessage;
    private String tmpName;
    private int start;
    private int limit;
    private String sortColumn;
}
