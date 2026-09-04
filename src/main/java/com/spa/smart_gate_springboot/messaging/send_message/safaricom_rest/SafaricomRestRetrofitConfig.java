package com.spa.smart_gate_springboot.messaging.send_message.safaricom_rest;

import com.spa.smart_gate_springboot.config.intercept.HttpPublisherInterceptor;
import lombok.RequiredArgsConstructor;
import okhttp3.ConnectionPool;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Configuration
@RequiredArgsConstructor
public class SafaricomRestRetrofitConfig {

    private final SafaricomRestProperties safaricomRestProperties;

    @Bean
    public Retrofit safComRestRetrofit() {
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .addInterceptor(createLoggingInterceptor())
                // Per-phase timeouts. Read is the one that bites: the prod DSDP endpoint sometimes
                // accepts the request but is slow to return response headers, so it's the most generous.
                // The send runs synchronously on the RabbitMQ consumer thread, so these also bound how
                // long a worker (and its prefetch slot) is pinned — see the whole-call ceiling below.
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(120, TimeUnit.SECONDS)
                // Hard whole-call ceiling. Without it the per-phase timeouts could stack and pin a
                // consumer thread far longer. 180s leaves headroom for a full 120s read after connect+write.
                .callTimeout(180, TimeUnit.SECONDS)
                // Pin to HTTP/1.1. The DSDP prod endpoint negotiates HTTP/2, which multiplexes every
                // concurrent send onto a few shared connections — so when one connection stalls, all
                // streams on it time out together (the correlated same-millisecond SocketTimeouts we saw).
                // v1's SDP endpoint ran HTTP/1.1, giving each in-flight send its own pooled connection, so
                // a single stalled connection never took out the others. Force the same behaviour here.
                .protocols(List.of(Protocol.HTTP_1_1))
                // Keep-alive pool sized ABOVE the send concurrency (sms.listener.max-concurrency). One
                // connection per in-flight send follows from pinning HTTP/1.1 above, and OkHttp's default
                // pool retains only 5 IDLE connections — past that it evicts, so bursty load re-dials and
                // pays a fresh TCP+TLS handshake to the carrier on the hot path. Sized generously; idle
                // sockets cost nothing and are reaped after 5 minutes anyway.
                .connectionPool(new ConnectionPool(64, 5, TimeUnit.MINUTES))
                .build();

        return new Retrofit.Builder()
                .baseUrl(safaricomRestProperties.getBaseUrl())
                .addConverterFactory(JacksonConverterFactory.create())
                .client(okHttpClient)
                .build();
    }

    @Bean
    public SafaricomRestInterface safaricomRestClient(Retrofit safComRestRetrofit) {
        return safComRestRetrofit.create(SafaricomRestInterface.class);
    }

    private Interceptor createLoggingInterceptor() {
        // quietOnSuccess: this client runs once per SMS, so successful sends log their bodies at DEBUG.
        // The per-send outcome (and its carrier round-trip time) is logged by SafaricomRestBulkService,
        // and failures still surface here at warn/error.
        return new HttpPublisherInterceptor(true);
    }
}
