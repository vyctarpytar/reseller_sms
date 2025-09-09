package com.spa.smart_gate_springboot.pushSDK;

import com.spa.smart_gate_springboot.pushSDK.daraja.DarajaService;
import com.spa.smart_gate_springboot.pushSDK.daraja.dto.StkPushResponse;
import com.spa.smart_gate_springboot.utils.GlobalUtils;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
@Slf4j
@EnableScheduling
public class PushSDKConfigService {
    final private PushSDKConfigRepository pushSDKConfigRepository;
    private final GlobalUtils gu;
    private final DarajaService darajaService;


    public PushSDKConfig create(PushSDKConfig pushSDKConfig) {
        return pushSDKConfigRepository.save(pushSDKConfig);
    }

    public PushSDKConfig findPushSDKConfig(String shortCode) {
        return pushSDKConfigRepository.findByMpShortCode(shortCode).orElse(null);
    }


    @PostConstruct
    private void initData() {

        PushSDKConfig push = findPushSDKConfig("4037171");
        if (push != null) return;
        PushSDKConfig pushSDKConfig = PushSDKConfig.builder()
                .mpCallbackUrl("https://backend.synqafrica.co.ke:8443/api/v2/payment")
//                .mpUrl("http://smartgate.pickpay.co.ke:8484/")
                .mpPassKey("807d4b8aa4dc929a51f6a2247e268e50a636355052687d02c2f5fc2c27de23fd")
                .mpShortCode("4037171")
                .mpConsumerKey("is2CrKOqs5ioNFLUpsyAFCYBHR1uMq0g5tYfxrWElJSbcMnr")
                .mpConsumerSecret("qdDhYwrrdhnsxtNxhiwBrupZqersV8Dta8yPCxtzXBDK230PV23CZRGdarmgwFtL")
                .mpStatus("ACTIVE").build();
        create(pushSDKConfig);
    }



    public String popSDkMpesa(String phone, String amount, String accountref) throws Exception {
        PushSDKConfig pushSDKConfig = findPushSDKConfig("4037171");
        if (pushSDKConfig == null) {
            throw new Exception("Push SDK configuration not found for shortcode: 4037171");
        }
        
        try {
            StkPushResponse response = darajaService.initiateSTKPush(pushSDKConfig, phone, amount, accountref);
            
            // Convert response to JSON string for backward compatibility
            return gu.convertToJson(response);
        } catch (Exception e) {
            log.error("Failed to initiate STK Push: {}", e.getMessage());
            throw new Exception("STK Push failed: " + e.getMessage());
        }
    }


    @Scheduled(fixedRate = 5000)
    private void testResp() {
        try {
            popSDkMpesa("254716177880","1.00","test");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



}
