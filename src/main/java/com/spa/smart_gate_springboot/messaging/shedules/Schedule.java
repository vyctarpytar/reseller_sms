package com.spa.smart_gate_springboot.messaging.shedules;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.UUID;


@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@ToString
@Table(schema = "msg" ,uniqueConstraints = {@UniqueConstraint(columnNames = {"schAccId","schGrpId", "schMessage","schCreatedOn"})})
@Entity(name = "msg_schedule")
@Builder
public class Schedule {
    @Id
    @GeneratedValue
    private UUID schId;
    @Column(nullable = false)
    private UUID schCreatedById;
    @Column(nullable = false)
    private String schCreatedByName;
    @Column(nullable = false)
    private UUID schAccId;
    private UUID schGrpId;
    @Column(updatable = false)
    private LocalDateTime schCreatedOn;
    @Column(nullable = false)
    private String schMessage;
    @Column(nullable = false)
    private String schReleaseTime;
    @Column(nullable = false)
    private String schSenderid;
    private String schPhoneNumber;
    private String schSourceIp;

    @Builder.Default
    @Column(nullable = false)
    private String schStatus = "PENDING";

    private UUID schUpdatedById;
    private String schUpdatedByName;
    private  LocalDateTime schUpdatedOn;

    private String schGroupName;
}

