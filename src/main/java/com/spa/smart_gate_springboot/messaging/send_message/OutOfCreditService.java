package com.spa.smart_gate_springboot.messaging.send_message;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.spa.smart_gate_springboot.MQRes.MQConfig;
import com.spa.smart_gate_springboot.MQRes.RMQPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;

@Component
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class OutOfCreditService {

    private final ObjectMapper objectMapper;
    private final MsgMessageQueueArcRepository msgMessageQueueArcRepository;
    private final RMQPublisher publisher;


    @RabbitListener(queues = MQConfig.OUT_OF_CREDIT_QUEUE, containerFactory = "rabbitListenerContainerFactory")
    public void handleOutOfCredit(Channel channel, Message message) {

        try {

          new Thread(() -> {
              try {
                  outOfCreditAction(channel, message) ;
              } catch (IOException e) {
                  throw new RuntimeException(e);
              }
          }).start();

            if (channel.isOpen()) channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (IOException e) {
            if (channel.isOpen()) {
                try {
                 channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, true);
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }

    }

    private boolean outOfCreditAction(Channel channel, Message message) throws IOException {
        byte[] payload = message.getBody();
        MsgMessageQueueArc msgMessageQueueArcDemo = objectMapper.readValue(payload, MsgMessageQueueArc.class);
        List<MsgMessageQueueArc> listMesages = msgMessageQueueArcRepository.findAllByMsgExternalIdAndMsgAccIdAndMsgSubMobileNo(msgMessageQueueArcDemo.getMsgExternalId(), msgMessageQueueArcDemo.getMsgAccId(), msgMessageQueueArcDemo.getMsgSubMobileNo());

        MsgMessageQueueArc msgMessageQueueArc = null;
        if (listMesages.size() == 1) {
            msgMessageQueueArc = listMesages.get(0);

        } else if (listMesages.size() > 1) {
            log.warn("More than one msg message queue No : {} See Request : {}", listMesages.size(), msgMessageQueueArcDemo);
            publisher.publishToOutQueue(msgMessageQueueArcDemo , "out_of_credit_more_than_one_record");
            if (channel.isOpen()) channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
            return true;
        } else {
            msgMessageQueueArc = msgMessageQueueArcDemo;
        }

        msgMessageQueueArc.setMsgStatus("OUT_OF_CREDIT");
        msgMessageQueueArc.setMsgClientDeliveryStatus("PENDING");

        msgMessageQueueArcRepository.save(msgMessageQueueArc);

        log.info("MSG:: {} Sending out-of-credit message: {}", msgMessageQueueArc.getMsgId(),msgMessageQueueArc.getMsgAccId());
        return false;
    }


}



