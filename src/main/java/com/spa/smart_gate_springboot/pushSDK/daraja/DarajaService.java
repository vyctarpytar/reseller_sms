package com.spa.smart_gate_springboot.pushSDK.daraja;

import com.spa.smart_gate_springboot.pushSDK.PushSDKConfig;
import com.spa.smart_gate_springboot.pushSDK.daraja.dto.DarajaTokenResponse;
import com.spa.smart_gate_springboot.pushSDK.daraja.dto.StkPushRequest;
import com.spa.smart_gate_springboot.pushSDK.daraja.dto.StkPushResponse;
import com.spa.smart_gate_springboot.utils.GlobalUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import retrofit2.Response;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class DarajaService {

    private final DarajaInterface darajaClient;
    
    // Token cache with expiry time
    private final ConcurrentHashMap<String, TokenCache> tokenCache = new ConcurrentHashMap<>();
    
    private static class TokenCache {
        private final String token;
        private final LocalDateTime expiryTime;
        
        public TokenCache(String token, int expiresInSeconds) {
            this.token = token;
            this.expiryTime = LocalDateTime.now().plusSeconds(expiresInSeconds - 60); // Subtract 60 seconds for safety
        }
        
        public boolean isExpired() {
            return LocalDateTime.now().isAfter(expiryTime);
        }
        
        public String getToken() {
            return token;
        }
    }

    public String getAccessToken(PushSDKConfig config) throws Exception {
        String cacheKey = config.getMpConsumerKey();
        
        // Check if we have a valid cached token
        TokenCache cachedToken = tokenCache.get(cacheKey);
        if (cachedToken != null && !cachedToken.isExpired()) {
            log.debug("Using cached token for consumer key: {}", config.getMpConsumerKey());
            return cachedToken.getToken();
        }

        // Generate new token
        log.info("Generating new access token for consumer key: {}", config.getMpConsumerKey());
        
        String credentials = config.getMpConsumerKey() + ":" + config.getMpConsumerSecret();
        String basicAuth = "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

        try {
            Response<DarajaTokenResponse> response = darajaClient.getAccessToken(basicAuth, "client_credentials").execute();
            
            if (response.isSuccessful() && response.body() != null) {
                DarajaTokenResponse tokenResponse = response.body();
                String accessToken = tokenResponse.getAccessToken();
                int expiresIn = Integer.parseInt(tokenResponse.getExpiresIn());
                
                // Cache the token
                tokenCache.put(cacheKey, new TokenCache(accessToken, expiresIn));
                
                log.info("Successfully obtained access token, expires in {} seconds", expiresIn);
                return accessToken;
            } else {
                String errorBody = response.errorBody() != null ? response.errorBody().string() : "Unknown error";
                throw new Exception("Failed to get access token: " + response.code() + " - " + errorBody);
            }
        } catch (Exception e) {
            log.error("Error getting access token: {}", e.getMessage());
            throw new Exception("Failed to get access token: " + e.getMessage());
        }
    }

    public StkPushResponse initiateSTKPush(PushSDKConfig config, String phone, String amount, String accountReference) throws Exception {
        String accessToken = getAccessToken(config);
        
        // Format phone number
        String formattedPhone = GlobalUtils.formatPhoneNumber(phone);
        
        // Generate timestamp
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        
        // Generate password: Base64.encode(Shortcode+Passkey+Timestamp)
        String passwordString = config.getMpShortCode() + config.getMpPassKey() + timestamp;
        String password = Base64.getEncoder().encodeToString(passwordString.getBytes(StandardCharsets.UTF_8));

        StkPushRequest request = StkPushRequest.builder()
                .businessShortCode(config.getMpShortCode())
                .password(password)
                .timestamp(timestamp)
                .transactionType("CustomerPayBillOnline")
                .amount(amount)
                .partyA(formattedPhone)
                .partyB(config.getMpShortCode())
                .phoneNumber(formattedPhone)
                .callBackURL(config.getMpCallbackUrl())
                .accountReference(accountReference)
                .transactionDesc("Payment for " + accountReference)
                .build();

        try {
            Response<StkPushResponse> response = darajaClient.stkPush(
                    "Bearer " + accessToken,
                    "application/json",
                    request
            ).execute();

            if (response.isSuccessful() && response.body() != null) {
                log.info("STK Push initiated successfully for phone: {}, amount: {}", formattedPhone, amount);
                return response.body();
            } else {
                String errorBody = response.errorBody() != null ? response.errorBody().string() : "Unknown error";
                throw new Exception("STK Push failed: " + response.code() + " - " + errorBody);
            }
        } catch (Exception e) {
            throw new Exception("Failed to initiate STK Push: " + e.getMessage());
        }
    }
}
