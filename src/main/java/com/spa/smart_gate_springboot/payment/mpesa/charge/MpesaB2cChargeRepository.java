package com.spa.smart_gate_springboot.payment.mpesa.charge;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface MpesaB2cChargeRepository extends JpaRepository<MpesaB2cCharge, Long> {

    @Query("SELECT c FROM MpesaB2cCharge c WHERE :amount >= c.minAmount AND :amount <= c.maxAmount ORDER BY c.minAmount ASC")
    Optional<MpesaB2cCharge> findByAmount(@Param("amount") BigDecimal amount);

    List<MpesaB2cCharge> findAllByOrderByMinAmountAsc();
}
