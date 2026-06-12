package com.spa.smart_gate_springboot.payment.mpesa.gateway;

import com.spa.smart_gate_springboot.config.intercept.HttpPublisherInterceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

import java.util.concurrent.TimeUnit;

/**
 * Retrofit setup for the Waretech M-Pesa gateway. Base URL and an optional bearer token come from
 * config (application.properties); the deployed gateway uses internal Safaricom credentials, so the
 * Authorization header is sent only when {@code mpesa.gateway.authToken} is set.
 */
@Configuration
public class WaretechRetrofitConfig {

    @Value("${mpesa.gateway.baseUrl:https://c2b.waretechlimited.com}")
    private String baseUrl;

    @Value("${mpesa.gateway.authToken:}")
    private String authToken;

    @Bean
    public Retrofit waretechRetrofit() {
        OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder()
                .addInterceptor(new HttpPublisherInterceptor())
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS);

        if (authToken != null && !authToken.isBlank()) {
            clientBuilder.addInterceptor(chain -> {
                Request authed = chain.request().newBuilder()
                        .header("Authorization", authToken.startsWith("Bearer ") ? authToken : "Bearer " + authToken)
                        .build();
                return chain.proceed(authed);
            });
        }

        String base = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
        return new Retrofit.Builder()
                .baseUrl(base)
                .addConverterFactory(JacksonConverterFactory.create())
                .client(clientBuilder.build())
                .build();
    }

    @Bean
    public WaretechInterface waretechClient(Retrofit waretechRetrofit) {
        return waretechRetrofit.create(WaretechInterface.class);
    }
}
