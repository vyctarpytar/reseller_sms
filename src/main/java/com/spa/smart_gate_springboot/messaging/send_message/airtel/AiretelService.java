package com.spa.smart_gate_springboot.messaging.send_message.airtel;

import com.spa.smart_gate_springboot.utils.AppTime;

import com.spa.smart_gate_springboot.account_setup.account.Account;
import com.spa.smart_gate_springboot.account_setup.account.AccountService;
import com.spa.smart_gate_springboot.account_setup.senderId.MsnProvider;
import com.spa.smart_gate_springboot.account_setup.senderId.ShortCodeService;
import com.spa.smart_gate_springboot.messaging.send_message.MsgMessageQueueArc;
import com.spa.smart_gate_springboot.messaging.send_message.MsgMessageQueueArcRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiretelService {

    private final MsgMessageQueueArcRepository arcRepository;
    private final AirtelNumberRepository airtelNumberRepository;
    private final RestTemplate restTemplate;
    private final ObjectProvider<AccountService> accountServiceProvider;
    private final ObjectProvider<ShortCodeService> shortCodeServiceProvider;

    /** Safaricom DLR status that marks a recipient as unreachable on Safaricom. */
    public static final String DELIVERY_IMPOSSIBLE = "DeliveryImpossible";

    /** Total tries at the Airtel gateway per send — the first attempt plus four retries. */
    private static final int AIRTEL_SEND_ATTEMPTS = 5;

    @Value("${sms.airtel.allowForAll}")
    private  String allowForAll;

    /** Used only when the tenant has no sender ID registered on AIRTEL. */
    @Value("${sms.airtel.defaultSenderId:letstalk}")
    private String defaultSenderId;


    /**
     * Whether this recipient should go out through Airtel. There is no prefix rule: a number is Airtel
     * only once Safaricom has reported {@code DeliveryImpossible} for it (see
     * {@link #recordDeliveryImpossible(String)}), or when {@code sms.airtel.allowForAll} is on.
     */
    public boolean checkIsAirtel(String msisdn) {
        if (msisdn == null || msisdn.isBlank()) return false;
        if (Boolean.parseBoolean(allowForAll)) return true;

        return airtelNumberRepository.existsByAnNumber(normalizeMsisdn(msisdn));
    }

    /**
     * Record a recipient in {@code msg.airtel_numbers} because Safaricom answered
     * {@code DeliveryImpossible} for it. This is the only way a number gets into that table — sending
     * through the Airtel gateway does not, because that gateway also carries Safaricom traffic.
     * Never throws: a failure here must not break DLR processing.
     */
    public void recordDeliveryImpossible(String msisdn) {
        if (msisdn == null || msisdn.isBlank()) return;
        String normalized = normalizeMsisdn(msisdn);
        try {
            if (airtelNumberRepository.existsByAnNumber(normalized)) return;
            airtelNumberRepository.save(AirtelNumber.builder()
                    .anNumber(normalized)
                    .anReason(DELIVERY_IMPOSSIBLE)
                    .anCreatedDate(AppTime.now())
                    .build());
            log.info("Recorded {} as an Airtel number — Safaricom reported DeliveryImpossible", normalized);
        } catch (Exception e) {
            log.error("Failed to record Airtel number {}: {}", normalized, e.getMessage(), e);
        }
    }

    /**
     * Clear rows learned under the old rule (every recipient of an Airtel send, which swept in Safaricom
     * numbers). Idempotent — only rows without {@code an_reason} go — and never blocks startup.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void purgeLegacyAirtelNumbers() {
        try {
            int deleted = airtelNumberRepository.deleteLegacyRows();
            if (deleted > 0) {
                log.info("[AIRTEL] removed {} airtel_numbers row(s) learned under the old routing rule", deleted);
            }
        } catch (Exception e) {
            log.error("[AIRTEL] legacy airtel_numbers purge failed — old rows may still route to Airtel", e);
        }
    }

    private String normalizeMsisdn(String msisdn) {
        String s = msisdn.trim().replace("+", "");
        if (s.startsWith("0")) {
            s = "254" + s.substring(1);
        }
        return s;
    }


    public MsgMessageQueueArc callback(@Valid CallBackResp callBackResp) {
        List<MsgMessageQueueArc> arcList = arcRepository.findByMsgCode(callBackResp.getMessageId());
        for (MsgMessageQueueArc arc  : arcList){
            arc.setMsgStatus(callBackResp.deliveryDescription);
            arc.setMsgClientDeliveryStatus("PENDING");
            arc.setMsgRetryCount(0);
            arcRepository.save(arc);
            return arc;
        }
      return arcList.get(0);
    }

    /**
     * The Airtel sender ID to send this message with. The sender ID that arrived on the message is a
     * Safaricom one (that is the network the web UI picks from), so it cannot be reused here —
     * Airtel only accepts a sender ID registered on its own network. We therefore look up the
     * account's, then the reseller's, sender ID registered with {@link MsnProvider#AIRTEL}
     * ({@code sh_msn_provider}), and fall back to {@code sms.airtel.defaultSenderId} for tenants
     * that have not registered one — that fallback is what every send used unconditionally before
     * the column existed.
     */
    private String resolveAirtelSenderId(MsgMessageQueueArc msg) {
        UUID resellerId = msg.getMsgResellerId();
        if (resellerId == null && msg.getMsgAccId() != null) {
            Account account = accountServiceProvider.getObject().findByAccId(msg.getMsgAccId());
            resellerId = account == null ? null : account.getAccResellerId();
        }

        return shortCodeServiceProvider.getObject()
                .findSenderIdForProvider(msg.getMsgAccId(), resellerId, MsnProvider.AIRTEL)
                .orElseGet(() -> {
                    log.info("No AIRTEL sender ID registered for accId={} resellerId={} — using default '{}'",
                            msg.getMsgAccId(), msg.getMsgResellerId(), defaultSenderId);
                    return defaultSenderId;
                });
    }

    private BigDecimal getCostPerSMS(UUID msgAccId) {
        Account acc = this.accountServiceProvider.getObject().findByAccId(msgAccId);
        BigDecimal _sms_price = acc.getAccSmsPrice();
        return _sms_price == null ? new BigDecimal("1.50") : _sms_price;
    }

    /**
     * Send via Airtel and bill the account (historic behaviour). Used by callers that did NOT
     * already reserve the units — the API-sandbox path and the retry cron.
     */
    public void sendMessageViaAirTel(MsgMessageQueueArc msgMessageQueueArc) {
        sendMessageViaAirTel(msgMessageQueueArc, true);
    }

    /**
     * @param bill {@code true} to debit the account as part of the send (historic behaviour);
     *             {@code false} when the caller already reserved the units up-front (the gate ->
     *             SafBulkService path), so billing here would double-charge.
     */
    public void sendMessageViaAirTel(MsgMessageQueueArc msgMessageQueueArc, boolean bill) {

        int no_of_msg = getNoOfMessage(msgMessageQueueArc);

        BigDecimal cost_per_sms = getCostPerSMS(msgMessageQueueArc.getMsgAccId());
//        BigDecimal cost_per_sms = new BigDecimal("0.50");
        BigDecimal totalCost = cost_per_sms.multiply(new BigDecimal(no_of_msg));
        msgMessageQueueArc.setMsgPage(no_of_msg);
        msgMessageQueueArc.setMsgCostId(totalCost);

        String senderId = resolveAirtelSenderId(msgMessageQueueArc);

        msgMessageQueueArc.setMsgSenderIdName(senderId);

        // Prepare request body
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("message", msgMessageQueueArc.getMsgMessage());
        requestBody.put("apikey", "aad395b77c99dc80e48eee05d2cbbee6");
        requestBody.put("partnerID", "15086");
        requestBody.put("shortcode", senderId);
        requestBody.put("mobile", msgMessageQueueArc.getMsgSubMobileNo());

        log.info("Sending to Airtel : {}", requestBody);


        // Reserve-then-send: when this caller owns billing (bill=true — the API-sandbox and retry
        // paths), atomically debit the account up-front and skip the send if it can't be covered, so
        // the balance never goes negative and we never send an SMS we can't bill. bill=false means
        // the gate (MQReceiverSynq) already reserved the units, so we just send.
        if (bill) {
            boolean reserved = accountServiceProvider.getObject()
                    .tryDebitAccountMsgBal(msgMessageQueueArc.getMsgAccId(), totalCost);
            if (!reserved) {
                log.warn("Airtel send skipped — insufficient units for account {} (cost {})",
                        msgMessageQueueArc.getMsgAccId(), totalCost);
                msgMessageQueueArc.setMsgStatus("PENDING_CREDIT");
                msgMessageQueueArc.setMsgClientDeliveryStatus("PENDING");
                arcRepository.save(msgMessageQueueArc);
                return;
            }
        }


        try {
            SMSReport responsee = postToAirtelWithRetry(requestBody);
            msgMessageQueueArc.setMsgCreatedDate(AppTime.now());
            msgMessageQueueArc.setMsgCreatedTime(AppTime.now());
            msgMessageQueueArc.setMsgStatus(responsee.responses.get(0).responseDescription);
            msgMessageQueueArc.setMsgDeliveredDate(AppTime.now());
            msgMessageQueueArc.setMsgClientDeliveryStatus("PENDING");
            msgMessageQueueArc.setMsgRetryCount(0);
            msgMessageQueueArc.setMsgCode(responsee.responses.get(0).messageid);
            msgMessageQueueArc.setMsgErrorDesc(responsee.toString());
            msgMessageQueueArc.setMsgSenderLevel("switchport".toUpperCase());
            arcRepository.save(msgMessageQueueArc);

            log.info("Sent to AirTel : {}", responsee);
        } catch (Exception e) {
            log.error("Error sending Airtel message . {}", e.getMessage(), e);
            throw e;
        }
    }


    /**
     * POST the send to the Airtel gateway, retrying transient connection failures up to
     * {@value #AIRTEL_SEND_ATTEMPTS} times. The gateway drops out intermittently (DNS/connect
     * failures on bulksms.switchportltd.com), and without this a single blip leaves the message for
     * the retry cron minutes later.
     *
     * <p>Only failures that provably never reached the gateway are retried — see
     * {@link #neverLeftThisHost(ResourceAccessException)}. An HTTP error or a malformed response
     * means the gateway answered, so re-posting would risk a second SMS to a real recipient.
     *
     * <p>Note this runs on a {@code @Scheduled} thread when the retry cron calls it, and a dead host
     * can take ~10s per attempt, so the backoff is deliberately short.
     */
    private SMSReport postToAirtelWithRetry(Map<String, String> requestBody) {
        String AIRTEL_END_POINT = "https://bulksms.switchportltd.com/api/services/sendsms";
        long backoffMs = 500L;

        for (int attempt = 1; ; attempt++) {
            try {
                return restTemplate.postForObject(AIRTEL_END_POINT, requestBody, SMSReport.class);
            } catch (ResourceAccessException e) {
                if (!neverLeftThisHost(e)) {
                    log.error("Airtel send failed and may already have been delivered — not retrying. mobile={}, cause={}",
                            requestBody.get("mobile"), String.valueOf(e.getCause()));
                    throw e;
                }
                if (attempt >= AIRTEL_SEND_ATTEMPTS) {
                    log.error("Airtel gateway unreachable after {} attempts. mobile={}",
                            attempt, requestBody.get("mobile"));
                    throw e;
                }
                log.warn("Airtel gateway unreachable (attempt {}/{}): {}. Retrying in {}ms",
                        attempt, AIRTEL_SEND_ATTEMPTS, e.getMessage(), backoffMs);
                try {
                    Thread.sleep(backoffMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw e;
                }
                backoffMs = Math.min(backoffMs * 2, 4000L);
            }
        }
    }

    /**
     * Whether the request demonstrably never reached the gateway, making a retry free of any risk of
     * sending the SMS twice.
     *
     * <p>DNS, connection-refused and no-route failures all happen before a single byte goes out. A
     * <em>read</em> timeout is the dangerous one: the POST was delivered and the gateway simply did
     * not answer within 30s, so the SMS may well have gone out and re-posting would bill the account
     * again and double-text a real person. Connect timeouts never established a socket, so they are
     * safe — the two share {@link SocketTimeoutException}, and only the message distinguishes them.
     */
    private boolean neverLeftThisHost(ResourceAccessException e) {
        Throwable cause = e.getCause();
        if (cause instanceof UnknownHostException
                || cause instanceof ConnectException
                || cause instanceof NoRouteToHostException) {
            return true;
        }
        if (cause instanceof SocketTimeoutException) {
            return String.valueOf(cause.getMessage()).toLowerCase().contains("connect");
        }
        return false;
    }

    private int getNoOfMessage(MsgMessageQueueArc msgQueue) {
        int MSG_LENGTH = msgQueue.getMsgMessage().length();

        int no_of_characters_per_message = 160;
        return (int) Math.ceil((double) MSG_LENGTH / no_of_characters_per_message);

    }





}
