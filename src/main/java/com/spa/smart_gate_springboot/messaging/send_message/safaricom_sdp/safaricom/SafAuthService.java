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

    private static String SMS_GATEWAY_SAF_TOKEN;

    private final SafaricomProperties safaricomProperties;
    private final SafaricomInterface safaricomInterface;

    public String getTokenFromSafaricom() throws IOException {


        SafAuthReq safAuthReq = SafAuthReq.builder().password(safaricomProperties.getSafPassword()).username(safaricomProperties.getSafApiUserName()).build();

        Call<SafTokenResponse> call = safaricomInterface.getToken(String.valueOf(MediaType.APPLICATION_JSON), "XMLHttpRequest", safAuthReq);

        Response<SafTokenResponse> res = call.execute();

        if (res.isSuccessful()) {
            assert res.body() != null;

            SMS_GATEWAY_SAF_TOKEN = res.body().getToken();

            return SMS_GATEWAY_SAF_TOKEN;

        }
        throw new RuntimeException("Safaricom Error : could not obtain token");

    }


    public String getAccessToken() throws IOException {
        String token = getTokenFromRedis() == null ? getTokenFromSafaricom() : getTokenFromRedis();
        log.warn(token);
        return token;
    }


    public String getTokenFromRedis() {
        String redisToken = SMS_GATEWAY_SAF_TOKEN;
        log.info("token from redis found : {}", !TextUtils.isEmpty(redisToken));
        return redisToken;
    }


    @Scheduled(cron = "0 */50 * * * *")
    public void refreshToken() {
        log.info("refreshing  token  from Safaricom");
        try {
            SMS_GATEWAY_SAF_TOKEN = null;
            getTokenFromSafaricom();

        } catch (Exception e) {
            SMS_GATEWAY_SAF_TOKEN = null;
            log.error("Error getting access token", e);
        }
    }


}

