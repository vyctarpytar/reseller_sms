package com.spa.smart_gate_springboot.pushSDK;

import com.spa.smart_gate_springboot.payment.mpesa.gateway.WaretechMpesaService;
import com.spa.smart_gate_springboot.payment.mpesa.gateway.dto.GatewayStkResponse;
import com.spa.smart_gate_springboot.utils.GlobalUtils;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
@EnableScheduling
public class PushSDKConfigService {
    final private PushSDKConfigRepository pushSDKConfigRepository;
    private final GlobalUtils gu;
    private final WaretechMpesaService waretechMpesaService;


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
                .mpCallbackUrl("https://backend.synqafrica.co.ke/api/v2/payment")
                .mpPassKey("807d4b8aa4dc929a51f6a2247e268e50a636355052687d02c2f5fc2c27de23fd")
                .mpShortCode("4037171")
                .mpConsumerKey("is2CrKOqs5ioNFLUpsyAFCYBHR1uMq0g5tYfxrWElJSbcMnr")
                .mpConsumerSecret("qdDhYwrrdhnsxtNxhiwBrupZqersV8Dta8yPCxtzXBDK230PV23CZRGdarmgwFtL")
                .mpStatus("ACTIVE").build();
        create(pushSDKConfig);
    }


    /**
     * Launch an STK collection prompt via the Waretech gateway. Signature kept (String amount) so
     * existing callers in InvoiceService are unchanged; the C2B confirmation still settles at
     * /api/v2/payment using {@code accountref} (the invoice code).
     */
    public String popSDkMpesa(String phone, String amount, String accountref) throws Exception {
        try {
            GatewayStkResponse response = waretechMpesaService.initiateStk(
                    phone, new BigDecimal(amount), accountref, "Payment for " + accountref);

            if (response == null || !response.isAccepted()) {
                throw new Exception("STK push not accepted by gateway: "
                        + (response != null ? response.getResponseDescription() : "no response"));
            }
            // Convert response to JSON string (kept for parity / logging).
            gu.convertToJson(response);
            // CheckoutRequestID lets us match the stkCallback (success / cancel / failure) back to the invoice.
            return response.getCheckoutRequestID();
        } catch (Exception e) {
            throw new Exception("STK Push failed: " + e.getMessage());
        }
    }


}
