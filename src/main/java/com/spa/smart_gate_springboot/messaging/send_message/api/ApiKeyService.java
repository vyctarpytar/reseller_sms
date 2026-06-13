package com.spa.smart_gate_springboot.messaging.send_message.api;

import com.spa.smart_gate_springboot.MQRes.MQConfig;
import com.spa.smart_gate_springboot.MQRes.RMQPublisher;
import com.spa.smart_gate_springboot.account_setup.account.Account;
import com.spa.smart_gate_springboot.account_setup.account.AccountRepository;
import com.spa.smart_gate_springboot.account_setup.shortsetup.MsgShortcodeSetup;
import com.spa.smart_gate_springboot.account_setup.shortsetup.MsgShortcodeSetupRepository;
import com.spa.smart_gate_springboot.account_setup.shortsetup.ShStatus;
import com.spa.smart_gate_springboot.dto.Layers;
import com.spa.smart_gate_springboot.messaging.send_message.MsgMessageQueueArc;
import com.spa.smart_gate_springboot.messaging.send_message.MsgMessageQueueArcRepository;
import com.spa.smart_gate_springboot.messaging.send_message.MsgQueue;
import com.spa.smart_gate_springboot.messaging.send_message.airtel.AiretelService;
import com.spa.smart_gate_springboot.user.User;
import com.spa.smart_gate_springboot.utils.StandardJsonResponse;
import com.spa.smart_gate_springboot.utils.UniqueCodeGenerator;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class ApiKeyService {

    private final ApiKeyRepository apiKeyRepository;
    private final AccountRepository accountRepo;
    private final MsgMessageQueueArcRepository arcRepository;
    private final MsgShortcodeSetupRepository shortcodeSetupRepository;
    private final RMQPublisher rmqPublisher;
    @Lazy
    private final AiretelService airetelService;

    private static final String SINGLE_SMS_ENDPOINT =
            "https://backend.synqafrica.co.ke/api/v2/sandbox/single-sms";
    private static final String BULK_SMS_ENDPOINT =
            "https://backend.synqafrica.co.ke/api/v2/sandbox/bulk-sms";

    private static final String DEFAULT_SENDER_ID = "DoNotReply";
    private static final String DEFAULT_MOBILE_NO = "254716177880";
    private static final String SAMPLE_MESSAGE =
            "Your verification code is 123456. It expires in 5 minutes. Do not share it with anyone.";

    private static final String API_RESPONSE_SAMPLE =
            "{\n" + "\t\"success\": true,\n" + "\t\"messages\": {\n"
            + "\t\t\"message\": \"Messages Sent Successfully\"\n" + "\t},\n"
            + "\t\"data\": {\n" + "\t},\n" + "\t\"total\": 1,\n"
            + "\t\"targetUrl\": null,\n" + "\t\"token\": null,\n"
            + "\t\"status\": 200\n" + "}";

    /** Applies the sandbox endpoint, key tag and documented payload/response samples. */
    private void applyApiDocs(ApiKey apiKey) {
        Account account = apiKey.getApiAccId() == null
                ? null
                : accountRepo.findById(apiKey.getApiAccId()).orElse(null);
        String senderId = resolveSenderId(apiKey.getApiAccId());
        String mobileNo = resolveMobileNo(account);

        apiKey.setApiEndPoint(SINGLE_SMS_ENDPOINT);
        apiKey.setApiKeyTag("X-API-KEY");
        apiKey.setApiPayload(buildSinglePayload(apiKey.getApiKey(), senderId, mobileNo));
        apiKey.setApiPayloadMultiple(buildBulkPayload(apiKey.getApiKey(), senderId, mobileNo));
        apiKey.setApiResponse(API_RESPONSE_SAMPLE);
    }

    /** First sender ID allocated to the account (ACTIVE preferred), else {@value #DEFAULT_SENDER_ID}. */
    private String resolveSenderId(UUID accId) {
        if (accId == null) {
            return DEFAULT_SENDER_ID;
        }
        List<MsgShortcodeSetup> allocated = shortcodeSetupRepository.findByShAccId(accId);
        return allocated.stream()
                .filter(s -> s.getShStatus() == ShStatus.ACTIVE)
                .map(MsgShortcodeSetup::getShCode)
                .filter(code -> code != null && !code.isBlank())
                .findFirst()
                .or(() -> allocated.stream()
                        .map(MsgShortcodeSetup::getShCode)
                        .filter(code -> code != null && !code.isBlank())
                        .findFirst())
                .orElse(DEFAULT_SENDER_ID);
    }

    /** The account contact person's mobile (admin, then office), else {@value #DEFAULT_MOBILE_NO}. */
    private String resolveMobileNo(Account account) {
        if (account != null) {
            if (account.getAccAdminMobile() != null && !account.getAccAdminMobile().isBlank()) {
                return account.getAccAdminMobile();
            }
            if (account.getAccOfficeMobile() != null && !account.getAccOfficeMobile().isBlank()) {
                return account.getAccOfficeMobile();
            }
        }
        return DEFAULT_MOBILE_NO;
    }

    private String buildSinglePayload(String key, String senderId, String mobileNo) {
        return "curl --request POST \\\n"
                + "  --url " + SINGLE_SMS_ENDPOINT + " \\\n"
                + "  --header 'Content-Type: application/json' \\\n"
                + "  --header 'X-API-KEY: " + key + "' \\\n"
                + "  --data '{\n"
                + "  \"msgExternalId\": 1,\n"
                + "  \"msgMobileNo\": \"" + mobileNo + "\",\n"
                + "  \"msgMessage\": \"" + SAMPLE_MESSAGE + "\",\n"
                + "  \"msgSenderId\": \"" + senderId + "\",\n"
                + "  \"callbackUrl\": \"https://your-server.com/dlr-callback\"\n"
                + "}'"
                + buildCallbackSample(senderId, mobileNo);
    }

    private String buildBulkPayload(String key, String senderId, String mobileNo) {
        return "curl --request POST \\\n"
                + "  --url " + BULK_SMS_ENDPOINT + " \\\n"
                + "  --header 'Content-Type: application/json' \\\n"
                + "  --header 'X-API-KEY: " + key + "' \\\n"
                + "  --data '{\n"
                + "  \"msgExternalId\": 1,\n"
                + "  \"msgMobileNos\": [\"" + mobileNo + "\",\"" + mobileNo + "\"],\n"
                + "  \"msgMessage\": \"" + SAMPLE_MESSAGE + "\",\n"
                + "  \"msgSenderId\": \"" + senderId + "\",\n"
                + "  \"callbackUrl\": \"https://your-server.com/dlr-callback\"\n"
                + "}'"
                + buildCallbackSample(senderId, mobileNo);
    }

    /**
     * Sample delivery report (DLR) this gateway POSTs to the client's {@code callbackUrl}.
     * Appended to the API payload examples so clients can build a callback endpoint that
     * matches the {@code ClientDeliveryPayload} contract (statusCode / statusDescription
     * may be null when the carrier does not supply them).
     */
    private String buildCallbackSample(String senderId, String mobileNo) {
        return "\n\n# Delivery report POSTed to your callbackUrl:\n" + "{\n"
                + "  \"messageId\": \"<your msgExternalId>\",\n"
                + "  \"mobileNo\": \"" + mobileNo + "\",\n"
                + "  \"senderId\": \"" + senderId + "\",\n"
                + "  \"message\": \"" + SAMPLE_MESSAGE + "\",\n"
                + "  \"status\": \"DeliveredToTerminal\",\n"
                + "  \"statusCode\": null,\n"
                + "  \"statusDescription\": null,\n"
                + "  \"deliveredAt\": \"2026-06-03T01:29:49.532988\",\n"
                + "  \"requestId\": \"1780439388289786166\"\n" + "}";
    }


    public boolean validateApiKey(String apiKey) {
        boolean isValid = apiKeyRepository.existsValidApiKey(apiKey);
        log.info("Api key validation result Service: {}", isValid);
        return isValid;
    }


    public void createApiKey(Account acc) {
        try {

            Optional<ApiKey> apiKeyop = apiKeyRepository.findByApiAccId(acc.getAccId());

            if (apiKeyop.isPresent()) {
                log.info("Api key already exists");
                return;
            }

            UniqueCodeGenerator ug = new UniqueCodeGenerator();
            ApiKey apiKey = ApiKey.builder().id(UUID.randomUUID()).apiKey(ug.generateSecureApiKey()).clientName(acc.getAccName()).apiResellerId(acc.getAccResellerId()).apiAccId(acc.getAccId()).createdDate(new Date()).build(); // expirationDate and active will be set by @PrePersist

            applyApiDocs(apiKey);
            apiKeyRepository.save(apiKey);
        } catch (Exception e) {
            log.error("Error Creating Api Key : {}", e.getMessage());
        }
    }

    public Map<String, Object> sendMessage(MsgApiDto msgApiDto, String apiKeyStr) {
        ApiKey apikey = apiKeyRepository.findByApiKey(apiKeyStr).orElseThrow(() -> new RuntimeException("Key Not Found"));

        MsgQueue msgQueue = MsgQueue.builder().msgAccId(apikey.getApiAccId()).msgStatus("PENDING_PROCESSING").msgExternalId(msgApiDto.getMsgExternalId()).msgSenderId(msgApiDto.getMsgSenderId()).msgMessage(msgApiDto.getMsgMessage()).msgCreatedDate(new Date()).msgCreatedTime(String.valueOf(LocalDateTime.now())).msgSubMobileNo(msgApiDto.getMsgMobileNo()).msgCallbackUrl(msgApiDto.getCallbackUrl()).msgCreatedBy(null)// dont set thid
                .msgCreatedByEmail("API_USER").build();

        // API idempotency: when the caller supplies a msgExternalId, derive a stable dedup key from it
        // (scoped by account so external ids can't collide across tenants) so a client retry with the
        // same external id is deduped to at-most-once debit/send. Without one the publisher assigns a
        // random key — still dedupes a RabbitMQ redelivery, just not a client-side retry.
        // The recipient number is part of the key: a bulk request reuses ONE msgExternalId across every
        // number, so a per-(externalId) key would make numbers 2..N collide on the unique index — the
        // funded path would drop them as duplicates and the out-of-credit path would throw. Keying on
        // (account, externalId, number) keeps each number distinct while still deduping a true retry.
        if (msgApiDto.getMsgExternalId() != null && !msgApiDto.getMsgExternalId().isBlank()) {
            msgQueue.setMsgDedupKey("api:" + apikey.getApiAccId() + ":" + msgApiDto.getMsgExternalId()
                    + ":" + msgApiDto.getMsgMobileNo());
        }

        MsgMessageQueueArc arcQueue = new MsgMessageQueueArc();
        BeanUtils.copyProperties(msgQueue, arcQueue);
        arcQueue.setMsgExternalId(msgQueue.getMsgExternalId());
        // msgSenderIdName (not msgSenderId) is the field the delivery-callback payload reports as
        // senderId; copyProperties only carries msgSenderId, so set it explicitly or the PENDING_CREDIT
        // callback would send senderId=null. (MQReceiverSynq.buildArc does the same for the queue path.)
        arcQueue.setMsgSenderIdName(msgQueue.getMsgSenderId());

        Account acc = accountRepo.findById(msgQueue.getMsgAccId()).orElseThrow(() -> new RuntimeException("Account Does Not Exist :" + msgQueue.getMsgAccId()));
        arcQueue.setMsgResellerId(acc.getAccResellerId());
        if (acc.getAccMsgBal().compareTo(BigDecimal.TEN) < 1) {
            msgQueue.setMsgStatus("PENDING_CREDIT");
            msgQueue.setMsgClientDeliveryStatus("PENDING");


            if (arcQueue.getMsgId() != null) {
                log.warn(" msg id should be null here ---{}", arcQueue.getMsgId());
                arcQueue.setMsgId(null);
            }
            // The arc was copied from msgQueue (above) while its status was still PENDING_PROCESSING,
            // so set the out-of-credit status ON THE ARC too — not just on msgQueue. The client-callback
            // cron (ClientDeliveryResponses.findStuckClientCallbacks) only selects rows whose
            // msg_status IN (PENDING_CREDIT, RS_CREDIT_ISSUE); without this the persisted row keeps
            // PENDING_PROCESSING and the API caller's msgCallbackUrl is never notified of the credit issue.
            arcQueue.setMsgStatus("PENDING_CREDIT");
            arcQueue.setMsgClientDeliveryStatus("PENDING");
            // Record the cost on the persisted PENDING_CREDIT arc (pages × account SMS price, same
            // formula as MQReceiverSynq). The resend-on-top-up flow debits this stored cost in place
            // (SmsDispatchService.debitAndResend); without it the arc would carry a null cost and the
            // later debit would fail. (The funded path below lets the receiver compute the cost instead.)
            int pages = (int) Math.ceil((double) msgQueue.getMsgMessage().length() / 160);
            BigDecimal price = acc.getAccSmsPrice() == null ? new BigDecimal("1.50") : acc.getAccSmsPrice();
            arcQueue.setMsgPage(pages);
            arcQueue.setMsgCostId(price.multiply(BigDecimal.valueOf(pages)));
            try {
                arcRepository.save(arcQueue);
            } catch (DataIntegrityViolationException duplicate) {
                // Idempotent retry: a row with this dedup key already exists (the client re-sent the same
                // externalId+number while still out of credit). The PENDING_CREDIT arc is already recorded
                // and will be funded+sent by the resend-on-top-up flow — treat the retry as a no-op, not a
                // 500. Mirrors the async dedup guard in MQReceiverSynq.receiver.
                log.info("[API] duplicate out-of-credit send (dedup {}) — already recorded PENDING_CREDIT, skipping",
                        arcQueue.getMsgDedupKey());
            }
        } else {


            boolean isAirtel = airetelService.checkIsAirtel(arcQueue.getMsgSubMobileNo());
            log.error(" synq sending sms --> {}   -> isAirtel {} ",arcQueue.getMsgSubMobileNo(),isAirtel);
            if (isAirtel) {
                log.info("Sending to Airtel");
                airetelService.sendMessageViaAirTel(arcQueue);
            } else {

                try {
                    rmqPublisher.publishToOutQueue(msgQueue, MQConfig.SYNQ_QUEUE);
                } catch (Exception e) {
                    log.error("Error Queue-ing Synq Messages : {}", e.getMessage());

                }
            }

        }
// Map <String,Object> resp = new HashMap<>();

        Map<String, Object> respData = new HashMap<>();
        respData.put("messageId", msgQueue.getMsgExternalId());
        respData.put("message", msgQueue.getMsgMessage());
        respData.put("senderId", msgQueue.getMsgSenderId());
        respData.put("mobileNo", msgQueue.getMsgSubMobileNo());
        respData.put("msgStatus", msgQueue.getMsgStatus());
        respData.put("errorDesc", null);

        // Echo the callback URL back to the client when one was supplied (optional).
        if (msgApiDto.getCallbackUrl() != null && !msgApiDto.getCallbackUrl().isBlank()) {
            respData.put("callbackUrl", msgApiDto.getCallbackUrl());
        }

        respData.put("status", 200);
        respData.put("success", true);

        return respData;

    }




    public StandardJsonResponse getAccountApiKeyInfo(User user) {
        StandardJsonResponse resp = new StandardJsonResponse();
        if (!user.getLayer().equals(Layers.ACCOUNT)) {
            resp.setStatus(400);
            resp.setSuccess(false);
            resp.setMessage("message", "User is not an account", resp);
            return resp;
        }

        ApiKey apikey = apiKeyRepository.findByApiAccIdAndActiveIsTrue(user.getUsrAccId()).orElse(null);
        resp.setData("result", apikey, resp);
        return resp;
    }

    public StandardJsonResponse getAccountApiKeyInfo(Account account) {
        StandardJsonResponse resp = new StandardJsonResponse();
        ApiKey apikey = apiKeyRepository.findByApiAccIdAndActiveIsTrue(account.getAccId()).orElse(null);
        resp.setData("result", apikey, resp);
        return resp;
    }

    @PostConstruct
    public void aupdateObject() {
        List<ApiKey> apiKeyList = apiKeyRepository.findAll();
        for (ApiKey apiKey : apiKeyList) {
            apiKey.setActive(true);
            applyApiDocs(apiKey);
            apiKeyRepository.save(apiKey);
        }

    }




}

