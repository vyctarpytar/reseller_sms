package com.spa.smart_gate_springboot.ndovuPay;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ResellerNdovuPayRepository extends JpaRepository<ResellerNdovuPay, UUID> {
    Optional<ResellerNdovuPay> findByNdResellerId(UUID id);
}
