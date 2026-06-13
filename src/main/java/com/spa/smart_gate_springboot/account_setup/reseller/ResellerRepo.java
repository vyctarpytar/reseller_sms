package com.spa.smart_gate_springboot.account_setup.reseller;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface ResellerRepo extends JpaRepository<Reseller, UUID> {
    List<Reseller> findByRsCreatedBy(UUID createdBy);

    /** Reseller census for the platform (TOP) overview cards: active vs inactive. */
    @Query("select count(r) from Reseller r where r.isActive = :active")
    long countByActiveFlag(@Param("active") boolean active);

    /** Total units still sitting in reseller pools (not yet allocated to accounts). */
    @Query("select coalesce(sum(r.rsAllocatableUnit), 0) from Reseller r")
    BigDecimal sumAllocatableUnits();
}
