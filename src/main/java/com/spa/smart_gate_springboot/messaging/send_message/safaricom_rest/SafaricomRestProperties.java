package com.spa.smart_gate_springboot.messaging.send_message.safaricom_rest;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "safaricom.rest")
public class SafaricomRestProperties {
    private String baseUrl;
    private String username;
    private String password;
    private String senderUserName;
    private String responseUrl;
    private String promotionalPackageId;
    private String transactionalPackageId;
}
