package com.spa.smart_gate_springboot.messaging.send_message.api;

import com.spa.smart_gate_springboot.MQRes.MQConfig;
import com.spa.smart_gate_springboot.MQRes.RMQPublisher;
import com.spa.smart_gate_springboot.account_setup.account.Account;
import com.spa.smart_gate_springboot.account_setup.account.AccountRepository;
import com.spa.smart_gate_springboot.account_setup.account.dtos.AccBalanceUpdate;
import com.spa.smart_gate_springboot.dto.Layers;
import com.spa.smart_gate_springboot.messaging.send_message.MsgMessageQueueArc;
import com.spa.smart_gate_springboot.messaging.send_message.MsgMessageQueueArcRepository;
import com.spa.smart_gate_springboot.messaging.send_message.MsgQueue;
import com.spa.smart_gate_springboot.messaging.send_message.airtel.SMSReport;
import com.spa.smart_gate_springboot.user.User;
import com.spa.smart_gate_springboot.utils.StandardJsonResponse;
import com.spa.smart_gate_springboot.utils.UniqueCodeGenerator;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

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
    private final RMQPublisher rmqPublisher;

    private final RestTemplate restTemplate;
    private final String AIRTEL_END_POINT = "http://smartgate.co.ke/usrA/sendAirtelMessage.action";

    public boolean validateApiKey(String apiKey) {
        boolean isValid = apiKeyRepository.existsValidApiKey(apiKey);
        log.info("Api key validation result Service: {}", isValid);
        return isValid;
    }


    public void createApiKey(Account acc) {
        try {

            Optional<ApiKey> apiKeyop =  apiKeyRepository.findByApiAccId(acc.getAccId());

            if (apiKeyop.isPresent()) {
                log.info("Api key already exists");
                return;
            }

            UniqueCodeGenerator ug = new UniqueCodeGenerator();
            ApiKey apiKey = ApiKey.builder()
                    .id(UUID.randomUUID())
                    .apiKey(ug.generateSecureApiKey())
                    .clientName(acc.getAccName())
                    .apiResellerId(acc.getAccResellerId())
                    .apiAccId(acc.getAccId())
                    .createdDate(new Date())
                    .build(); // expirationDate and active will be set by @PrePersist


            apiKey.setApiEndPoint("https://backend.synqafrica.co.ke:8443/api/v2/sandbox/single-sms");
            apiKey.setApiKeyTag("X-API-KEY");
            apiKey.setApiPayload(
                    "curl --request POST \\\n" +
                            "  --url " + apiKey.getApiEndPoint() + " \\\n" +
                            "  --header 'Content-Type: application/json' \\\n" +
                            "  --header 'X-API-KEY: " + apiKey.getApiKey() + "' \\\n" +
                            "  --data '{\n" +
                            "  \"msgExternalId\": 1,\n" +
                            "  \"msgMobileNo\": \"254716177880\",\n" +
                            "  \"msgMessage\": \"Test Api Message Dukapay\",\n" +
                            "  \"msgSenderId\": \"DoNotReply\"\n" +
                            "}'"
            );

            apiKey.setApiPayloadMultiple(
                    "curl --request POST \\\n" +
                            "  --url https://backend.synqafrica.co.ke:8443/api/v2/sandbox/bulk-sms \\\n" +
                            "  --header 'Content-Type: application/json' \\\n" +
                            "  --header 'X-API-KEY: " + apiKey.getApiKey() + "' \\\n" +
                            "  --data '{\n" +
                            "  \"msgExternalId\": 1,\n" +
                            "  \"msgMobileNos\": [\"254716177880\",\"254716177880\"],\n" +
                            "  \"msgMessage\": \"Test Api Message Dukapay\",\n" +
                            "  \"msgSenderId\": \"DoNotReply\"\n" +
                            "}'"
            );

            apiKey.setApiResponse("{\n" +
                    "\t\"success\": true,\n" +
                    "\t\"messages\": {\n" +
                    "\t\t\"message\": \"Messages Sent Successfully\"\n" +
                    "\t},\n" +
                    "\t\"data\": {\n" +
                    "\t},\n" +
                    "\t\"total\": 0,\n" +
                    "\t\"targetUrl\": null,\n" +
                    "\t\"token\": null,\n" +
                    "\t\"status\": 200\n" +
                    "}");
            apiKeyRepository.save(apiKey);
        }catch (Exception e) {
            log.error( "Error Creating Api Key : {}" ,e.getMessage());
        }
    }

    public Map<String,Object> sendMessage(MsgApiDto msgApiDto, String apiKeyStr) {
        ApiKey apikey = apiKeyRepository.findByApiKey(apiKeyStr).orElseThrow(() -> new RuntimeException("Key Not Found"));

        MsgQueue msgQueue = MsgQueue.builder()
                .msgAccId(apikey.getApiAccId()).msgStatus("PENDING_PROCESSING")
                .msgExternalId(msgApiDto.getMsgExternalId())
                .msgSenderId(msgApiDto.getMsgSenderId()).msgMessage(msgApiDto.getMsgMessage())
                .msgCreatedDate(new Date()).msgCreatedTime(String.valueOf(LocalDateTime.now())).msgSubMobileNo(msgApiDto.getMsgMobileNo())
                .msgCreatedBy(null)// dont set thid
                .msgCreatedByEmail("API_USER")
                .build();

        MsgMessageQueueArc arcQueue = new MsgMessageQueueArc();
        BeanUtils.copyProperties(msgQueue, arcQueue);
        arcQueue.setMsgExternalId(msgQueue.getMsgExternalId());

        Account acc = accountRepo.findById(msgQueue.getMsgAccId()).orElseThrow(() -> new RuntimeException("Account Does Not Exist :" + msgQueue.getMsgAccId()));
        if (acc.getAccMsgBal().compareTo(BigDecimal.TEN) < 1) {
            msgQueue.setMsgStatus("PENDING_CREDIT");
            msgQueue.setMsgClientDeliveryStatus("PENDING");


            if (arcQueue.getMsgId() != null) {
                log.warn(" msg id should be null here ---{}", arcQueue.getMsgId());
                arcQueue.setMsgId(null);
            }
            arcQueue.setMsgClientDeliveryStatus("PENDING");
            arcRepository.save(arcQueue);
        } else {
            log.error(" synq sending sms --> ");

            boolean isAirtel = true;
            if(isAirtel){
                log.info("Sending to Airtel");
                sendMessageViaAirTel(arcQueue);
            }else {

                try {
                    rmqPublisher.publishToOutQueue(msgQueue, MQConfig.SYNQ_QUEUE);
                } catch (Exception e) {
                    log.error("Error Queue-ing Synq Messages : {}", e.getMessage());

                }
            }

        }
// Map <String,Object> resp = new HashMap<>();

        Map<String,Object> respData = new HashMap<>();
        respData.put("messageId", msgQueue.getMsgExternalId());
        respData.put("message", msgQueue.getMsgMessage());
        respData.put("senderId", msgQueue.getMsgSenderId());
        respData.put("mobileNo", msgQueue.getMsgSubMobileNo());
        respData.put("msgStatus", msgQueue.getMsgStatus());
        respData.put("errorDesc", null);

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
            apiKey.setApiEndPoint("https://backend.synqafrica.co.ke:8443/api/v2/sandbox/single-sms");
            apiKey.setApiKeyTag("X-API-KEY");
            apiKey.setApiPayload(
                    "curl --request POST \\\n" +
                            "  --url " + apiKey.getApiEndPoint() + " \\\n" +
                            "  --header 'Content-Type: application/json' \\\n" +
                            "  --header 'X-API-KEY: " + apiKey.getApiKey() + "' \\\n" +
                            "  --data '{\n" +
                            "  \"msgExternalId\": 1,\n" +
                            "  \"msgMobileNo\": \"254716177880\",\n" +
                            "  \"msgMessage\": \"Test Api Message Dukapay\",\n" +
                            "  \"msgSenderId\": \"DoNotReply\"\n" +
                            "}'"
            );

            apiKey.setApiPayloadMultiple(
                    "curl --request POST \\\n" +
                            "  --url https://backend.synqafrica.co.ke:8443/api/v2/sandbox/bulk-sms \\\n" +
                            "  --header 'Content-Type: application/json' \\\n" +
                            "  --header 'X-API-KEY: " + apiKey.getApiKey() + "' \\\n" +
                            "  --data '{\n" +
                            "  \"msgExternalId\": 1,\n" +
                            "  \"msgMobileNos\": [\"254716177880\",\"254716177880\"],\n" +
                            "  \"msgMessage\": \"Test Api Message Dukapay\",\n" +
                            "  \"msgSenderId\": \"DoNotReply\"\n" +
                            "}'"
            );

            apiKey.setApiResponse("{\n" +
                    "\t\"success\": true,\n" +
                    "\t\"messages\": {\n" +
                    "\t\t\"message\": \"Messages Sent Successfully\"\n" +
                    "\t},\n" +
                    "\t\"data\": {\n" +
                    "\t},\n" +
                    "\t\"total\": 0,\n" +
                    "\t\"targetUrl\": null,\n" +
                    "\t\"token\": null,\n" +
                    "\t\"status\": 200\n" +
                    "}");
            apiKeyRepository.save(apiKey);
        }

    }



    public void sendMessageViaAirTel(MsgMessageQueueArc msgMessageQueueArc) {

        // Prepare request body
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("message", msgMessageQueueArc.getMsgMessage());
        requestBody.put("apikey", "aad395b77c99dc80e48eee05d2cbbee6");
        requestBody.put("partnerID", "15086");
        requestBody.put("shortcode", "letstalk");
        requestBody.put("mobile", msgMessageQueueArc.getMsgSubMobileNo());


        msgMessageQueueArc.setMsgCreatedDate(new Date());
        msgMessageQueueArc.setMsgCreatedTime(new Date());
        msgMessageQueueArc = arcRepository.save(msgMessageQueueArc);

        handleUpdateOfAccountBalance(msgMessageQueueArc.getMsgCostId(), msgMessageQueueArc.getMsgAccId(), msgMessageQueueArc.getMsgResellerId());


        try {
            SMSReport response = restTemplate.postForObject(AIRTEL_END_POINT, requestBody, SMSReport.class);


            msgMessageQueueArc.setMsgStatus(response.responses.get(0).responseDescription);
            msgMessageQueueArc.setMsgDeliveredDate(new Date());
            msgMessageQueueArc.setMsgClientDeliveryStatus("PENDING");
            msgMessageQueueArc.setMsgRetryCount(0);
            msgMessageQueueArc.setMsgCode(response.responses.get(0).messageid);
            msgMessageQueueArc.setMsgErrorDesc(response.toString());
            arcRepository.save(msgMessageQueueArc);

            log.info("Sent to AirTel : {}", response);
        } catch (Exception e) {
            log.error("Error sending Airtel message", e);
            throw e;
        }
    }

    public void handleUpdateOfAccountBalance(BigDecimal msgCostId, UUID accId, UUID accResellerId) {
        AccBalanceUpdate accBalanceUpdate = AccBalanceUpdate.builder().accId(accId).accResellerId(accResellerId).msgCost(msgCostId).build();

        try {
            rmqPublisher.publishToOutQueue(accBalanceUpdate, MQConfig.UPDATE_ACCOUNT_BALANCE);
        } catch (Exception e) {
            log.error("Error queueing update_balance");
        }

    }

}

