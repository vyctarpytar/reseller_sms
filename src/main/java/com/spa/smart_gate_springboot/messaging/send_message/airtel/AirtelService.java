package com.spa.smart_gate_springboot.messaging.send_message.airtel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.spa.smart_gate_springboot.MQRes.MQConfig;
import com.spa.smart_gate_springboot.account_setup.account.AccountService;
import com.spa.smart_gate_springboot.messaging.send_message.MsgMessageQueueArc;
import com.spa.smart_gate_springboot.messaging.send_message.MsgMessageQueueArcRepository;
import com.spa.smart_gate_springboot.utils.GlobalUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.RejectedExecutionException;

@Component
@RequiredArgsConstructor
@Slf4j
public class AirtelService {
    private final String AIRTEL_END_POINT = "http://smartgate.co.ke/usrA/sendAirtelMessage.action";
    private final RestTemplate restTemplate;
    private final MsgMessageQueueArcRepository msgMessageQueueArcRepository;
    private final AccountService accountService;


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
        msgMessageQueueArc = msgMessageQueueArcRepository.save(msgMessageQueueArc);

        accountService.handleUpdateOfAccountBalance(msgMessageQueueArc.getMsgCostId(), msgMessageQueueArc.getMsgAccId(), msgMessageQueueArc.getMsgResellerId());


        try {
            SMSReport response = restTemplate.postForObject(AIRTEL_END_POINT, requestBody, SMSReport.class);


            msgMessageQueueArc.setMsgStatus(response.responses.get(0).responseDescription);
            msgMessageQueueArc.setMsgDeliveredDate(new Date());
            msgMessageQueueArc.setMsgClientDeliveryStatus("PENDING");
            msgMessageQueueArc.setMsgRetryCount(0);
            msgMessageQueueArc.setMsgCode(response.responses.get(0).messageid);
            msgMessageQueueArc.setMsgErrorDesc(response.toString());
            msgMessageQueueArcRepository.save(msgMessageQueueArc);

            log.info("Sent to AirTel : {}", response);
        } catch (Exception e) {
            log.error("Error sending Airtel message", e);
            throw e;
        }
    }

}
