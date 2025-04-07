package com.spa.smart_gate_springboot.account_setup.member;

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
@Builder
@Table(schema = "js_core")
@Entity(name = "grp_members")
public class ChMember {
    @Id
    @GeneratedValue
    private UUID chId;
    @NotNull(message = "Provide group Id")
    private UUID chGroupId;
    @NotNull(message = "Provide Acc Id")
    private UUID chAccId;
    @Column(nullable = false)
    private String chFirstName;
    private String chOtherName;
    private String chGenderCode;
    private LocalDate chDob;
    private String chNationalId;
    @Column(nullable = false)
    private String chTelephone;
    private String chOption1;
    private String chOption2;
    private String chOption3;
    private String chOption4;
    private LocalDateTime chMemberCreatedDateTime;
    private UUID chMemberCreatedBy;
}



