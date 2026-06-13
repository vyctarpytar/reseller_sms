package com.spa.smart_gate_springboot.MQRes;


import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.backoff.ExponentialBackOff;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableRabbit
@Slf4j
public class RabbitMQConfig {

    @Value("${spring.rabbitmq.host}")
    private String rabbitmqHost;

    @Value("${spring.rabbitmq.username}")
    private String rabbitmqUsername;

    @Value("${spring.rabbitmq.password}")
    private String rabbitmqPassword;

    @Value("${spring.rabbitmq.port}")
    private String rabbitmqPort;


    @Bean
    public ConnectionFactory connectionFactory() {
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory(rabbitmqHost);
        connectionFactory.setUsername(rabbitmqUsername);
        connectionFactory.setPassword(rabbitmqPassword);
        connectionFactory.setPort(Integer.parseInt(rabbitmqPort));

        return connectionFactory;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        // Sized for 4 cores: the container threads just pull + dispatch to the worker pool below.
        factory.setConcurrentConsumers(4);
        factory.setMaxConcurrentConsumers(8);
        factory.setPrefetchCount(10); // cap unacked messages held per consumer
        factory.setTaskExecutor(rabbitListenerTaskExecutor());
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL); // Use manual acknowledgements
        factory.setRecoveryBackOff(new ExponentialBackOff(1000, 2.0)); // Start with 1 sec, doubling on each retry
        factory.setContainerCustomizer(container -> container.setShutdownTimeout(5000));
        factory.setErrorHandler(t -> log.error("Error in RMQ lditener", t));

        return factory;
    }


    /**
     * Dedicated factory for the heavy SMS-send listener (MQReceiverSynq.consumeMessage). That listener
     * now processes each message SYNCHRONOUSLY on the consumer thread (no hand-off to a worker pool),
     * so the manual ack is issued on the thread that owns the Channel — fixing the cross-thread ack that
     * left messages stuck unacked. Consequences of the synchronous model, encoded here:
     *  - concurrency == consumer threads, so concurrent/maxConcurrent IS the concurrent-send count
     *    (kept near the prior ~16 worker sweet spot, still well under the 25-conn Hikari pool because a
     *    send holds no DB connection during the carrier HTTP call);
     *  - prefetch=1 gives true backpressure: a slow/stalled carrier pins only its own consumer and never
     *    hoards a backlog of unacked messages behind it;
     *  - defaultRequeueRejected=false so a delivery is never requeue-looped — failures are re-driven by
     *    the DB-status retry cron (SchedulingConfig), not by AMQP redelivery (which would double-debit).
     * The DLR + out-of-credit listeners stay on rabbitListenerContainerFactory, untouched.
     */
    @Bean
    public SimpleRabbitListenerContainerFactory smsListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setConcurrentConsumers(8);
        factory.setMaxConcurrentConsumers(16);
        factory.setPrefetchCount(1);
        factory.setTaskExecutor(smsListenerTaskExecutor());
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        factory.setDefaultRequeueRejected(false);
        factory.setRecoveryBackOff(new ExponentialBackOff(1000, 2.0));
        factory.setContainerCustomizer(container -> container.setShutdownTimeout(5000));
        factory.setErrorHandler(t -> log.error("Error in SMS RMQ listener", t));
        return factory;
    }

    @Bean
    public ThreadPoolTaskExecutor smsListenerTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // Supplies the SMS send container's consumer threads ONLY (receiver() runs inline on these, no
        // tasks are queued onto this pool). Sized to hold up to maxConcurrentConsumers long-lived
        // consumer threads with a little headroom; queueCapacity=0 because these are threads, not tasks.
        executor.setCorePoolSize(8);
        executor.setMaxPoolSize(18);
        executor.setQueueCapacity(0);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("smsListener-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }


    @Bean
    public ThreadPoolTaskExecutor rabbitListenerTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // The SMS workers are I/O-bound (DB debit + carrier HTTP). On 4 cores, ~16 concurrent sends is
        // the sweet spot — enough to overlap network waits, few enough to fit under the 25-conn DB pool
        // (leaving headroom for Tomcat + crons) and not thrash the CPU. The old 100/200 oversubscribed
        // the box ~10x: 200 threads = ~200MB of stacks + constant context-switching, all contending for
        // a pool that was never that big. CallerRuns applies backpressure if the queue fills.
        executor.setCorePoolSize(12);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(200);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("rmqListener-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        RabbitAdmin rabbitAdmin = new RabbitAdmin(connectionFactory);
        rabbitAdmin.getRabbitTemplate().setMessageConverter(new Jackson2JsonMessageConverter());
        return rabbitAdmin;
    }


    @Bean
    public RestTemplate restTemplate() {
        // Timeouts are mandatory: this RestTemplate calls the Airtel SMS gateway, and with no read
        // timeout a single hung carrier socket pins a worker thread forever — under load that drains
        // the whole pool. Connect fast (5s); allow a generous read window (30s) for the send to return.
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(30000);
        return new RestTemplate(factory);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(new Jackson2JsonMessageConverter());
        return rabbitTemplate;
    }
}