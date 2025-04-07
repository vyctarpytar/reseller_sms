package com.spa.smart_gate_springboot.messaging.send_message.api;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Calendar;
import java.util.Date;
import java.util.UUID;


@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(schema = "js_core")
@Entity(name = "api_key")
public class ApiKey {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, unique = true, length = 100)
    private String apiKey;

    @Column(nullable = false)
    private String clientName;

    @Column(nullable = false, unique = true)
    private UUID apiAccId;

    @Column(nullable = false)
    private UUID apiResellerId;

    @Column(nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdDate;

    @Column
    @Temporal(TemporalType.TIMESTAMP)
    private Date expirationDate;

    @Column(nullable = false)
    private Boolean active;
    @Column(length = 1000)
    private String apiEndPoint;
    private String apiKeyTag;
    @Column(length = 2000)
    private String apiPayload;

    @Column(length = 2000)
    private String apiPayloadMultiple;

    @Column(length = 3000)
    private String apiResponse;


    @PrePersist
    protected void onCreate() {
        if (this.createdDate == null) {
            this.createdDate = new Date();
        }
        // Set expiration date to 1 year from the creation date
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(this.createdDate);
        calendar.add(Calendar.YEAR, 1);
        this.expirationDate = calendar.getTime();
        this.active = true;
        this.apiEndPoint = "https://backend.smartgate.co.ke/api/v2/sandbox/single-sms";
        this.apiKeyTag = "X-API-KEY";
        this.apiPayload = "curl --request POST \\\n" +
                "  --url "+this.apiEndPoint+" \\\n" +
                "  --header 'Content-Type: application/json' \\\n" +
                "  --header 'X-API-KEY: "+this.apiKey+"' \\\n" +
                "  --data '{\n" +
                "  \"msgExternalId\": 1,\n" +
                "  \"msgMobileNo\": \"254716177880\",\n" +
                "  \"msgMessage\": \"Test Api Message Dukapay\",\n" +
                "  \"msgSenderId\": \"DoNotReply\"\n" +
                "}'";
        this.apiResponse = "{\n" +
                "\t\"success\": true,\n" +
                "\t\"messages\": {\n" +
                "\t\t\"message\": \"Messages Sent Successfully\"\n" +
                "\t},\n" +
                "\t\"data\": {\n" +
                "\t},\n" +
                "\t\"total\": 0,\n" +
                "\t\"targetUrl\": null,\n" +
                "\t\"token\": null,\n" +
                "\t\"status\": 200\n" +
                "}";
    }
}
