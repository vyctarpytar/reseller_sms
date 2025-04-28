package com.spa.smart_gate_springboot.utils;


import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
@Slf4j
public class SmartGate {

    public void sendSMSOld(String msisdn, String message) {
        new Thread(() -> {

            try {

//                String sender = "Ndovu Pay";
//                String sender = "weiserstamm".toUpperCase(Locale.ROOT);


                String username = "smartgate_user";
                String password = "I82Y65xLi-FET$viT3NJ-Ljn0FM-zdU8A$@";
                String urlEndpoint = "https://synq.weiserstamm.com/api/PushSMS?mobile=";
                String shCode = "DoNotReply";

//
//                String SEND_SMS_URL = "https://click.weiserstamm.com/api/PushSMS?mobile=" + msisdn + "&shortcode="
//                        + sender + "&username=spaotp_api_user&password=65RopdFIS-H47lNv4kg@-Lw5nrS-m5d5enP&message="+message;

                String SEND_SMS_URL = urlEndpoint + URLEncoder.encode(msisdn, StandardCharsets.UTF_8)
                        + "&shortcode=" + URLEncoder.encode(shCode, StandardCharsets.UTF_8)
                        + "&username=" + URLEncoder.encode(username, StandardCharsets.UTF_8)
                        + "&password=" + URLEncoder.encode(password, StandardCharsets.UTF_8)
                        + "&message=" + message;


                URL url = new URL(SEND_SMS_URL);
                URI uri = new URI(url.getProtocol(), url.getUserInfo(), url.getHost(), url.getPort(), url.getPath(),
                        url.getQuery(), url.getRef());

                System.out.println(url);
                CloseableHttpClient client = HttpClientBuilder.create().build();
                HttpPost post = new HttpPost(uri);

                CloseableHttpResponse response = client.execute(post);


                InputStream in = response.getEntity().getContent();
                String encoding = "UTF-8";
                String body = IOUtils.toString(in, encoding);
                System.out.println("--BODY" + body + "---END");

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void sendSMS(String msisdn, String message) {
        message = message.replaceAll("[^*A-Za-z0-9@$_\\/.,\"():;\\-=+&%#!?<>' \\n]", "");
        String username = "smartgate_user";
        String password = "I82Y65xLi-FET$viT3NJ-Ljn0FM-zdU8A$@";
        String urlEndpoint = "https://synq.weiserstamm.com/api/PushSMS?mobile=";
        String shCode = "SYNQSMS";

        String finalMessage = message;
        new Thread(() -> {

            try {


//
//                String SEND_SMS_URL = "https://click.weiserstamm.com/api/PushSMS?mobile=" + msisdn + "&shortcode="
//                        + sender + "&username=spaotp_api_user&password=65RopdFIS-H47lNv4kg@-Lw5nrS-m5d5enP&message="+message;

                String SEND_SMS_URL = urlEndpoint + URLEncoder.encode(msisdn, StandardCharsets.UTF_8)
                        + "&shortcode=" + URLEncoder.encode(shCode, StandardCharsets.UTF_8)
                        + "&username=" + URLEncoder.encode(username, StandardCharsets.UTF_8)
                        + "&password=" + URLEncoder.encode(password, StandardCharsets.UTF_8)
                        + "&message=" + URLEncoder.encode(finalMessage, StandardCharsets.UTF_8);


                log.info("Send SMS to : {}", SEND_SMS_URL);

                CloseableHttpClient client = HttpClientBuilder.create().build();
                HttpPost post = new HttpPost(SEND_SMS_URL);
                CloseableHttpResponse response = client.execute(post);

                InputStream in = response.getEntity().getContent();
                String encoding = "UTF-8";
                String body = IOUtils.toString(in, encoding);
                log.warn("Response body : {}", body);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }


//    @PostConstruct
//    public void init() {
//        sendSMS("254716177880", "Hello , vyctar  see @Your password is x vsvsvsvs");
//    }


}
