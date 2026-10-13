package com.spa.smart_gate_springboot.account_setup.smpp;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SmppConnectionRepo extends JpaRepository<SmppConnection, UUID> {

    List<SmppConnection> findAllByOrderByCreatedAtDesc();

    List<SmppConnection> findByResellerIdOrderByCreatedAtDesc(UUID resellerId);

    List<SmppConnection> findByAccIdOrderByCreatedAtDesc(UUID accId);

    // Deliberately across every type and state: system_id is unique table-wide on the gateway side.
    boolean existsBySystemIdIgnoreCase(String systemId);

    boolean existsByAccIdAndConnectionTypeAndIsActiveTrue(UUID accId, String connectionType);
}
