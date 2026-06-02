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
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
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
