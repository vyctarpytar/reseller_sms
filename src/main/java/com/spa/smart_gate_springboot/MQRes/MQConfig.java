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

    public static final String QUEUE = "SMART_GATE_V2_RECEIVE";
    public static final String OUT_OF_CREDIT_QUEUE = "SMART_GATE_V2_OUT_OF_CREDIT_SPA";
    public static final String OUT_OF_CREDIT_QUEUE_SYNQ = "SMART_GATE_V2_OUT_OF_CREDIT_SYNQ";
    public static final String BALANCE_UPDATE = "smart.gate.v2.update.account.balance";
    public static final String SYNQ_QUEUE = "SMART_GATE_V2_RECEIVE_SYNQ";
    public static final String WEISER_RESPONSE = "SMART_GATE_V2_WEISER_RESPONSE";
    public static final String ERROR_QUEUE = "SMART_GATE_V2_ERRORS";
    public static final String EXCHANGE = "sgr_message_exchange";
    public static final String FAILED_EXCHANGE = "sgr_message_exchange_dlq";
    public static final String ROUTING_KEY = "sgr_routing_Key";
    public static final String AIRTEL_DNR = "airtel.dnr";
    public static final String UPDATE_ACCOUNT_BALANCE = "smart.gate.v2.update.account.balance";
    public static final String INCOMING_SMS_DLR = "sdp_gateway.sms.dlr";

    @Bean
    public Queue queue() {
        return new Queue(QUEUE, true, false, false);
    }

    @Bean
    public Queue queue_weiser_response() {
        return new Queue(WEISER_RESPONSE, true, false, false);
    }

    @Bean
    public Queue errors() {
        return new Queue(ERROR_QUEUE, true, false, false);
    }

    @Bean
    public Queue airtelRepone() {
        return new Queue(AIRTEL_DNR, true, false, false);
    }

    @Bean
    public Queue outOfCreditQueue() {
        return new Queue(OUT_OF_CREDIT_QUEUE, true, false, false);
    }

    @Bean
    public Queue balanceUpdate() {
        return new Queue(BALANCE_UPDATE, true, false, false);
    }

    @Bean
    public Queue outOfCreditQueueSynq() {
        return new Queue(OUT_OF_CREDIT_QUEUE_SYNQ, true, false, false);
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
    public TopicExchange exchange() {
        return new TopicExchange(EXCHANGE);
    }

    @Bean
    public TopicExchange failed_Exchange() {
        return new TopicExchange(FAILED_EXCHANGE);
    }

    @Bean
    public Binding binding(Queue queue, TopicExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(ROUTING_KEY);
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
