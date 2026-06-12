package com.spa.smart_gate_springboot.payment.mpesa.charge;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * M-Pesa B2C transaction charge band table. Each row is the service charge applied to a
 * withdrawal whose gross amount falls within [minAmount, maxAmount]. The charge is deducted
 * from the payout (recipient bears it).
 */
@Entity
@Table(name = "mpesa_b2c_charge", schema = "msg")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MpesaB2cCharge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "min_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal minAmount;

    @Column(name = "max_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal maxAmount;

    @Column(name = "charge", nullable = false, precision = 10, scale = 2)
    private BigDecimal charge;
}
