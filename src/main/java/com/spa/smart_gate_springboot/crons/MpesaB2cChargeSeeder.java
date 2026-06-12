package com.spa.smart_gate_springboot.crons;

import com.spa.smart_gate_springboot.payment.mpesa.charge.MpesaB2cCharge;
import com.spa.smart_gate_springboot.payment.mpesa.charge.MpesaB2cChargeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Seeds the M-Pesa B2C withdrawal charge band table on startup if empty.
 * Standard Safaricom B2C (Transfer to M-Pesa Users / Business) tariff, KES.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Order(40)
public class MpesaB2cChargeSeeder implements ApplicationRunner {

    private final MpesaB2cChargeRepository repo;

    @Override
    public void run(ApplicationArguments args) {
        if (repo.count() > 0) return;

        List<MpesaB2cCharge> rates = List.of(
                charge(1,      49,     0),
                charge(50,     100,    0),
                charge(101,    500,    7),
                charge(501,    1000,   13),
                charge(1001,   1500,   23),
                charge(1501,   2500,   33),
                charge(2501,   3500,   53),
                charge(3501,   5000,   57),
                charge(5001,   7500,   78),
                charge(7501,   10000,  90),
                charge(10001,  15000,  100),
                charge(15001,  20000,  105),
                charge(20001,  35000,  108),
                charge(35001,  50000,  108),
                charge(50001,  250000, 108)
        );

        repo.saveAll(rates);
        log.info("[MpesaB2cCharge] Seeded {} charge tiers", rates.size());
    }

    private MpesaB2cCharge charge(long min, long max, long fee) {
        return MpesaB2cCharge.builder()
                .minAmount(BigDecimal.valueOf(min))
                .maxAmount(BigDecimal.valueOf(max))
                .charge(BigDecimal.valueOf(fee))
                .build();
    }
}
