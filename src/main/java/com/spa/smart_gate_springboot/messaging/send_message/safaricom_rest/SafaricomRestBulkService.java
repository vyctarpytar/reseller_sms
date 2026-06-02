package com.spa.smart_gate_springboot.messaging.send_message.safaricom_rest;

import com.spa.smart_gate_springboot.account_setup.shortsetup.MsgShortcodeSetup;
import com.spa.smart_gate_springboot.account_setup.shortsetup.MsgShortcodeSetupService;
import com.spa.smart_gate_springboot.messaging.send_message.MsgMessageQueueArc;
import com.spa.smart_gate_springboot.messaging.send_message.MsgMessageQueueArcRepository;
import com.spa.smart_gate_springboot.messaging.send_message.safaricom_rest.dto.DsdpBulkDataSet;
import com.spa.smart_gate_springboot.messaging.send_message.safaricom_rest.dto.SafaricomRestSendRequest;
import com.spa.smart_gate_springboot.messaging.send_message.safaricom_rest.dto.SafaricomRestSendResponse;
import com.spa.smart_gate_springboot.utils.AppUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.util.TextUtils;
import org.springframework.stereotype.Service;
import retrofit2.Call;
import retrofit2.Response;

import java.io.IOException;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class SafaricomRestBulkService {

    private final SafaricomRestInterface safaricomRestInterface;
    private final SafaricomRestAuthService safaricomRestAuthService;
    private final MsgMessageQueueArcRepository msgMessageQueueArcRepository;
    private final MsgShortcodeSetupService msgShortcodeSetupService;
    private final SafaricomRestProperties safaricomRestProperties;

    public void sendSms(MsgMessageQueueArc msg) throws Exception {
        String accessToken = safaricomRestAuthService.getAccessToken();
        if (TextUtils.isEmpty(accessToken)) {
            throw new RuntimeException("[DSDP] Failed to obtain access token");
        }

        MsgShortcodeSetup shortcodeSetup = msgShortcodeSetupService.findByShCodeAndShAccId(
                msg.getMsgSenderIdName().trim(), msg.getMsgAccId()
        );

        if (TextUtils.isEmpty(shortcodeSetup.getShSenderType())) {
            throw new RuntimeException("Sender ID type not mapped for sender: " + msg.getMsgSenderIdName());
        }

        String packageId = shortcodeSetup.getShSenderType().equalsIgnoreCase("TRANSACTION")
                ? safaricomRestProperties.getTransactionalPackageId()
                : safaricomRestProperties.getPromotionalPackageId();

        DsdpBulkDataSet dataSet = DsdpBulkDataSet.builder()
                .userName(safaricomRestProperties.getSenderUserName())
                .channel(AppUtils.CHANNEL_SMS)
                .oa(shortcodeSetup.getShCode())
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

        log.info("[DSDP] Sending to={} sender={} accId={}", msg.getMsgSubMobileNo(), shortcodeSetup.getShCode(), msg.getMsgAccId());

        Response<SafaricomRestSendResponse> res = executeSend(request, accessToken);

        // One-shot 401 recovery: the token may have been revoked/expired between scheduled refreshes.
        if (res.code() == 401) {
            log.warn("[DSDP] Send got HTTP 401 — refreshing token and retrying once for msgCode={}", msg.getMsgCode());
            accessToken = safaricomRestAuthService.refreshOnUnauthorized(accessToken);
            res = executeSend(request, accessToken);
        }

        if (res.isSuccessful() && res.body() != null) {
            SafaricomRestSendResponse body = res.body();
            boolean success = AppUtils.BULK_SMS_SEND_SUCCESS_STATUS_CODE.equalsIgnoreCase(body.getStatusCode());
            log.info("[DSDP] Response — status={} statusCode={}", body.getStatus(), body.getStatusCode());
            updateMessageStatus(success, msg.getMsgCode(), res.code(), body.getStatusCode());
        } else {
            log.error("[DSDP] Send failed — HTTP {}", res.code());
            updateMessageStatus(false, msg.getMsgCode(), res.code(), "HTTP_" + res.code());
        }
    }

    private Response<SafaricomRestSendResponse> executeSend(SafaricomRestSendRequest request, String accessToken) throws IOException {
        Call<SafaricomRestSendResponse> call = safaricomRestInterface.sendBulkSms("Bearer " + accessToken, request);
        return call.execute();
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
