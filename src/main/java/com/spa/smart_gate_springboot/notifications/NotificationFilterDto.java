package com.spa.smart_gate_springboot.notifications;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationFilterDto {

    private String snStatus;
    private String search;
    private int start;
    private int limit;
    private String sortColumn;
}
