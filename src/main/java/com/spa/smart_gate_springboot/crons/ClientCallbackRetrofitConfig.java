package com.spa.smart_gate_springboot.crons;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

import java.util.concurrent.TimeUnit;

/**
 * Retrofit setup for delivery-report callbacks. The {@link CallbackLoggingInterceptor}
 * logs the request body and response body as JSON, so callback problems (e.g. a 403
 * from the client server) are visible in the logs.
 */
@Configuration
public class ClientCallbackRetrofitConfig {

    @Bean
    public Retrofit clientCallbackRetrofit(ObjectMapper objectMapper) {
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .addInterceptor(new CallbackLoggingInterceptor(objectMapper))
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .build();

        return new Retrofit.Builder()
                // Placeholder only — every call passes an absolute @Url that overrides this.
                .baseUrl("http://localhost/")
                // Reuse the app ObjectMapper (JSR-310 etc.) so LocalDateTime serialises correctly.
                .addConverterFactory(JacksonConverterFactory.create(objectMapper))
                .client(okHttpClient)
                .build();
    }

    @Bean
    public ClientCallbackInterface clientCallbackClient(Retrofit clientCallbackRetrofit) {
        return clientCallbackRetrofit.create(ClientCallbackInterface.class);
    }
}
