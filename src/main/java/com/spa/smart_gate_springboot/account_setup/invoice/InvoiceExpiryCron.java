package com.spa.smart_gate_springboot.account_setup.invoice;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Sweeps STK top-ups that never produced a callback (the customer ignored the SIM prompt and it
 * lapsed) so they don't sit in PENDING_PAYMENT forever. The stkCallback handler covers explicit
 * cancel/failure; this covers the "no callback at all" case after the invoice due date.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class InvoiceExpiryCron {

    private final InvoiceService invoiceService;

    @Scheduled(fixedDelay = 60_000)
    public void expireStalePending() {
        try {
            int expired = invoiceService.expireStalePending();
            if (expired > 0) {
                log.info("[Invoice] Expired {} stale PENDING_PAYMENT invoice(s)", expired);
            }
        } catch (Exception e) {
            log.error("Invoice expiry sweep failed: {}", e.getMessage(), e);
        }
    }
}
