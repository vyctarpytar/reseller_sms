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
    private final GlobalUtils gu;
    private final MsgMessageQueueArcRepository msgMessageQueueArcRepository;
    private final AccountService accountService;
    private final ObjectMapper objectMapper;

    private final ThreadPoolTaskExecutor taskExecutor;

    public void sendMessageViaAirTel(MsgMessageQueueArc msgMessageQueueArc) {

        // Prepare request body
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("msgSubMobileNo", msgMessageQueueArc.getMsgSubMobileNo());
        requestBody.put("msgMessage", msgMessageQueueArc.getMsgMessage());
        requestBody.put("msgAccSenderName", msgMessageQueueArc.getMsgSenderIdName());

        msgMessageQueueArc.setMsgCreatedDate(new Date());
        msgMessageQueueArc.setMsgCreatedTime(new Date());
        msgMessageQueueArc = msgMessageQueueArcRepository.save(msgMessageQueueArc);

        accountService.handleUpdateOfAccountBalance(msgMessageQueueArc.getMsgCostId(), msgMessageQueueArc.getMsgAccId(), msgMessageQueueArc.getMsgResellerId());


        try {
            SMSReport response = restTemplate.postForObject(AIRTEL_END_POINT, requestBody, SMSReport.class);


            msgMessageQueueArc.setMsgStatus(response.getStatus().getName());
            msgMessageQueueArc.setMsgDeliveredDate(new Date());
            msgMessageQueueArc.setMsgClientDeliveryStatus("PENDING");
            msgMessageQueueArc.setMsgRetryCount(0);
            msgMessageQueueArc.setMsgCode(response.getMessageId());
            msgMessageQueueArc.setMsgErrorDesc(response.toString());
            msgMessageQueueArcRepository.save(msgMessageQueueArc);

            log.info("Sent to AirTel : {}", response);
        } catch (Exception e) {
            log.error("Error sending Airtel message", e);
            throw e;
        }
    }


    @RabbitListener(queues = MQConfig.AIRTEL_DNR, containerFactory = "rabbitListenerContainerFactory")
    public void processDNRresponse(Channel channel, Message message) {

        try {
            taskExecutor.execute(() -> processResponse(message, channel));
        } catch (RejectedExecutionException e) {
            new Thread(() -> processResponse(message, channel)).start(); // Execute in the caller's thread as a last resort
        }
    }


    private void processResponse(Message message, Channel channel) {
        SMSReport smsReport = null;

        try {
            byte[] payload = message.getBody();
            smsReport = objectMapper.readValue(payload, SMSReport.class);


            MsgMessageQueueArc msg = msgMessageQueueArcRepository.findByMsgCode(smsReport.getMessageId()).orElse(null);
            if(msg != null) {
                msg.setMsgStatus(smsReport.getStatus().getName());
                msg.setMsgDeliveredDate(new Date());
                msg.setMsgClientDeliveryStatus("PENDING");
                msg.setMsgRetryCount(0);
                msg.setMsgCode(smsReport.getMessageId());
                msgMessageQueueArcRepository.saveAndFlush(msg);
            }

            if (channel.isOpen()) {
                channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
            }


        } catch (Exception e) {
            log.error(e.getMessage());
            try {
                if (channel.isOpen()) {
                    channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, true);
                }
            } catch (Exception eb) {
                eb.printStackTrace();
            }
        }


    }


}
