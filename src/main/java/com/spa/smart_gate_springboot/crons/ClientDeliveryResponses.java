package com.spa.smart_gate_springboot.crons;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.spa.smart_gate_springboot.messaging.send_message.MsgMessageQueueArc;
import com.spa.smart_gate_springboot.messaging.send_message.MsgMessageQueueArcRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import org.apache.http.util.TextUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import retrofit2.Response;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Cron that pushes delivery reports back to client servers.
 * <p>
 * Two flows, both POSTing a {@link ClientDeliveryPayload} to the client's callback URL:
 * <ul>
 *   <li><b>Delivery callbacks</b> – messages with a delivery report from the carrier
 *       (msg_delivered_date set) that have not yet been pushed to the client.</li>
 *   <li><b>Stuck/never-sent callbacks</b> – messages that never received a delivery
 *       report and are stuck in a non-deliverable status (e.g. PENDING_CREDIT) past a
 *       grace period, so the client is told the message was not delivered.</li>
 * </ul>
 * If the client server is unreachable the attempt is retried at most once every
 * {@code retryIntervalMinutes} (default 30), up to {@code maxRetries} times, after
 * which the row is marked {@code CALLBACK_FAILED} and dropped.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ClientDeliveryResponses {

    private static final int PAGE_SIZE = 200;
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    /** How long to wait before retrying a failed callback. */
    @Value("${sms.callback.retry-interval-minutes:30}")
    private long retryIntervalMinutes;

    /** Max number of attempts before giving up (controlled via the retry counter column). */
    @Value("${sms.callback.max-retries:10}")
    private int maxRetries;

    /** Grace period before a never-delivered message is reported to the client as not-sent. */
    @Value("${sms.callback.stuck-grace-minutes:360}")
    private long stuckGraceMinutes;

    /** Statuses that mean a message was never sent and won't progress on its own. */
    @Value("${sms.callback.failed-statuses:PENDING_CREDIT,RS_CREDIT_ISSUE}")
    private List<String> stuckStatuses;

    private final MsgMessageQueueArcRepository arcRepository;
    private final ObjectMapper objectMapper;

    /** Retrofit client; the OkHttp interceptor logs the full request and response. */
    private final ClientCallbackInterface callbackClient;

    /** Delivered messages: push the carrier delivery report to the client. */
    @Scheduled(fixedRate = 15_000)
    public void sendPendingDeliveryCallbacks() {
        try {
            LocalDateTime retryBefore = LocalDateTime.now().minusMinutes(retryIntervalMinutes);
            Pageable pageable = PageRequest.of(0, PAGE_SIZE);
            Page<MsgMessageQueueArc> page = arcRepository.findPendingClientCallbacks(retryBefore, pageable);
            List<MsgMessageQueueArc> pending = page.getContent();
            if (pending.isEmpty()) {
                return;
            }
            log.info("Dispatching {} delivery callback(s)", pending.size());
            for (MsgMessageQueueArc m : pending) {
                dispatchCallback(m);
            }
        } catch (Exception e) {
            log.error("Error running delivery callback cron: {}", e.getMessage(), e);
        }
    }

    /** Never-sent / stuck messages: tell the client the message was not delivered. */
    @Scheduled(fixedRate = 5 * 60_000)
    public void sendStuckMessageCallbacks() {
        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime retryBefore = now.minusMinutes(retryIntervalMinutes);
            LocalDateTime stuckBefore = now.minusMinutes(stuckGraceMinutes);
            Pageable pageable = PageRequest.of(0, PAGE_SIZE);
            Page<MsgMessageQueueArc> page =
                    arcRepository.findStuckClientCallbacks(stuckStatuses, stuckBefore, retryBefore, pageable);
            List<MsgMessageQueueArc> pending = page.getContent();
            if (pending.isEmpty()) {
                return;
            }
            log.info("Dispatching {} stuck-message callback(s)", pending.size());
            for (MsgMessageQueueArc m : pending) {
                dispatchCallback(m);
            }
        } catch (Exception e) {
            log.error("Error running stuck-message callback cron: {}", e.getMessage(), e);
        }
    }

    private void dispatchCallback(MsgMessageQueueArc m) {
        if (TextUtils.isEmpty(m.getMsgCallbackUrl())) {
            return;
        }
        ClientDeliveryPayload payload = ClientDeliveryPayload.from(m);

        RequestBody body;
        try {
            body = RequestBody.create(objectMapper.writeValueAsString(payload), JSON);
        } catch (Exception e) {
            log.warn("Failed to serialise callback payload msgId={}: {}", m.getMsgId(), e.getMessage(), e);
            handleFailure(m, "payload serialise error: " + e);
            return;
        }

        try {
            Response<ResponseBody> response =
                    callbackClient.sendCallback(m.getMsgCallbackUrl(), body).execute();

            if (response.isSuccessful()) {
                if (response.body() != null) {
                    response.body().close();
                }
                arcRepository.markClientCallbackNotified(m.getMsgId(), LocalDateTime.now());
                log.info("Delivery callback delivered msgId={} url={} httpStatus={}",
                        m.getMsgId(), m.getMsgCallbackUrl(), response.code());
            } else {
                if (response.errorBody() != null) {
                    response.errorBody().close();
                }
                // Full request/response JSON is logged by CallbackLoggingInterceptor.
                handleFailure(m, response.code() + " " + response.message());
            }
        } catch (Exception e) {
            // Log the full stack trace so the exact failure point is visible (the request may
            // never reach the interceptor, e.g. a body-serialisation error inside Retrofit).
            log.warn("Delivery callback errored msgId={} url={}", m.getMsgId(), m.getMsgCallbackUrl(), e);
            handleFailure(m, e.toString());
        }
    }

    private void handleFailure(MsgMessageQueueArc m, String reason) {
        int attempts = m.getMsgRetryCount() + 1;
        LocalDateTime attemptTime = LocalDateTime.now();
        if (attempts >= maxRetries) {
            // Give up: mark the client delivery as failed so it is no longer picked up.
            // msg_status is left untouched so SMS resend/credit logic is unaffected.
            arcRepository.markClientCallbackFailed(m.getMsgId(), attempts, attemptTime);
            log.warn("Delivery callback gave up msgId={} url={} attempts={} reason={}",
                    m.getMsgId(), m.getMsgCallbackUrl(), attempts, reason);
        } else {
            // Keep status PENDING and bump the counter; the next run retries after the interval.
            arcRepository.updateClientCallbackRetry(m.getMsgId(), attempts, attemptTime);
            log.warn("Delivery callback failed (retry in {}min) msgId={} url={} attempt={} reason={}",
                    retryIntervalMinutes, m.getMsgId(), m.getMsgCallbackUrl(), attempts, reason);
        }
    }
}
