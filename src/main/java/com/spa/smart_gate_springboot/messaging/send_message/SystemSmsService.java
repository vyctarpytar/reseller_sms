package com.spa.smart_gate_springboot.messaging.send_message;

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

import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * Sends internal/system SMS (OTPs, password notifications, admin alerts) through the same Safaricom
 * integration that powers the bulk-SMS product — replacing the retired third-party OTP gateway.
 *
 * Unlike the billable {@code QueueMsgService} path, these messages are NOT tied to a customer account:
 * there is no message-balance debit, no reseller credit check, and no shortcode lookup. The sender ID
 * is fixed to {@value #SYSTEM_SENDER_ID} and the transactional package is always used. Delivery is
 * fire-and-forget on a background thread so callers never block (or fail) on the SMS gateway.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SystemSmsService {

    /** Fixed sender ID for all system-originated SMS. */
    public static final String SYSTEM_SENDER_ID = "DoNotReply";

    // v1 (legacy SDP)
    private final SafAuthService safAuthService;
    private final SafaricomInterface safaricomInterface;
    private final SafaricomProperties safaricomProperties;

    // v2 (Daraja REST)
    private final SafaricomRestAuthService safaricomRestAuthService;
    private final SafaricomRestInterface safaricomRestInterface;
    private final SafaricomRestProperties safaricomRestProperties;

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
