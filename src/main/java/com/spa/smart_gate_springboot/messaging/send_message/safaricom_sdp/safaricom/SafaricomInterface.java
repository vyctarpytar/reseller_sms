package com.spa.smart_gate_springboot.messaging.send_message.safaricom_sdp.safaricom;


import com.spa.smart_gate_springboot.messaging.send_message.safaricom_sdp.safaricom.dto.BulkResponse;
import com.spa.smart_gate_springboot.messaging.send_message.safaricom_sdp.safaricom.models.SafAuthReq;
import com.spa.smart_gate_springboot.messaging.send_message.safaricom_sdp.safaricom.models.SendBulkSafReq;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface SafaricomInterface {

    @POST("/api/public/CMS/bulksms")
    Call<BulkResponse> sendBulkSms(@Header("Content-Type") String contentType,
                                   @Header("X-Requested-With") String requestWith,
                                   @Header("X-Authorization") String authoraization,
                                   @Body SendBulkSafReq body);


    @POST("/api/auth/login")
    Call<SafTokenResponse> getToken(@Header("Content-Type") String contentType,
                                           @Header("X-Requested-With") String requestWith,
                                           @Body SafAuthReq body);
}
