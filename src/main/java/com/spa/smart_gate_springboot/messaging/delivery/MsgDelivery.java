package com.spa.smart_gate_springboot.messaging.delivery;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@ToString
@Table(schema = "msg")
@Entity(name = "delivery")
@Builder
public class MsgDelivery {
    @Id
    @GeneratedValue
    private UUID msgdId;

    @Column(nullable = false, updatable = false)
    @NotNull(message = "msgdMsgId is null")
    private UUID msgdMsgId;

    private String msgdDelCode;
    private LocalDate msgdDate;
    private LocalDateTime msgdTimestamp;
    private String msgdStatus;
    private String msgdSender;
    private String msgdLogs;
    private String msgdIsRetry;
    private String msgdWhy;
    private String msgdPrsp;

}