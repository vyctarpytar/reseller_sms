package com.spa.smart_gate_springboot.messaging.send_message.safaricom_rest;

import com.spa.smart_gate_springboot.config.intercept.HttpPublisherInterceptor;
import lombok.RequiredArgsConstructor;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

import java.util.concurrent.TimeUnit;

@Configuration
@RequiredArgsConstructor
public class SafaricomRestRetrofitConfig {

    private final SafaricomRestProperties safaricomRestProperties;

    @Bean
    public Retrofit safComRestRetrofit() {
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .addInterceptor(createLoggingInterceptor())
                // Per-phase timeouts kept generous (unchanged from the original) so legitimate slow
                // carrier responses don't fail; the whole-call ceiling below is what actually bounds the
                // thread-hold time now that the send runs synchronously on the RabbitMQ consumer thread.
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                // Hard whole-call ceiling. Without it the per-phase timeouts could stack to ~180s and pin
                // a consumer thread (and its prefetch slot) that long. 90s bounds it like the SDP v1 client.
                .callTimeout(90, TimeUnit.SECONDS)
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
        return new HttpPublisherInterceptor();
    }
}
