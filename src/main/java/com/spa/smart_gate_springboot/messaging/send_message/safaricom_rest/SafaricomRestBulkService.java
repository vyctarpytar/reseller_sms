package com.spa.smart_gate_springboot.messaging.send_message.safaricom_rest;

import com.spa.smart_gate_springboot.messaging.send_message.MsgMessageQueueArc;
import com.spa.smart_gate_springboot.messaging.send_message.MsgMessageQueueArcRepository;
import com.spa.smart_gate_springboot.messaging.send_message.SendMetadataCache;
import com.spa.smart_gate_springboot.messaging.send_message.safaricom_rest.dto.DsdpBulkDataSet;
import com.spa.smart_gate_springboot.messaging.send_message.safaricom_rest.dto.SafaricomRestSendRequest;
import com.spa.smart_gate_springboot.messaging.send_message.safaricom_rest.dto.SafaricomRestSendResponse;
import com.spa.smart_gate_springboot.utils.AppUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.ResponseBody;
import org.apache.http.util.TextUtils;
import org.springframework.stereotype.Service;
import retrofit2.Call;
import retrofit2.Converter;
import retrofit2.Response;
import retrofit2.Retrofit;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class SafaricomRestBulkService {

    private final SafaricomRestInterface safaricomRestInterface;
    private final SafaricomRestAuthService safaricomRestAuthService;
    private final MsgMessageQueueArcRepository msgMessageQueueArcRepository;
    /** Sender-ID setup was re-queried per message; every SMS in a campaign resolves the same row. */
    private final SendMetadataCache metadataCache;
    private final SafaricomRestProperties safaricomRestProperties;
    /** Field name matters: it selects the v2 Retrofit bean over the v1 {@code safComRetrofit} one. */
    private final Retrofit safComRestRetrofit;

    public void sendSms(MsgMessageQueueArc msg) throws Exception {
        String accessToken = safaricomRestAuthService.getAccessToken();
        if (TextUtils.isEmpty(accessToken)) {
            throw new RuntimeException("[DSDP] Failed to obtain access token");
        }

        SendMetadataCache.SenderMeta shortcodeSetup = metadataCache.sender(
                msg.getMsgSenderIdName().trim(), msg.getMsgAccId()
        );

        if (TextUtils.isEmpty(shortcodeSetup.senderType())) {
            throw new RuntimeException("Sender ID type not mapped for sender: " + msg.getMsgSenderIdName());
        }

        String packageId = shortcodeSetup.senderType().equalsIgnoreCase("TRANSACTION")
                ? safaricomRestProperties.getTransactionalPackageId()
                : safaricomRestProperties.getPromotionalPackageId();

        DsdpBulkDataSet dataSet = DsdpBulkDataSet.builder()
                .userName(safaricomRestProperties.getSenderUserName())
                .channel(AppUtils.CHANNEL_SMS)
                .oa(shortcodeSetup.code())
                .msisdn(msg.getMsgSubMobileNo())
                .message(msg.getMsgMessage())
                .uniqueId(msg.getMsgCode())
                .actionResponseURL(safaricomRestProperties.getResponseUrl())
                .hashed("no")
                .packageId(packageId)
                .build();

        SafaricomRestSendRequest request = SafaricomRestSendRequest.builder()
                .timeStamp(System.currentTimeMillis())
                .dataSet(List.of(dataSet))
                .build();

        log.debug("[DSDP] Sending to={} sender={} accId={}", msg.getMsgSubMobileNo(), shortcodeSetup.code(), msg.getMsgAccId());

        // Time the carrier round trip. The send runs synchronously on a RabbitMQ consumer thread, so
        // this span IS the per-message latency that caps throughput (throughput = consumers / latency).
        // It is the one number that says whether more consumers or request batching is the right lever,
        // so it is logged per send at INFO — one short line, unlike the full body dump the OkHttp
        // interceptor writes at DEBUG.
        long startNanos = System.nanoTime();
        Response<SafaricomRestSendResponse> res = executeSend(request, accessToken);

        // One-shot 401 recovery: the token may have been revoked/expired between scheduled refreshes.
        if (res.code() == 401) {
            log.warn("[DSDP] Send got HTTP 401 — refreshing token and retrying once for msgCode={}", msg.getMsgCode());
            accessToken = safaricomRestAuthService.refreshOnUnauthorized(accessToken);
            res = executeSend(request, accessToken);
        }
        long carrierMillis = (System.nanoTime() - startNanos) / 1_000_000L;

        // These codes can come back on either side of isSuccessful(), so parse the error body too rather
        // than collapsing every non-2xx to "HTTP_<code>" — that hid the carrier's own status code.
        SafaricomRestSendResponse body = res.isSuccessful() ? res.body() : parseError(res.errorBody());
        String statusCode = (body != null && !TextUtils.isEmpty(body.getStatusCode()))
                ? body.getStatusCode()
                : "HTTP_" + res.code();

        if (AppUtils.requiresTokenEviction(statusCode)) {
            // SC0029 / SC0012 — the token being replayed is spent or dead on Safaricom's side. Both come
            // back 200-level, so the 401 recovery above never fired; drop the cache here instead. The
            // message is left ERROR, so the retry cron re-sends it (~5s) against a freshly minted token.
            log.warn("[DSDP] statusCode={} ({}) for msgCode={} — evicting cached token so the retry logs in fresh",
                    statusCode, body != null ? body.getStatus() : "?", msg.getMsgCode());
            safaricomRestAuthService.evictTokens();
        }

        boolean success = res.isSuccessful()
                && AppUtils.BULK_SMS_SEND_SUCCESS_STATUS_CODE.equalsIgnoreCase(statusCode);
        if (success) {
            log.info("[DSDP] sent msgCode={} carrierMs={} status={} statusCode={}",
                    msg.getMsgCode(), carrierMillis, body.getStatus(), statusCode);
        } else {
            log.error("[DSDP] Send failed — HTTP {} statusCode={} carrierMs={}",
                    res.code(), statusCode, carrierMillis);
        }
        updateMessageStatus(success, msg.getMsgCode(), res.code(), statusCode);
    }

    private Response<SafaricomRestSendResponse> executeSend(SafaricomRestSendRequest request, String accessToken) throws IOException {
        Call<SafaricomRestSendResponse> call = safaricomRestInterface.sendBulkSms("Bearer " + accessToken, request);
        return call.execute();
    }

    /** Decode a non-2xx body into the same response shape; null if it is absent or unparseable. */
    private SafaricomRestSendResponse parseError(ResponseBody errorBody) {
        if (errorBody == null) return null;
        try {
            Converter<ResponseBody, SafaricomRestSendResponse> converter = safComRestRetrofit
                    .responseBodyConverter(SafaricomRestSendResponse.class, new Annotation[0]);
            return converter.convert(errorBody);
        } catch (Exception e) {
            log.warn("[DSDP] Could not parse error body: {}", e.getMessage());
            return null;
        }
    }

    private void updateMessageStatus(boolean success, String msgCode, int httpCode, String safResponse) {
        msgMessageQueueArcRepository.updateInitialReceiveNote(
                success ? "SENT" : "ERROR",
                httpCode,
                List.of(msgCode),
                safResponse
        );
    }
}
