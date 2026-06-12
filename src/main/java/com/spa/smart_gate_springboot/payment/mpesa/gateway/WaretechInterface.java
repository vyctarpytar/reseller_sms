package com.spa.smart_gate_springboot.payment.mpesa.gateway;

import com.spa.smart_gate_springboot.payment.mpesa.gateway.dto.*;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Query;

/**
 * Retrofit client for the Waretech M-Pesa gateway (https://c2b.waretechlimited.com).
 * The gateway proxies Safaricom Daraja and holds the initiator/security credentials internally.
 */
public interface WaretechInterface {

    @POST("mpesa/v2/stkpush")
    Call<GatewayStkResponse> stkPush(@Body GatewayStkRequest request);

    @POST("mpesa/v2/b2c")
    Call<GatewayB2cResponse> b2c(@Body GatewayB2cRequest request);

    @POST("mpesa/v2/get-transaction-status")
    Call<GatewayStatusResponse> getTransactionStatus(@Query("conversationId") String conversationId);
}
