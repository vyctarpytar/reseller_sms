package com.spa.smart_gate_springboot.pushSDK;

import com.spa.smart_gate_springboot.utils.GlobalUtils;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
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
public class PushSDKConfigService {
    final private PushSDKConfigRepository pushSDKConfigRepository;
    private final GlobalUtils gu;


    public PushSDKConfig create(PushSDKConfig pushSDKConfig) {
        return pushSDKConfigRepository.save(pushSDKConfig);
    }

    public PushSDKConfig findPushSDKConfig(String shortCode) {
        return pushSDKConfigRepository.findByMpShortCode(shortCode).orElse(null);
    }


//    @PostConstruct
    private void initData() {

        PushSDKConfig push = findPushSDKConfig("4095123");
        if (push != null) return;
        PushSDKConfig pushSDKConfig = PushSDKConfig.builder().mpCallbackUrl("https://smartgate.pickpay.co.ke/usr/processMpesaCallback.action").mpUrl("http://smartgate.pickpay.co.ke:8484/").mpPassKey("4b06eddf3fbc5ec3e71e9b0e338dfa524e0f51e7d22d2cafbddc528cadd63488").mpShortCode("4095123").mpConsumerKey("Aci0mcTNUxawothBGbQBm25WU8KPR3NT").mpConsumerSecret("GFyB6X3TVbPCA12o").mpStatus("ACTIVE").build();
        create(pushSDKConfig);
    }


    public String popSDkMpesa(String phone, String amount, String accountref) throws Exception {
        PushSDKConfig pushSDKConfig = findPushSDKConfig("4095123");
        phone = GlobalUtils.formatPhoneNumber(phone);
        String data = "Amount=" + URLEncoder.encode(amount, StandardCharsets.UTF_8) +
                      "&PartyA=" + URLEncoder.encode(phone, StandardCharsets.UTF_8) +
                      "&AccountReference=" + URLEncoder.encode(accountref, StandardCharsets.UTF_8) +
                      "&BusinessShortCode=" + URLEncoder.encode(pushSDKConfig.getMpShortCode(), StandardCharsets.UTF_8) +
                      "&LipaNaMpesaPasskey=" + URLEncoder.encode(pushSDKConfig.getMpPassKey(), StandardCharsets.UTF_8) +
                      "&TransactionType=" + URLEncoder.encode("CustomerPayBillOnline", StandardCharsets.UTF_8) +
                      "&CallBackURL=" + URLEncoder.encode("https://smartgate.pickpay.co.ke/pick/processMpesaCallback.action", StandardCharsets.UTF_8) +
                      "&PartyB=" + URLEncoder.encode(pushSDKConfig.getMpShortCode(), StandardCharsets.UTF_8) +
                      "&TransactionDesc=" + URLEncoder.encode("bill", StandardCharsets.UTF_8) +
                      "&Remark=" + URLEncoder.encode("ok", StandardCharsets.UTF_8) +
                      "&PhoneNumber=" + URLEncoder.encode(phone, StandardCharsets.UTF_8) +
                      "&initiateStkPush=" + URLEncoder.encode(phone, StandardCharsets.UTF_8) +
                      "&consumer_key=" + URLEncoder.encode(pushSDKConfig.getMpConsumerKey(), StandardCharsets.UTF_8) +
                      "&consumer_secret=" + URLEncoder.encode(pushSDKConfig.getMpConsumerSecret(), StandardCharsets.UTF_8) +
                      "&sp_paybill_no=" + URLEncoder.encode(pushSDKConfig.getMpShortCode(), StandardCharsets.UTF_8);

        String requestURL = "http://smartgate.pickpay.co.ke:8484/";
       return sendHttpPostRequest(requestURL, data);
    }


    public String sendHttpPostRequest(String requestURL, String data) throws Exception {
        URL url = new URL(requestURL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Length", String.valueOf(data.getBytes().length));
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");
        connection.setUseCaches(false);
        connection.setDoInput(true);
        connection.setDoOutput(true);

        try (DataOutputStream output = new DataOutputStream(connection.getOutputStream())) {
            output.writeBytes(data);
        }

        StringBuilder response = new StringBuilder();
        try (BufferedReader input = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            String inputLine;
            while ((inputLine = input.readLine()) != null) {
                response.append(inputLine).append("\n");
            }
        }
        return response.toString();
    }



}
