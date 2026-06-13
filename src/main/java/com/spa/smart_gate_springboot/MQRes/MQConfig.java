package com.spa.smart_gate_springboot.MQRes;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MQConfig {

    // Only the live queues remain. SMS sends flow through SYNQ_QUEUE (and the legacy QUEUE), both
    // consumed by MQReceiverSynq; carrier delivery reports arrive on INCOMING_SMS_DLR (SafDlrService).
    // Messages are published via RMQPublisher's per-queue DirectExchanges, so no topic exchange/binding
    // is declared here.
    //
    // Removed as dead/vestigial:
    //  - OUT_OF_CREDIT_QUEUE_SYNQ — no-credit sends are now persisted in place (PENDING_CREDIT arc),
    //    so nothing publishes here and the consumer is gone.
    //  - ERROR_QUEUE / WEISER_RESPONSE / AIRTEL_DNR — never had an active consumer.
    //  - sgr_message_exchange (+ _dlq) topic exchanges, their binding and ROUTING_KEY — real publishing
    //    uses DirectExchanges, so these were declared but never on the delivery path.
    // The corresponding broker queues/exchanges (if already created) are now orphaned and can be
    // deleted manually; leaving them is harmless.
    public static final String QUEUE = "SMART_GATE_V2_RECEIVE";
    public static final String SYNQ_QUEUE = "SMART_GATE_V2_RECEIVE_SYNQ";
    public static final String INCOMING_SMS_DLR = "sdp_gateway.sms.dlr";

    @Bean
    public Queue queue() {
        return new Queue(QUEUE, true, false, false);
    }

    @Bean
    public Queue synqReceiveQueue() {
        return new Queue(SYNQ_QUEUE, true, false, false);
    }

    @Bean
    public Queue deliverNotes() {
        return new Queue(INCOMING_SMS_DLR, true, false, false);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public AmqpTemplate template(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }
}
