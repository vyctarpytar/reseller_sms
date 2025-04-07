package com.spa.smart_gate_springboot.account_setup.account;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.spa.smart_gate_springboot.MQRes.MQConfig;
import com.spa.smart_gate_springboot.MQRes.RMQPublisher;
import com.spa.smart_gate_springboot.account_setup.account.dtos.AccBalanceUpdate;
import com.spa.smart_gate_springboot.account_setup.reseller.ResellerService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@EnableRabbit
@Slf4j
public class AccBalanceUpdateConsumer {


    private final AccountService accountService;
    private final AccountRepository accountRepository;
    private final ResellerService resellerService;
    private final ObjectMapper objectMapper;
    private final RMQPublisher rmqPublisher;

    @RabbitListener(queues = MQConfig.BALANCE_UPDATE, containerFactory = "rabbitListenerContainerFactory")
    @Transactional
    public void consumeMessage(Channel channel, Message message) {
        AccBalanceUpdate accBalanceUpdate = null;
        try {
            byte[] payload = message.getBody();
            accBalanceUpdate = objectMapper.readValue(payload, AccBalanceUpdate.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try {

            var res = accountRepository.updateAccountMsgBal(accBalanceUpdate.getAccId(), accBalanceUpdate.getMsgCost());

            if (res == 0) throw new RuntimeException("Account not found or insufficient balance");

            if (channel.isOpen()) channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);

        } catch (Exception e) {
            if (channel.isOpen()) {
                try {
                    channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
                    rmqPublisher.publishToErrorQueue(accBalanceUpdate, "sms.update.credit.error");
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
            log.error("Error parsing acc balance update", e);
        }
    }


}


