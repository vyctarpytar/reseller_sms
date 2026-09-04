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

    /**
     * Concurrency of the SMS send listener. Because that listener processes synchronously on the
     * consumer thread, these ARE the concurrent-send counts, and throughput is exactly
     * {@code concurrency / per-message-latency} — at 16 consumers and a ~296ms per-message round trip
     * the queue drained at ~54 msg/s with every consumer busy 100% of the time (unacked == consumers).
     * Externalised so the ceiling can be retuned against the carrier's real TPS limit without a rebuild.
     */
    @Value("${sms.listener.concurrency:16}")
    private int smsConcurrency;

    @Value("${sms.listener.max-concurrency:32}")
    private int smsMaxConcurrency;

    /** Same, for the DLR listener — carriers post back roughly one DLR per sent SMS, so it has to scale in step. */
    @Value("${dlr.listener.concurrency:8}")
    private int dlrConcurrency;

    @Value("${dlr.listener.max-concurrency:16}")
    private int dlrMaxConcurrency;


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
        // Only the DLR listener (SafDlrService) uses this factory. Carriers post back ~1 DLR per sent
        // SMS, so its concurrency has to track the send side: at 8 consumers it was already running at
        // ~42/s deliver against ~46/s inbound, i.e. barely keeping up at the OLD send rate. Raised with
        // the senders so delivery status doesn't lag behind the sends that produced it.
        factory.setConcurrentConsumers(dlrConcurrency);
        factory.setMaxConcurrentConsumers(dlrMaxConcurrency);
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
     *  - concurrency == consumer threads, so concurrent/maxConcurrent IS the concurrent-send count.
     *    A send thread spends the large majority of its cycle blocked on the carrier's socket, and it
     *    holds NO DB connection while it waits, so consumer count is not bounded by the Hikari pool the
     *    way a CPU- or DB-bound worker would be — 16 was far below what 4 cores can carry. Raised to
     *    16/32 (see {@code sms.listener.*}); the next ceiling above this is the carrier's own TPS limit
     *    and the per-account row lock in {@code AccountRepository.updateAccountMsgBal}, not this box;
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
        factory.setConcurrentConsumers(smsConcurrency);
        factory.setMaxConcurrentConsumers(smsMaxConcurrency);
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
        //
        // maxPoolSize MUST stay > sms.listener.max-concurrency, hence the derivation below rather than
        // two independently-edited literals. With queueCapacity=0 the pool hands each consumer its own
        // thread on demand; if the pool ever ran out, CallerRunsPolicy would run a consumer's blocking
        // receive loop on the container's own thread and wedge it.
        executor.setCorePoolSize(smsConcurrency);
        executor.setMaxPoolSize(smsMaxConcurrency + 4);
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
        // Supplies the DLR container's consumer threads (nothing else uses this executor). Same shape
        // and same constraint as smsListenerTaskExecutor: maxPoolSize > dlr.listener.max-concurrency.
        //
        // queueCapacity is 0 on purpose. A ThreadPoolExecutor only grows past corePoolSize once its
        // queue is FULL, so the old core=12 / queue=200 pairing meant consumers 13..N would have been
        // parked in the queue and never started — harmless while maxConcurrentConsumers was 8 (below
        // core), a silent cap the moment it is raised above it. With no queue, each consumer gets a
        // thread immediately, up to maxPoolSize.
        executor.setCorePoolSize(dlrConcurrency);
        executor.setMaxPoolSize(dlrMaxConcurrency + 4);
        executor.setQueueCapacity(0);
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