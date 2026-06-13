package com.spa.smart_gate_springboot.messaging.send_message;

import com.spa.smart_gate_springboot.account_setup.account.Account;
import com.spa.smart_gate_springboot.account_setup.account.AccountRepository;
import com.spa.smart_gate_springboot.messaging.send_message.safaricom_rest.SafaricomRestAuthService;
import com.spa.smart_gate_springboot.messaging.send_message.safaricom_rest.SafaricomRestInterface;
import com.spa.smart_gate_springboot.messaging.send_message.safaricom_rest.SafaricomRestProperties;
import com.spa.smart_gate_springboot.messaging.send_message.safaricom_rest.dto.DsdpBulkDataSet;
import com.spa.smart_gate_springboot.messaging.send_message.safaricom_rest.dto.SafaricomRestSendRequest;
import com.spa.smart_gate_springboot.messaging.send_message.safaricom_rest.dto.SafaricomRestSendResponse;
import com.spa.smart_gate_springboot.messaging.send_message.safaricom_sdp.safaricom.SafAuthService;
import com.spa.smart_gate_springboot.messaging.send_message.safaricom_sdp.safaricom.SafaricomInterface;
import com.spa.smart_gate_springboot.messaging.send_message.safaricom_sdp.safaricom.SafaricomProperties;
import com.spa.smart_gate_springboot.messaging.send_message.safaricom_sdp.safaricom.dto.BulkResponse;
import com.spa.smart_gate_springboot.messaging.send_message.safaricom_sdp.safaricom.models.SafBulkDataSet;
import com.spa.smart_gate_springboot.messaging.send_message.safaricom_sdp.safaricom.models.SendBulkSafReq;
import com.spa.smart_gate_springboot.utils.AppUtils;
import com.spa.smart_gate_springboot.utils.UniqueCodeGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.util.TextUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import retrofit2.Call;
import retrofit2.Response;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * Sends internal/system SMS (OTPs, password notifications, admin alerts) through the same Safaricom
 * integration that powers the bulk-SMS product — replacing the retired third-party OTP gateway.
 *
 * Unlike the billable {@code QueueMsgService} path, these messages are not tied to the recipient's
 * account: there is no per-user message-balance debit and no shortcode lookup, and the sender ID is
 * fixed to {@value #SYSTEM_SENDER_ID} on the transactional package. The cost of every system SMS is
 * instead borne by a single dedicated account ({@link #SYSTEM_ACCOUNT_ID} / {@link #SYSTEM_RESELLER_ID}),
 * debited directly with a synchronous, guarded unit debit (best-effort — never blocks delivery).
 * Delivery is fire-and-forget on a background thread so callers never block (or fail) on the gateway.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SystemSmsService {

    /** Fixed sender ID for all system-originated SMS. */
    public static final String SYSTEM_SENDER_ID = "SYNQSMS";

    /** Account / reseller that bears the cost of every system SMS. */
    public static final UUID SYSTEM_ACCOUNT_ID = UUID.fromString("c40f39a1-3fe4-465f-a0c7-4df1a372ee8c");
    public static final UUID SYSTEM_RESELLER_ID = UUID.fromString("9918cd85-6cde-4bd2-a04d-3c3aaca8d009");

    /** Fallback per-SMS price (KES) when the system account has no rate configured. */
    private static final BigDecimal DEFAULT_SMS_PRICE = new BigDecimal("1.50");
    private static final int CHARS_PER_SMS = 160;

    // v1 (legacy SDP)
    private final SafAuthService safAuthService;
    private final SafaricomInterface safaricomInterface;
    private final SafaricomProperties safaricomProperties;

    // v2 (Daraja REST)
    private final SafaricomRestAuthService safaricomRestAuthService;
    private final SafaricomRestInterface safaricomRestInterface;
    private final SafaricomRestProperties safaricomRestProperties;

    // Billing — the repo is used directly (not AccountService) to avoid a circular dependency:
    // AccountService → UserService → SystemSmsService.
    private final AccountRepository accountRepository;

    /** Mirrors {@code SafBulkService}: switch between legacy SDP (v1) and Daraja REST (v2). */
    @Value("${safaricom.api.version:v1}")
    private String safApiVersion;

    /** Fire-and-forget: send a single transactional SMS from {@value #SYSTEM_SENDER_ID}. */
    public void sendSms(String msisdn, String message) {
        if (TextUtils.isEmpty(msisdn) || TextUtils.isEmpty(message)) return;

        final String cleanMsisdn = msisdn.replace("+", "").replace("-", "").trim();
        final String cleanMessage = message.replaceAll("[^*A-Za-z0-9@$_\\/.,\"():;\\-=+&%#!?<>' \\n]", "");

        new Thread(() -> {
            try {
                // Bill the dedicated system account first (mirrors the bulk pipeline, which debits
                // before sending and does not refund on gateway failure).
                billSystemAccount(cleanMessage);

                if ("v2".equalsIgnoreCase(safApiVersion)) {
                    sendViaRest(cleanMsisdn, cleanMessage);
                } else {
                    sendViaSdp(cleanMsisdn, cleanMessage);
                }
            } catch (Exception e) {
                log.error("[SYSTEM-SMS] Failed to send to {}: {}", cleanMsisdn, e.getMessage());
            }
        }).start();
    }

    /**
     * Debit the dedicated system account for this SMS — synchronous, atomic and guarded (the
     * {@code acc_msg_bal >= cost} predicate lives in {@code updateAccountMsgBal}) so the balance can
     * never go negative. Billing is best-effort: a system SMS (OTP, alert) must never be blocked or
     * fail on a billing hiccup, so we log and proceed regardless of the outcome.
     */
    private void billSystemAccount(String message) {
        try {
            BigDecimal price = accountRepository.findById(SYSTEM_ACCOUNT_ID)
                    .map(Account::getAccSmsPrice)
                    .orElse(DEFAULT_SMS_PRICE);
            if (price == null) price = DEFAULT_SMS_PRICE;

            int pages = (int) Math.ceil((double) message.length() / CHARS_PER_SMS);
            if (pages < 1) pages = 1;
            BigDecimal cost = price.multiply(BigDecimal.valueOf(pages));

            int debited = accountRepository.updateAccountMsgBal(SYSTEM_ACCOUNT_ID, cost);
            if (debited == 0) {
                log.warn("[SYSTEM-SMS] System account {} has insufficient units for cost {} — sending anyway",
                        SYSTEM_ACCOUNT_ID, cost);
            }
        } catch (Exception e) {
            // Never block delivery on a billing hiccup — log and proceed.
            log.error("[SYSTEM-SMS] Failed to debit system account: {}", e.getMessage());
        }
    }

    private void sendViaSdp(String msisdn, String message) throws Exception {
        String accessToken = safAuthService.getAccessToken();
        if (TextUtils.isEmpty(accessToken)) {
            throw new RuntimeException("[SYSTEM-SMS] Failed to obtain Safaricom (SDP) access token");
        }

        SafBulkDataSet dataSet = new SafBulkDataSet();
        dataSet.setMessage(message);
        dataSet.setMsisdn(msisdn);
        dataSet.setChannel(AppUtils.CHANNEL_SMS);
        dataSet.setOa(SYSTEM_SENDER_ID);
        dataSet.setPackageId(safaricomProperties.getSafTransactionalPackageId());
        dataSet.setUniqueId(new UniqueCodeGenerator().generateSecureApiKey());
        dataSet.setUserName(safaricomProperties.getSafUserName());
        dataSet.setActionResponseURL(safaricomProperties.getSafResponseUrl());

        SendBulkSafReq req = new SendBulkSafReq();
        req.setDataSet(List.of(dataSet));
        req.setTimeStamp(String.valueOf(System.currentTimeMillis()));

        Call<BulkResponse> call = safaricomInterface.sendBulkSms(
                APPLICATION_JSON_VALUE, "XMLHttpRequest", "Bearer " + accessToken, req);
        Response<BulkResponse> res = call.execute();

        if (res.isSuccessful() && res.body() != null
                && AppUtils.BULK_SMS_SEND_SUCCESS_STATUS_CODE.equalsIgnoreCase(res.body().getStatusCode())) {
            log.info("[SYSTEM-SMS] Sent to {} via SDP (statusCode={})", msisdn, res.body().getStatusCode());
        } else {
            log.error("[SYSTEM-SMS] SDP send failed to {} — HTTP {} statusCode {}", msisdn, res.code(),
                    res.body() != null ? res.body().getStatusCode() : null);
        }
    }

    private void sendViaRest(String msisdn, String message) throws Exception {
        String accessToken = safaricomRestAuthService.getAccessToken();
        if (TextUtils.isEmpty(accessToken)) {
            throw new RuntimeException("[SYSTEM-SMS] Failed to obtain Safaricom (REST) access token");
        }

        DsdpBulkDataSet dataSet = DsdpBulkDataSet.builder()
                .userName(safaricomRestProperties.getSenderUserName())
                .channel(AppUtils.CHANNEL_SMS)
                .oa(SYSTEM_SENDER_ID)
                .msisdn(msisdn)
                .message(message)
                .uniqueId(new UniqueCodeGenerator().generateSecureApiKey())
                .actionResponseURL(safaricomRestProperties.getResponseUrl())
                .hashed("no")
                .packageId(safaricomRestProperties.getTransactionalPackageId())
                .build();

        SafaricomRestSendRequest req = SafaricomRestSendRequest.builder()
                .timeStamp(System.currentTimeMillis())
                .dataSet(List.of(dataSet))
                .build();

        Call<SafaricomRestSendResponse> call = safaricomRestInterface.sendBulkSms("Bearer " + accessToken, req);
        Response<SafaricomRestSendResponse> res = call.execute();

        if (res.isSuccessful() && res.body() != null
                && AppUtils.BULK_SMS_SEND_SUCCESS_STATUS_CODE.equalsIgnoreCase(res.body().getStatusCode())) {
            log.info("[SYSTEM-SMS] Sent to {} via REST (statusCode={})", msisdn, res.body().getStatusCode());
        } else {
            log.error("[SYSTEM-SMS] REST send failed to {} — HTTP {} statusCode {}", msisdn, res.code(),
                    res.body() != null ? res.body().getStatusCode() : null);
        }
    }
}
