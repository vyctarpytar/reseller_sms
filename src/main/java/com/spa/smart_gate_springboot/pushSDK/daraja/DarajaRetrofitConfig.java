package com.spa.smart_gate_springboot.pushSDK.daraja;

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
public class DarajaRetrofitConfig {

    private static final String DARAJA_BASE_URL = "https://api.safaricom.co.ke";

    @Bean
    public Retrofit darajaRetrofit() {
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .addInterceptor(createLoggingInterceptor())
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();

        return new Retrofit.Builder()
                .baseUrl(DARAJA_BASE_URL)
                .addConverterFactory(JacksonConverterFactory.create())
                .client(okHttpClient)
                .build();
    }

    @Bean
    public DarajaInterface darajaClient(Retrofit darajaRetrofit) {
        return darajaRetrofit.create(DarajaInterface.class);
    }

    private Interceptor createLoggingInterceptor() {
        return new HttpPublisherInterceptor();
    }
}
