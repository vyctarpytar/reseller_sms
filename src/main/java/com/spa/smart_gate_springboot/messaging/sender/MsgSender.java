package com.spa.smart_gate_springboot.messaging.sender;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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
@Entity(name = "senders")
@Builder
public class MsgSender {
    @Id
    @GeneratedValue

    private UUID msgId;
    private String msgSender;
    private LocalDateTime msgTime;
    private String msgDesc;
    private UUID msgMsgId;
    private LocalDate msgDate;
    private String msgWhyResent;
    private String msgDelStatus;

}
