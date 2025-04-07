package com.spa.smart_gate_springboot.messaging.send_message.api;

import com.spa.smart_gate_springboot.MQRes.MQConfig;
import com.spa.smart_gate_springboot.MQRes.RMQPublisher;
import com.spa.smart_gate_springboot.account_setup.account.Account;
import com.spa.smart_gate_springboot.account_setup.account.AccountRepository;
import com.spa.smart_gate_springboot.dto.Layers;
import com.spa.smart_gate_springboot.messaging.send_message.MsgMessageQueueArc;
import com.spa.smart_gate_springboot.messaging.send_message.MsgMessageQueueArcRepository;
import com.spa.smart_gate_springboot.messaging.send_message.MsgQueue;
import com.spa.smart_gate_springboot.user.User;
import com.spa.smart_gate_springboot.utils.StandardJsonResponse;
import com.spa.smart_gate_springboot.utils.UniqueCodeGenerator;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import springfox.documentation.spring.web.plugins.Docket;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class ApiKeyService {

    private final ApiKeyRepository apiKeyRepository;
    private final AccountRepository accountRepo;
    private final MsgMessageQueueArcRepository arcRepository;
    private final RMQPublisher rmqPublisher;
    private final Docket api;

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
            apiKeyRepository.save(apiKey);
        }catch (Exception e) {
            log.error( "Error Creating Api Key : {}" ,e.getMessage());
        }
    }

    public StandardJsonResponse sendMessage(MsgApiDto msgApiDto, String apiKeyStr) {
        ApiKey apikey = apiKeyRepository.findByApiKey(apiKeyStr).orElseThrow(() -> new RuntimeException("Key Not Found"));

        MsgQueue msgQueue = MsgQueue.builder()
                .msgAccId(apikey.getApiAccId()).msgStatus("PENDING_PROCESSING")
                .msgExternalId(msgApiDto.getMsgExternalId())
                .msgSenderId(msgApiDto.getMsgSenderId()).msgMessage(msgApiDto.getMsgMessage())
                .msgCreatedDate(new Date()).msgCreatedTime(String.valueOf(LocalDateTime.now())).msgSubMobileNo(msgApiDto.getMsgMobileNo())
                .msgCreatedBy(null)// dont set thid
                .msgCreatedByEmail("API_USER")
                .build();


        Account acc = accountRepo.findById(msgQueue.getMsgAccId()).orElseThrow(() -> new RuntimeException("Account Does Not Exist :" + msgQueue.getMsgAccId()));
        if (acc.getAccMsgBal().compareTo(BigDecimal.TEN) < 1) {
            msgQueue.setMsgStatus("PENDING_CREDIT");
            msgQueue.setMsgClientDeliveryStatus("PENDING");

            MsgMessageQueueArc arcQueue = new MsgMessageQueueArc();
            BeanUtils.copyProperties(msgQueue, arcQueue);
            arcQueue.setMsgExternalId(msgQueue.getMsgExternalId());
            if (arcQueue.getMsgId() != null) {
                log.warn(" msg id should be null here ---{}", arcQueue.getMsgId());
                arcQueue.setMsgId(null);
            }
            arcQueue.setMsgClientDeliveryStatus("PENDING");
            arcRepository.save(arcQueue);
        } else {
            log.error(" synq sending sms --> ");
                try {
                    rmqPublisher.publishToOutQueue(msgQueue, MQConfig.SYNQ_QUEUE);
                } catch (Exception e) {
                    log.error("Error Queue-ing Synq Messages : {}", e.getMessage());

                }

        }

        StandardJsonResponse resp = new StandardJsonResponse();
//        resp.setData("result", msgQueue, resp);
        resp.setMessage("message", "Messages Sent Successfully", resp);
        return resp;

    }


    public StandardJsonResponse getAccountApiKeyInfo(User user) {
        StandardJsonResponse resp = new StandardJsonResponse();
        if (!user.getLayer().equals(Layers.ACCOUNT)) {
            resp.setStatus(400);
            resp.setSuccess(false);
            resp.setMessage("message", "Messages Sent Successfully", resp);
            return resp;
        }

        ApiKey apikey = apiKeyRepository.findByApiAccIdAndActiveIsTrue(user.getUsrAccId()).orElse(null);
        resp.setData("result", apikey, resp);
        return resp;
    }

//    @PostConstruct
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
            apiKeyRepository.saveAndFlush(apiKey);
        }

    }
}

