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
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .addInterceptor(createLoggingInterceptor())  // Add the logging interceptor
                .connectTimeout(600, TimeUnit.SECONDS) // Increased connection timeout to 5 minutes
                .readTimeout(300, TimeUnit.SECONDS)    // Increased read timeout to 2 minutes
                .writeTimeout(300, TimeUnit.SECONDS)   // Increased write timeout to 2 minutes
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

