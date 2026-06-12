package com.spa.smart_gate_springboot.payment.mpesa.gateway;

import com.spa.smart_gate_springboot.payment.mpesa.gateway.dto.*;
import com.spa.smart_gate_springboot.utils.GlobalUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import retrofit2.Response;

import java.math.BigDecimal;

/**
 * Thin service over the Waretech gateway for STK collection (money in) and B2C payout (money out).
 * Replaces the previous direct-Daraja STK path.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WaretechMpesaService {

    private final WaretechInterface gateway;

    /** Collection shortcode used for STK push. */
    @Value("${mpesa.gateway.shortcode:4037171}")
    private String stkShortcode;

    /** Paybill/shortcode used for B2C disbursement. */
    @Value("${mpesa.gateway.b2cPaybill:4037171}")
    private String b2cPaybill;

    /**
     * Initiate an STK push. {@code accountReference} is the invoice code Safaricom echoes back in the
     * C2B confirmation, which {@code InvoiceService.receivePayment} uses to settle.
     */
    public GatewayStkResponse initiateStk(String phone, BigDecimal amount, String accountReference,
                                          String description) throws Exception {
        GatewayStkRequest req = GatewayStkRequest.builder()
                .shortcode(stkShortcode)
                .phoneNumber(GlobalUtils.formatPhoneNumber(phone))
                .amount(amount)
                .accountReference(accountReference)
                .description(description != null ? description : "Payment for " + accountReference)
                .build();

        Response<GatewayStkResponse> response = gateway.stkPush(req).execute();
        if (response.isSuccessful() && response.body() != null) {
            GatewayStkResponse body = response.body();
            log.info("STK push to {} amount {} ref {} -> code {} ({})", req.getPhoneNumber(), amount,
                    accountReference, body.getResponseCode(), body.getCheckoutRequestID());
            return body;
        }
        String err = response.errorBody() != null ? response.errorBody().string() : "unknown";
        throw new Exception("Gateway STK push failed: HTTP " + response.code() + " - " + err);
    }

    /**
     * Initiate a B2C payout. {@code amount} is the NET payout (gross minus M-Pesa charge).
     */
    public GatewayB2cResponse initiateB2c(String phone, BigDecimal amount, String remarks) throws Exception {
        GatewayB2cRequest req = GatewayB2cRequest.builder()
                .paybill(b2cPaybill)
                .phoneNumber(GlobalUtils.formatPhoneNumber(phone))
                .amount(amount)
                .commandId("BusinessPayment")
                .remarks(remarks != null ? remarks : "Withdrawal")
                .occasion(remarks != null ? remarks : "Withdrawal")
                .build();

        Response<GatewayB2cResponse> response = gateway.b2c(req).execute();
        if (response.isSuccessful() && response.body() != null) {
            GatewayB2cResponse body = response.body();
            log.info("B2C to {} amount {} -> code {} ({})", req.getPhoneNumber(), amount,
                    body.getResponseCode(), body.getConversationId());
            return body;
        }
        String err = response.errorBody() != null ? response.errorBody().string() : "unknown";
        throw new Exception("Gateway B2C failed: HTTP " + response.code() + " - " + err);
    }

    /**
     * Poll the gateway for a B2C result. Returns null if the transaction is not yet recorded
     * (gateway 404) or the call fails — caller should treat null as "still pending".
     */
    public GatewayStatusResponse getStatus(String conversationId) {
        try {
            Response<GatewayStatusResponse> response = gateway.getTransactionStatus(conversationId).execute();
            if (response.isSuccessful()) {
                return response.body();
            }
            log.debug("Status poll for {} -> HTTP {}", conversationId, response.code());
            return null;
        } catch (Exception e) {
            log.warn("Status poll error for {}: {}", conversationId, e.getMessage());
            return null;
        }
    }

    /**
     * Query the live Safaricom account balance for the B2C paybill (working + utility float).
     * Returns null if the gateway call fails — callers should treat that as "balance unavailable".
     */
    public GatewayBalanceResponse queryBalance() {
        try {
            GatewayBalanceRequest req = GatewayBalanceRequest.builder().paybill(b2cPaybill).build();
            Response<GatewayBalanceResponse> response = gateway.balanceInquiry(req).execute();
            if (response.isSuccessful() && response.body() != null) {
                GatewayBalanceResponse body = response.body();
                log.info("Balance inquiry paybill {} -> working={}, utility={}", b2cPaybill,
                        body.getWorkingBalance(), body.getUtilityBalance());
                return body;
            }
            log.warn("Balance inquiry failed for paybill {}: HTTP {}", b2cPaybill, response.code());
            return null;
        } catch (Exception e) {
            log.error("Balance inquiry error for paybill {}: {}", b2cPaybill, e.getMessage(), e);
            return null;
        }
    }
}
