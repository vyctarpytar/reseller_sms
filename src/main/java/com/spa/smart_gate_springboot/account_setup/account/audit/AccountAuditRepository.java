package com.spa.smart_gate_springboot.account_setup.account.audit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;
@Repository
public interface AccountAuditRepository extends JpaRepository<AccountAudit, UUID> {}
