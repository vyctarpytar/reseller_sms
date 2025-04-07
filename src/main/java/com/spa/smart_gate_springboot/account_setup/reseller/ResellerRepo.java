package com.spa.smart_gate_springboot.account_setup.reseller;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ResellerRepo extends JpaRepository<Reseller, UUID> {
    List<Reseller> findByRsHasNdovuPayAccountIsFalseOrRsHasNdovuPayAccountIsNull();
    List<Reseller> findByRsCreatedBy(UUID createdBy);
}
