package com.spa.smart_gate_springboot.messaging.send_message;

import com.spa.smart_gate_springboot.utils.AppTime;

import com.spa.smart_gate_springboot.messaging.send_message.airtel.AiretelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;


import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

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



    @Scheduled(fixedDelayString = "${sms.retry.interval-ms:5000}")
    public void retryFailedMessages() {
        List<MsgMessageQueueArc> batch = arcRepository.findRetryBatch(
                RETRYABLE_STATUSES, AppTime.today().minusDays(2).atStartOfDay(), retryBatchSize);
        if (batch.isEmpty()) return;

        List<MsgMessageQueueArc> airtelFallback = new ArrayList<>();
        List<MsgMessageQueueArc> resend = new ArrayList<>();
        for (MsgMessageQueueArc m : batch) {
            (DELIVERY_IMPOSSIBLE.equalsIgnoreCase(m.getMsgStatus()) ? airtelFallback : resend).add(m);
        }

        log.info("Retry cron: processing {} failed message(s) — {} via Airtel fallback, {} re-sent via Synq",
                batch.size(), airtelFallback.size(), resend.size());

        markRetried(airtelFallback);                  // 1 UPDATE for the whole Airtel half
        smsDispatchService.resendBilledBatch(resend); // 1 UPDATE for the whole Synq half, then sends
        sendViaAirtel(airtelFallback);
    }

    /** Flag the Airtel half as retried in one statement, and mirror it onto the in-memory arcs. */
    private void markRetried(List<MsgMessageQueueArc> msgs) {
        if (msgs.isEmpty()) return;

        StringJoiner ids = new StringJoiner(",");
        for (MsgMessageQueueArc m : msgs) {
            m.setMsgSentRetried(true);
            ids.add(m.getMsgId().toString());
        }
        arcRepository.markSentRetried(ids.toString());
    }

    /**
     * Carrier I/O only — the retried flag is already persisted by {@link #markRetried}. The Airtel send
     * records its own outcome (and registers the MSISDN as an Airtel number when it isn't known yet), so
     * nothing here writes to the DB.
     */
    private void sendViaAirtel(List<MsgMessageQueueArc> msgs) {
        for (MsgMessageQueueArc m : msgs) {
            try {
                log.info("Saf failed :- Now Sending to Airtel");
                airetelService.sendMessageViaAirTel(m, false); // bill=false: already reserved on the Saf attempt
            } catch (Exception e) {
                log.error("Retry failed for msgCode {} : {}", m.getMsgCode(), e.getMessage());
            }
        }
    }


    //disable temporarily
//    @Scheduled(fixedRate = 1000 * 60)
    public void resendSentStatusWithinHrs() {
        try {
            PageRequest pageRequest = PageRequest.of(0, 100);
            Page<MsgMessageQueueArc> pagedData = arcRepository.resendSentStatusAfter4hrs(
                    AppTime.now().getHour() - 4, AppTime.today(), pageRequest);
            // In-place re-send (no delete, no re-debit) — same model as retryOne.
            pagedData.getContent().forEach(smsDispatchService::resendBilled);
        } catch (Exception e) {
            log.error(e.getLocalizedMessage());
        }

    }
    


}
