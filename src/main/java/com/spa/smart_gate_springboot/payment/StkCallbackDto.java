package com.spa.smart_gate_springboot.payment;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * Safaricom STK push result callback envelope: {@code {"Body":{"stkCallback":{...}}}}.
 * Delivered to {@code /api/v2/payment} after an STK push resolves — whether the customer pays,
 * cancels the SIM prompt, or lets it time out. {@code ResultCode == 0} is success; anything else
 * is a failure whose {@code ResultDesc} explains why (e.g. 1032 = "Request cancelled by user").
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class StkCallbackDto {

    @JsonProperty("Body")
    private Body body;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Body {
        @JsonProperty("stkCallback")
        private StkCallback stkCallback;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class StkCallback {
        @JsonProperty("MerchantRequestID")
        private String merchantRequestID;
        @JsonProperty("CheckoutRequestID")
        private String checkoutRequestID;
        @JsonProperty("ResultCode")
        private Integer resultCode;
        @JsonProperty("ResultDesc")
        private String resultDesc;
        @JsonProperty("CallbackMetadata")
        private CallbackMetadata callbackMetadata;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CallbackMetadata {
        @JsonProperty("Item")
        private List<MetadataItem> item;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MetadataItem {
        @JsonProperty("Name")
        private String name;
        @JsonProperty("Value")
        private Object value;
    }

    /** True when the payload actually carries an stkCallback (vs a flat C2B confirmation). */
    public boolean isStkCallback() {
        return body != null && body.stkCallback != null && body.stkCallback.checkoutRequestID != null;
    }

    public StkCallback callback() {
        return body != null ? body.stkCallback : null;
    }

    /** Reads a named value out of CallbackMetadata.Item (e.g. "MpesaReceiptNumber", "Amount", "PhoneNumber"). */
    public Object metadata(String name) {
        if (body == null || body.stkCallback == null
                || body.stkCallback.callbackMetadata == null
                || body.stkCallback.callbackMetadata.item == null) return null;
        return body.stkCallback.callbackMetadata.item.stream()
                .filter(i -> name.equals(i.getName()))
                .map(MetadataItem::getValue)
                .filter(v -> v != null)
                .findFirst().orElse(null);
    }
}
