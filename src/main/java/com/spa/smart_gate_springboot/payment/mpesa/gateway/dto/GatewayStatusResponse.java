package com.spa.smart_gate_springboot.payment.mpesa.gateway.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.Map;

/**
 * Response of the Waretech gateway {@code POST /mpesa/v2/get-transaction-status?conversationId=...}.
 * For B2C, {@code transactionId} carries the M-Pesa receipt and {@code resultCode == 0} means success.
 * A 404 from the gateway (transaction not yet recorded) surfaces as a Retrofit non-2xx, not this body.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GatewayStatusResponse {
    private String transactionType;
    private String conversationId;
    private String originatorConversationId;
    private String transactionId;       // M-Pesa receipt for B2C
    private Integer resultCode;          // 0 = success; non-null & non-zero = terminal failure
    private String resultDesc;
    private String processingStatus;
    private Boolean processed;
    private Map<String, Object> details;

    /** Terminal success once the gateway has a result code of 0. */
    public boolean isSuccess() {
        return resultCode != null && resultCode == 0;
    }

    /** Terminal failure once the gateway has a non-zero result code. */
    public boolean isFailed() {
        return resultCode != null && resultCode != 0;
    }

    public String resolveReceipt() {
        if (transactionId != null && !transactionId.isBlank()) return transactionId;
        if (details != null && details.get("transactionReceipt") != null) {
            return String.valueOf(details.get("transactionReceipt"));
        }
        return null;
    }
}
