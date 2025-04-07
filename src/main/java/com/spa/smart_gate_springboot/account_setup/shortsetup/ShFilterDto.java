package com.spa.smart_gate_springboot.account_setup.shortsetup;

import lombok.*;

import java.util.Date;
import java.util.UUID;


@Builder
@Data
@Getter
@Setter
@AllArgsConstructor
@RequiredArgsConstructor
public class ShFilterDto {
    private String  shStatus;
    private UUID shAccId;
    private String shSenderId;
    private Date shCreatedDate;
    private UUID shSaleUserId;
    private UUID shResellerId;
    private int start;
    private int limit;
    private String sortColumn;
}
