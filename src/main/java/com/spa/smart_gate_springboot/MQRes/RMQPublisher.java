package com.spa.smart_gate_springboot.MQRes;

import com.spa.smart_gate_springboot.messaging.send_message.MsgQueue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.util.TextUtils;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
@Slf4j
public class RMQPublisher {

    private final RabbitAdmin rabbitAdmin;
//    private final GlobalUtils globalUtils;
    @Value("${rabbitmq.exchange.name}")
    private String XEXCHANGE;
    @Value("${rabbitmq.exchange.name}")
    private String XEXCHANGE_KEY;

    public <T> void publishToOutQueue(T response, String outQueue) {

        // Idempotency safety net: stamp a stable dedup key on every SMS payload as it's published, so a
        // RabbitMQ redelivery is deduped by the consumer's unique-key insert (no double-debit/send). This
        // is the one chokepoint all SMS sends funnel through (web, API, retry cron), so a missing key
        // here can't slip a send through unkeyed. Callers that want client-level idempotency (e.g. the
        // API path keying off the client's msgExternalId) set it before publishing; we never overwrite.
        if (response instanceof MsgQueue mq && TextUtils.isEmpty(mq.getMsgDedupKey())) {
            mq.setMsgDedupKey(java.util.UUID.randomUUID().toString());
        }

        String mqExchange = outQueue + "_" + XEXCHANGE;
        String mqKey = outQueue + "_" + XEXCHANGE_KEY;

        DirectExchange exchange = new DirectExchange(mqExchange);
        rabbitAdmin.declareExchange(exchange);
        Queue queue = new Queue(outQueue, true, false, false);
        rabbitAdmin.declareQueue(queue);

        Binding binding = BindingBuilder.bind(queue).to(exchange).with(mqKey);
        rabbitAdmin.declareBinding(binding);

        RabbitTemplate rabbitTemplate = rabbitAdmin.getRabbitTemplate();
        rabbitTemplate.setMessageConverter(new Jackson2JsonMessageConverter());
        rabbitTemplate.convertAndSend(mqExchange, mqKey, response);

//            globalUtils.printToJson(response, "success");


    }


}