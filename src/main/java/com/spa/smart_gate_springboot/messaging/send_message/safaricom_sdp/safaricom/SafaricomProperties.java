package com.spa.smart_gate_springboot.messaging.send_message.safaricom_sdp.safaricom;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "safaricom")
public class SafaricomProperties {
    private String safUserName; // send messages
    private String safApiUserName; // get Token
    private String safPassword;
    private String safBaseUrl;
    private String safPromotionalPackageId;
    private String safTransactionalPackageId;
    private String safCpId;
    private String safResponseUrl;

}

