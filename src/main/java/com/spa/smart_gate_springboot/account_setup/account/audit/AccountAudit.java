package com.spa.smart_gate_springboot.account_setup.account.audit;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.UUID;

@AllArgsConstructor
@RequiredArgsConstructor
@Getter
@Setter
@Builder
@Table(schema = "js_core")
@Entity(name = "jsc_accounts_audit")
public class AccountAudit{
    @Id
    @GeneratedValue
    private UUID accId;
    private UUID accActionBy;
    private String accActionByName;
    private LocalDateTime accActionDate;
    private String accAction;
}

