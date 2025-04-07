package com.spa.smart_gate_springboot.pushSDK;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Data
@AllArgsConstructor
@Builder
@NoArgsConstructor
@Table(schema = "js_core", name = "mpesa_push_config")
public class PushSDKConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
   private UUID mpId;
    private String mpPassKey;
    private String mpShortCode;
    private String mpCallbackUrl;
    private String mpConsumerKey;
    private String mpConsumerSecret;
    private String mpStatus;
    private String mpUrl;
}
