package com.spa.smart_gate_springboot.messaging.send_message.airtel;

import com.spa.smart_gate_springboot.MQRes.MQConfig;
import com.spa.smart_gate_springboot.MQRes.RMQPublisher;
import com.spa.smart_gate_springboot.account_setup.account.dtos.AccBalanceUpdate;
import com.spa.smart_gate_springboot.messaging.send_message.MsgMessageQueueArc;
import com.spa.smart_gate_springboot.messaging.send_message.MsgMessageQueueArcRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiretelService {

    private final MsgMessageQueueArcRepository arcRepository;
    private final RMQPublisher rmqPublisher;
    private final AirtelNumberRepository airtelNumberRepository;
    private final RestTemplate restTemplate;
    private final String AIRTEL_END_POINT = "https://bulksms.switchportltd.com/api/services/sendsms";

    private static final Set<String> AIRTEL_PREFIXES = Set.of(
            "25473", "25475", "25478", "25470", "25474" // adjust as needed
    );

    public boolean checkIsAirtel(String msisdn) {
        if (msisdn == null || msisdn.isBlank()) return false;

        String normalized = normalizeMsisdn(msisdn);

        // Prefix check (fast)
        for (String prefix : AIRTEL_PREFIXES) {
            if (normalized.startsWith(prefix)) {
                return true;
            }
        }

        // DB check (fallback)
        return airtelNumberRepository.existsByAnNumber(normalized);
    }

    private String normalizeMsisdn(String msisdn) {
        String s = msisdn.trim().replace("+", "");
        if (s.startsWith("0")) {
            s = "254" + s.substring(1);
        }
        return s;
    }


    public MsgMessageQueueArc callback(@Valid CallBackResp callBackResp) {
        MsgMessageQueueArc arc = arcRepository.findByMsgCode(callBackResp.getMessageId()).orElseThrow(() -> new RuntimeException("Message Not Found with code:  " + callBackResp.getMessageId()));
        arc.setMsgStatus(callBackResp.deliveryDescription);
        arc.setMsgClientDeliveryStatus("PENDING");
        arc.setMsgRetryCount(0);
        arcRepository.save(arc);
        return arc;
    }


    public void sendMessageViaAirTel(MsgMessageQueueArc msgMessageQueueArc) {

        int no_of_msg = getNoOfMessage(msgMessageQueueArc);

//        BigDecimal cost_per_sms = getCostPerSMS(msgMessageQueueArc.getMsgAccId());
        BigDecimal cost_per_sms = new BigDecimal("0.20");
        BigDecimal totalCost = cost_per_sms.multiply(new BigDecimal(no_of_msg));
        msgMessageQueueArc.setMsgPage(no_of_msg);
        msgMessageQueueArc.setMsgCostId(totalCost);
//        String senderId = "SYNQSMS"; //""letstalk";
        String senderId = "letstalk"; //""letstalk";


        msgMessageQueueArc.setMsgSenderIdName(senderId);

        // Prepare request body
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("message", msgMessageQueueArc.getMsgMessage());
        requestBody.put("apikey", "aad395b77c99dc80e48eee05d2cbbee6");
        requestBody.put("partnerID", "15086");
        requestBody.put("shortcode", senderId);
        requestBody.put("mobile", msgMessageQueueArc.getMsgSubMobileNo());

        log.info("Sending to Airtel : {}", requestBody);


        handleUpdateOfAccountBalance(msgMessageQueueArc.getMsgCostId(), msgMessageQueueArc.getMsgAccId(), msgMessageQueueArc.getMsgResellerId());


        try {
            SMSReport responsee = restTemplate.postForObject(AIRTEL_END_POINT, requestBody, SMSReport.class);
            msgMessageQueueArc.setMsgCreatedDate(new Date());
            msgMessageQueueArc.setMsgCreatedTime(new Date());
            msgMessageQueueArc.setMsgStatus(responsee.responses.get(0).responseDescription);
            msgMessageQueueArc.setMsgDeliveredDate(new Date());
            msgMessageQueueArc.setMsgClientDeliveryStatus("PENDING");
            msgMessageQueueArc.setMsgRetryCount(0);
            msgMessageQueueArc.setMsgCode(responsee.responses.get(0).messageid);
            msgMessageQueueArc.setMsgErrorDesc(responsee.toString());
            msgMessageQueueArc.setMsgSenderLevel("switchport".toUpperCase());
            arcRepository.save(msgMessageQueueArc);

            if (!airtelNumberRepository.existsByAnNumber(msgMessageQueueArc.getMsgSubMobileNo())) {
                airtelNumberRepository.save(AirtelNumber.builder().anNumber(msgMessageQueueArc.getMsgSubMobileNo()).build());
            }

            log.info("Sent to AirTel : {}", responsee);
        } catch (Exception e) {
            log.error("Error sending Airtel message . {}", e.getMessage(), e);
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

    private int getNoOfMessage(MsgMessageQueueArc msgQueue) {


        int MSG_LENGTH = msgQueue.getMsgMessage().length();

        int no_of_characters_per_message = 160;
        return (int) Math.ceil((double) MSG_LENGTH / no_of_characters_per_message);


    }


}
