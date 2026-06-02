package com.spa.smart_gate_springboot.messaging.send_message.safaricom_rest;

import com.spa.smart_gate_springboot.messaging.send_message.safaricom_rest.dto.SafaricomRestLoginRequest;
import com.spa.smart_gate_springboot.messaging.send_message.safaricom_rest.dto.SafaricomRestOAuthResponse;
import com.spa.smart_gate_springboot.messaging.send_message.safaricom_rest.dto.SafaricomRestSendRequest;
import com.spa.smart_gate_springboot.messaging.send_message.safaricom_rest.dto.SafaricomRestSendResponse;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface SafaricomRestInterface {

    @Headers({
            "accept: application/json",
            "X-Requested-With: XMLHttpRequest",
            "X-Country: KEN",
            "Content-Type: application/json"
    })
    @POST("/api/auth/login")
    Call<SafaricomRestOAuthResponse> login(@Body SafaricomRestLoginRequest body);

    @Headers({
            "accept: application/json",
            "X-Requested-With: XMLHttpRequest",
            "X-Country: KEN",
            "Content-Type: application/json"
    })
    @GET("/api/auth/RefreshToken")
    Call<SafaricomRestOAuthResponse> refreshToken(@Header("X-Authorization") String bearerRefreshToken);

    @Headers({
            "accept: application/json",
            "X-Requested-With: XMLHttpRequest",
            "X-Country: KEN",
            "Content-Type: application/json"
    })
    @POST("/api/public/CMS/bulksms")
    Call<SafaricomRestSendResponse> sendBulkSms(
            @Header("X-Authorization") String bearerToken,
            @Body SafaricomRestSendRequest body
    );
}
