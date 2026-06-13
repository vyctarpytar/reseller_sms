package com.spa.smart_gate_springboot.messaging.send_message;

import com.spa.smart_gate_springboot.account_setup.account.AccountService;
import com.spa.smart_gate_springboot.messaging.send_message.safaricom_sdp.SafBulkService;
import com.spa.smart_gate_springboot.utils.UniqueCodeGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Single owner of "persist + debit + send an SMS arc". Every path that bills or dispatches a message —
 * the RabbitMQ consumer (first send), the retry cron (resend failed), and the resend-on-credit flows —
 * goes through here, so the billing rules live in exactly one place.
 *
 * <h3>Idempotency</h3>
 * A message carries a stable {@code msgDedupKey} (stamped at publish). {@link #reserveAndDebit} and
 * {@link #persistPending} insert the arc keyed by that column; the unique index makes a redelivery fail
 * the insert with {@link DataIntegrityViolationException}, which callers treat as "already processed,
 * skip" — so a RabbitMQ redelivery can never double-debit or double-send. Insert happens in the SAME
 * transaction as the debit (save-before-debit), so a crash between them can't leave units debited with
 * no record (the old order debited first, then saved — losing units if the save failed).
 *
 * <h3>Reserve-then-send billing</h3>
 * Units are reserved exactly ONCE, at first processing. Retries ({@link #resendBilled}) re-send the
 * same arc without re-debiting; the carrier id ({@code msgCode}) is regenerated per attempt for DLR
 * correlation while the idempotency key stays fixed. The arc is the source of truth and is never
 * deleted — failures persist as rows the retry cron re-drives.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SmsDispatchService {

    private final MsgMessageQueueArcRepository arcRepo;
    private final AccountService accountService;
    private final SafBulkService safBulkService;

    public enum Reservation {
        /** Inserted + debited; caller should now dispatch the carrier send. */
        RESERVED,
        /** Inserted but the account couldn't cover it; persisted as PENDING_CREDIT, no debit, no send. */
        NO_CREDIT
    }

    /**
     * First processing of a queued message: insert the arc (keyed by its unique {@code msgDedupKey})
     * and, in the same transaction, debit the account. The carrier send is intentionally NOT done here
     * — it's external I/O and must run outside the DB transaction (caller invokes {@link #dispatchSend}
     * when this returns {@link Reservation#RESERVED}).
     *
     * @throws DataIntegrityViolationException if the dedup key already exists (redelivery) — the whole
     *         transaction rolls back (nothing inserted, nothing debited) and the caller skips the message.
     */
    @Transactional
    public Reservation reserveAndDebit(MsgMessageQueueArc arc) {
        arcRepo.saveAndFlush(arc); // flush now so a duplicate dedup key surfaces here (rolls back the tx)
        if (accountService.tryDebitAccountMsgBal(arc.getMsgAccId(), arc.getMsgCostId())) {
            arc.setMsgStatus("PENDING_PROCESSING");
            return Reservation.RESERVED;
        }
        arc.setMsgStatus("PENDING_CREDIT"); // managed entity — flushed on commit; resend-credit picks it up
        return Reservation.NO_CREDIT;
    }

    /**
     * Re-send an arc that was ALREADY billed on its first attempt (the retry-cron path). No re-debit —
     * units stay reserved. A fresh {@code msgCode} is minted so the new attempt's DLRs correlate to it,
     * and {@code msgSentRetried} is set so the cron retries each message at most once.
     */
    public void resendBilled(MsgMessageQueueArc arc) {
        arc.setMsgCode(new UniqueCodeGenerator().generateSecureApiKey());
        arc.setMsgStatus("PENDING_PROCESSING");
        arc.setMsgSentRetried(true);
        arcRepo.save(arc);
        dispatchSend(arc);
    }

    /**
     * Resend an arc that was persisted but never billed (PENDING_CREDIT / RS_CREDIT_ISSUE), used when an
     * account/reseller tops up. Debits the existing arc in place and, if funded, sends it. No new row,
     * no delete.
     *
     * @return true if it was funded and dispatched, false if still short on credit (left pending).
     */
    public boolean debitAndResend(MsgMessageQueueArc arc) {
        if (arc.getMsgCostId() == null) {
            // A pending arc must carry its cost (every persist path records it); guard so a stray
            // null-cost row can't NPE the whole top-up batch — log and leave it for inspection.
            log.error("[SMS] PENDING_CREDIT arc {} has null cost — skipping resend (dedup {})",
                    arc.getMsgId(), arc.getMsgDedupKey());
            return false;
        }
        if (!accountService.tryDebitAccountMsgBal(arc.getMsgAccId(), arc.getMsgCostId())) {
            return false; // still no credit — leave it pending for the next top-up
        }
        arc.setMsgCode(new UniqueCodeGenerator().generateSecureApiKey());
        arc.setMsgStatus("PENDING_PROCESSING");
        arcRepo.save(arc);
        dispatchSend(arc);
        return true;
    }

    /**
     * Perform the carrier send for an already-persisted, already-billed arc. The send path records its
     * own HTTP-level outcome (SENT / ERROR) in the DB; this only guards the transport-level throw
     * (timeout, token failure) by marking the arc ERROR so the retry cron re-drives it. Never throws —
     * a send failure must not abort the caller (consumer ack / cron batch loop).
     */
    public void dispatchSend(MsgMessageQueueArc arc) {
        try {
            safBulkService.sendArcSms(arc);
        } catch (Exception e) {
            log.error("[SMS] carrier send threw for msgCode {} (dedup {}): {}",
                    arc.getMsgCode(), arc.getMsgDedupKey(), e.getMessage());
            arcRepo.updateInitialReceiveNote("ERROR", 0, List.of(arc.getMsgCode()), "SEND_EXCEPTION");
        }
    }
}
