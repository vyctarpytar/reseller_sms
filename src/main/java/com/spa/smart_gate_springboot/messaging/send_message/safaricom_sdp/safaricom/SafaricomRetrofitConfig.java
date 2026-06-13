package com.spa.smart_gate_springboot.messaging.send_message.safaricom_sdp.safaricom;

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
public class SafaricomRetrofitConfig {

    private final SafaricomProperties safaricomProperties;


    @Bean
    public Retrofit safComRetrofit() {
        // Sane timeouts: a send must not pin a worker thread for minutes. The old 600s connect / 300s
        // read meant one hung Safaricom socket held an rmqListener thread for up to 10 minutes — under
        // load that drains the worker pool. Bound the dispatcher so we never open more concurrent
        // connections to Safaricom than the worker pool can drive.
        okhttp3.Dispatcher dispatcher = new okhttp3.Dispatcher();
        dispatcher.setMaxRequests(32);
        dispatcher.setMaxRequestsPerHost(32);

        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .addInterceptor(createLoggingInterceptor())  // Add the logging interceptor
                .dispatcher(dispatcher)
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .callTimeout(90, TimeUnit.SECONDS)     // hard ceiling on the whole call
                .retryOnConnectionFailure(true)
                .build();

        return new Retrofit.Builder()
                .baseUrl(safaricomProperties.getSafBaseUrl())
                .addConverterFactory(JacksonConverterFactory.create())
                .client(okHttpClient)
                .build();
    }



    @Bean
    public SafaricomInterface safaricomClient(Retrofit safComRetrofit) {
        return safComRetrofit.create(SafaricomInterface.class);
    }


    private Interceptor createLoggingInterceptor() {
        return new HttpPublisherInterceptor();
    }
}

