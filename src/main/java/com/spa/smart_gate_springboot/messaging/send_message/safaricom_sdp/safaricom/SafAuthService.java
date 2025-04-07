package com.spa.smart_gate_springboot.messaging.send_message.safaricom_sdp.safaricom;


import com.spa.smart_gate_springboot.messaging.send_message.safaricom_sdp.safaricom.models.SafAuthReq;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.util.TextUtils;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import retrofit2.Call;
import retrofit2.Response;

import java.io.IOException;

@Service
@RequiredArgsConstructor
@Slf4j
@EnableScheduling
public class SafAuthService {

    public static final String SMS_GATEWAY_SAF_TOKEN = "SMS_GATEWAY_SAF_TOKEN";
    private final SafaricomProperties safaricomProperties;

    private final SafaricomInterface safaricomInterface;

    public String getTokenFromSafaricom() throws IOException {


        SafAuthReq safAuthReq = SafAuthReq.builder().password(safaricomProperties.getSafPassword()).username(safaricomProperties.getSafApiUserName()).build();

//        String json = AppFunctions.convertToStringJson(safAuthReq);

        Call<SafTokenResponse> call = safaricomInterface.getToken(String.valueOf(MediaType.APPLICATION_JSON), "XMLHttpRequest", safAuthReq);

        Response<SafTokenResponse> res = call.execute();

        if (res.isSuccessful()) {
            assert res.body() != null;
            logTokenToRedis(res.body().getToken());
            return res.body().getToken();


        }
       throw new RuntimeException("Safaricom Error : could not obtain token");

    }

    private void logTokenToRedis(String token) {

        try {
            int tokenExpiresInMinutes = 50 ; // minutes

            log.info("Token will expire in {} seconds.", tokenExpiresInMinutes);


        } catch (Exception e) {
            System.err.println("Error Caching token: " + e.getMessage());
        }
    }


    public String getAccessToken() throws IOException {
        String token = getTokenfromRedis() == null ? getTokenFromSafaricom() : getTokenfromRedis();
        log.warn(token);
        return token;
    }


    public String getTokenfromRedis() {
//        String redisToken = redisManagerService.getValueByKey(SMS_GATEWAY_SAF_TOKEN, SMS_GATEWAY_SAF_TOKEN);
        String redisToken = null;
        log.info("token from redis found : {}", !TextUtils.isEmpty(redisToken));
        return redisToken;
    }


    @Scheduled(fixedRate = 20 * 60 * 1000)  // run ever 20 minutes
    public void init() {
        try {
//            redisManagerService.deleteValueByKey(SMS_GATEWAY_SAF_TOKEN,SMS_GATEWAY_SAF_TOKEN);
            getTokenFromSafaricom();
        } catch (Exception e) {
            log.error("Error getting access token", e);
        }
    }


}

