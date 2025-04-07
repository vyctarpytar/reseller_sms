package com.spa.smart_gate_springboot.messaging.send_message;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.spa.smart_gate_springboot.MQRes.MQConfig;
import com.spa.smart_gate_springboot.account_setup.blacklist.BlackListService;
import com.spa.smart_gate_springboot.messaging.send_message.safaricom_sdp.safaricom.dto.Datum;
import com.spa.smart_gate_springboot.messaging.send_message.safaricom_sdp.safaricom.dto.SmsResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.util.TextUtils;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.concurrent.RejectedExecutionException;


@Service
@RequiredArgsConstructor
@EnableRabbit
@Slf4j
public class SafDlrService {

    private final ThreadPoolTaskExecutor taskExecutor;
    private final ObjectMapper objectMapper;
    private final MsgMessageQueueArcRepository msgRepo;
    private final BlackListService blackListService;
//    private final BlackListService blackListService;


    @RabbitListener(queues = {MQConfig.INCOMING_SMS_DLR}, containerFactory = "rabbitListenerContainerFactory")
    public void consumeMessage(Channel channel, Message message) {
        try {
            taskExecutor.execute(() -> consumerAction(channel, message));
        } catch (RejectedExecutionException e) {
            new Thread(() -> consumerAction(channel, message)).start();
        }

    }

    private void consumerAction(Channel channel, Message message) {

        byte[] payload = message.getBody();
        long tag = message.getMessageProperties().getDeliveryTag();

        SmsResponse res = null;
        try {
            res = objectMapper.readValue(payload, SmsResponse.class);
        } catch (IOException exc) {
            throw new RuntimeException(exc);
        }


        String msgCode = null;
        String msgStatus = null;
        String msgmsisdn = null;

        for (Datum data : res.getRequestParam().data) {
            if (data.getName().equalsIgnoreCase("correlatorId")) {
                msgCode = data.getValue();

            } else if (data.getName().equalsIgnoreCase("Msisdn")) {
                msgmsisdn = data.getValue();

            } else if (data.getName().equalsIgnoreCase("Description")) {
                msgStatus = data.getValue();

            }
        }

        if (TextUtils.isEmpty(msgCode) && TextUtils.isEmpty(msgStatus)) {
            log.error("CorrelatorId and Description are empty");
            return;
        }


        try {

            updateDeliveryNote(msgStatus,res.getRequestId(),msgmsisdn,msgCode);

           if(channel.isOpen()) channel.basicAck(tag, false);
        } catch (Exception e) {
            log.error("Error at Updating DNRs : {}", e.getMessage());
            try {
                if(channel.isOpen())   channel.basicNack(message.getMessageProperties().getDeliveryTag(), true, true);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }

    }

    public void updateDeliveryNote(String msgStatus, String msgRequestId, String msisdn, String msgCode) {
        msgRepo.updateDeliverNote(msgStatus, msgRequestId, msisdn, msgCode);

        if(msgStatus.equalsIgnoreCase("SenderName Blacklisted")){
            blackListService.addToBlacklist(msisdn);
        }
    }


}
