package com.spa.smart_gate_springboot.crons;

import com.spa.smart_gate_springboot.payment.mpesa.b2c.B2cTransaction;
import com.spa.smart_gate_springboot.payment.mpesa.b2c.B2cTransactionRepository;
import com.spa.smart_gate_springboot.payment.mpesa.b2c.B2cTransactionStatus;
import com.spa.smart_gate_springboot.payment.mpesa.gateway.WaretechMpesaService;
import com.spa.smart_gate_springboot.payment.mpesa.gateway.dto.GatewayStatusResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Polls the Waretech gateway for terminal results of PROCESSING B2C payouts and flips them to
 * SUCCESS/FAILED. The wallet was already debited at initiation (gross), so:
 *   - SUCCESS: record the M-Pesa receipt; no further wallet movement.
 *   - FAILED:  mark FAILED only. We deliberately do NOT auto-credit the wallet back (avoids
 *              double-refunds on uncertain terminal states) — an admin reverses via the wallet API.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MpesaB2cPayoutsCron {

    private final B2cTransactionRepository b2cRepo;
    private final WaretechMpesaService gatewayService;

    /** A PROCESSING payout older than this with no terminal result is given up on and marked FAILED. */
    private static final Duration STUCK_THRESHOLD = Duration.ofHours(24);

    @Scheduled(fixedRate = 10_000)
    public void pollProcessingPayouts() {
        List<B2cTransaction> processing = b2cRepo.findByStatus(B2cTransactionStatus.PROCESSING);
        if (processing.isEmpty()) return;

        for (B2cTransaction tx : processing) {
            // Stuck-row guard: never poll a payout forever. After the threshold, mark it FAILED (not
            // reversed) so an admin can investigate and reverse via the wallet API if funds didn't move.
            if (tx.getCreatedAt() != null
                    && Duration.between(tx.getCreatedAt(), LocalDateTime.now()).compareTo(STUCK_THRESHOLD) > 0) {
                tx.setStatus(B2cTransactionStatus.FAILED);
                tx.setFailureReason("Poller gave up after " + STUCK_THRESHOLD.toHours()
                        + "h with no terminal gateway result — needs manual reconciliation");
                b2cRepo.save(tx);
                log.warn("[B2C] {} marked FAILED (stuck >{}h, wallet still debited — admin must reconcile)",
                        tx.getConversationId(), STUCK_THRESHOLD.toHours());
                continue;
            }
            if (tx.getConversationId() == null) continue;
            try {
                GatewayStatusResponse status = gatewayService.getStatus(tx.getConversationId());
                if (status == null) continue; // not yet recorded — keep polling

                if (status.isSuccess()) {
                    tx.setStatus(B2cTransactionStatus.SUCCESS);
                    tx.setMpesaReceipt(status.resolveReceipt());
                    tx.setResponseDescription(status.getResultDesc());
                    b2cRepo.save(tx);
                    log.info("[B2C] {} SUCCESS receipt={}", tx.getConversationId(), tx.getMpesaReceipt());
                } else if (status.isFailed()) {
                    tx.setStatus(B2cTransactionStatus.FAILED);
                    tx.setFailureReason(status.getResultDesc());
                    b2cRepo.save(tx);
                    log.warn("[B2C] {} FAILED: {} (wallet stays debited — admin must reverse)",
                            tx.getConversationId(), status.getResultDesc());
                }
                // else: still pending at the gateway — keep polling
            } catch (Exception e) {
                log.error("[B2C] poll error for {}: {}", tx.getConversationId(), e.getMessage());
            }
        }
    }
}
