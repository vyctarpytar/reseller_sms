package com.spa.smart_gate_springboot.messaging.send_message;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.spa.smart_gate_springboot.MQRes.MQConfig;
import com.spa.smart_gate_springboot.account_setup.blacklist.BlackListService;
import com.spa.smart_gate_springboot.messaging.send_message.dtos.SmsDlr;
import com.spa.smart_gate_springboot.messaging.send_message.safaricom_sdp.safaricom.dto.Datum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.util.TextUtils;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;


@Service
@RequiredArgsConstructor
@EnableRabbit
@Slf4j
public class SafDlrService {

    private final ObjectMapper objectMapper;
    private final MsgMessageQueueArcRepository msgRepo;
    private final BlackListService blackListService;


    @RabbitListener(queues = {MQConfig.INCOMING_SMS_DLR}, containerFactory = "rabbitListenerContainerFactory")
    public void consumeMessage(Channel channel, Message message) {
        // Process SYNCHRONOUSLY on the consumer thread that owns this Channel (no taskExecutor hand-off),
        // so the ack is issued on the right thread and the finally block guarantees it. A delivery report
        // is best-effort status metadata — on failure we ack-and-log instead of requeueing. The old code
        // nacked with requeue=true (and multiple=true) on every error, turning one poison DLR into an
        // infinite redelivery loop with no DLQ and no attempt cap.
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            consumerAction(message);
        } catch (Exception e) {
            log.error("Error at Updating DNRs : {}", e.getMessage(), e);
        } finally {
            ack(channel, deliveryTag);
        }
    }

    private void consumerAction(Message message) {
        byte[] payload = message.getBody();
        log.info("Safaricom response: {}", new String(payload, StandardCharsets.UTF_8));

        SmsDlr res;
        try {
            res = objectMapper.readValue(payload, SmsDlr.class);
        } catch (IOException exc) {
            throw new RuntimeException(exc);
        }
        log.info("safaricom res: {} ", res);

        if (res == null || TextUtils.isEmpty(res.getRequestId())) {
            return;
        }

        String msgCode = null;
        String msgStatus = null;
        String msgmsisdn = null;
        for (Datum data : res.getRequestParam().getData()) {
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

        updateDeliveryNote(msgStatus, res.getRequestId(), msgmsisdn, msgCode);
    }

    /** Ack a delivery on the consumer thread that owns the Channel — exactly once, never throwing. */
    private void ack(Channel channel, long deliveryTag) {
        try {
            if (channel.isOpen()) {
                channel.basicAck(deliveryTag, false);
            } else {
                log.warn("DLR channel closed before ack of delivery {} — broker will redeliver", deliveryTag);
            }
        } catch (IOException e) {
            log.error("Failed to ack DLR delivery {}: {}", deliveryTag, e.getMessage());
        }
    }

    public void updateDeliveryNote(String msgStatus, String msgRequestId, String msisdn, String msgCode) {
        msgRepo.updateDeliverNote(msgStatus, msgRequestId, msisdn, msgCode);

        if (msgStatus.equalsIgnoreCase("SenderName Blacklisted")) {
            blackListService.addToBlacklist(msisdn);
        }
    }
}
