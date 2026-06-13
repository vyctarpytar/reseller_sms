package com.spa.smart_gate_springboot.messaging.send_message;

import com.spa.smart_gate_springboot.messaging.send_message.airtel.AiretelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

@Component
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class SchedulingConfig {
    private final MsgMessageQueueArcRepository arcRepository;
    private final AiretelService airetelService;
    private final SmsDispatchService smsDispatchService;

    /** Statuses that mean "the send failed — try again". */
    private static final List<String> RETRYABLE_STATUSES =
            List.of("Exception sending", "ERROR", "ERRORR", "DeliveryImpossible");
    /** Safaricom couldn't deliver — this status falls back to Airtel instead of re-queuing to Synq. */
    private static final String DELIVERY_IMPOSSIBLE = "DeliveryImpossible";

    /** Max messages retried per tick. The throughput knob — tune up if a failure spike backs up. */
    @Value("${sms.retry.batch-size:500}")
    private int retryBatchSize;

    /**
     * Retry failed sends.
     * <p>
     * ONE bounded, indexed query across all tenants per tick (see
     * {@link MsgMessageQueueArcRepository#findRetryBatch}). This replaces the previous design, which
     * every 5s looped <em>every</em> reseller × 4 statuses and, for each, loaded the reseller's
     * accounts and ran a 500-row {@code IN (...)} query — so DB load grew with tenant count and could
     * overlap itself. Now it's O(batch) per tick regardless of how many resellers exist.
     * <p>
     * {@code fixedDelay} (not {@code fixedRate}) guarantees a slow tick never overlaps the next one.
     * Single-instance + per-row delete + the {@code msg_sent_retried} filter prevent reprocessing; if
     * this ever runs on more than one instance, switch {@code findRetryBatch} to a claim-style
     * {@code UPDATE ... RETURNING} (or {@code FOR UPDATE SKIP LOCKED}) so instances don't double-send.
     * <p>
     * <b>Prod index</b> (run once, {@code CONCURRENTLY} so it doesn't lock the big table):
     * <pre>
     * CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_mqa_retry
     *   ON msg.message_queue_arc (msg_created_date)
     *   WHERE msg_sent_retried IS NOT TRUE;
     * </pre>
     */
    @Scheduled(fixedDelayString = "${sms.retry.interval-ms:5000}")
    public void retryFailedMessages() {
        List<MsgMessageQueueArc> batch = arcRepository.findRetryBatch(RETRYABLE_STATUSES, retryBatchSize);
        if (batch.isEmpty()) return;

        log.info("Retry cron: processing {} failed message(s)", batch.size());
        for (MsgMessageQueueArc m : batch) {
            try {
                retryOne(m);
            } catch (Exception e) {
                log.error("Retry failed for msgCode {} : {}", m.getMsgCode(), e.getMessage());
            }
        }
    }

    private void retryOne(MsgMessageQueueArc m) {
        // In-place retry: the arc is the source of truth and is NEVER deleted (it's an archive row). Units
        // were already reserved on the first attempt (reserve-then-send), so a retry re-sends WITHOUT
        // re-debiting. Setting msgSentRetried=true bounds it to ONE retry per message — findRetryBatch
        // excludes it afterwards — so a permanently failing message can't loop.
        if (DELIVERY_IMPOSSIBLE.equalsIgnoreCase(m.getMsgStatus())) {
            log.info("Saf failed :- Now Sending to Airtel");
            m.setMsgSentRetried(true);
            arcRepository.save(m);
            airetelService.sendMessageViaAirTel(m, false); // bill=false: already reserved on the Saf attempt
            airetelService.saveAirtelNumberWithRetry(m.getMsgSubMobileNo());
        } else {
            smsDispatchService.resendBilled(m); // fresh msgCode + re-send the existing arc; no delete, no re-debit
        }
    }


    //disable temporarily
//    @Scheduled(fixedRate = 1000 * 60)
    public void resendSentStatusWithinHrs() {
        try {
            PageRequest pageRequest = PageRequest.of(0, 100);
            Page<MsgMessageQueueArc> pagedData = arcRepository.resendSentStatusAfter4hrs(pageRequest);
            // In-place re-send (no delete, no re-debit) — same model as retryOne.
            pagedData.getContent().forEach(smsDispatchService::resendBilled);
        } catch (Exception e) {
            log.error(e.getLocalizedMessage());
        }

    }


    @Scheduled(fixedRate = 1000 * 60 * 5)
    public void health() {
        log.info("Health Check Cron sms:   {}", new Date());
    }


}
