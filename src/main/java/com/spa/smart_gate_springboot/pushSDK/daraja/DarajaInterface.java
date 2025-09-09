package com.spa.smart_gate_springboot.pushSDK.daraja;

import com.spa.smart_gate_springboot.pushSDK.daraja.dto.DarajaTokenResponse;
import com.spa.smart_gate_springboot.pushSDK.daraja.dto.StkPushRequest;
import com.spa.smart_gate_springboot.pushSDK.daraja.dto.StkPushResponse;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface DarajaInterface {

    @GET("/oauth/v1/generate")
    Call<DarajaTokenResponse> getAccessToken(
            @Header("Authorization") String authorization,
            @Query("grant_type") String grantType
    );

    @POST("/mpesa/stkpush/v1/processrequest")
    Call<StkPushResponse> stkPush(
            @Header("Authorization") String authorization,
            @Header("Content-Type") String contentType,
            @Body StkPushRequest request
    );
}
