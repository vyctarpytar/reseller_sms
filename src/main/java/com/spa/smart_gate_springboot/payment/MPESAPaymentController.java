package com.spa.smart_gate_springboot.payment;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spa.smart_gate_springboot.MQRes.RMQPublisher;
import com.spa.smart_gate_springboot.account_setup.invoice.InvoiceService;
import com.spa.smart_gate_springboot.utils.GlobalUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v2/payment")
@Slf4j
public class MPESAPaymentController {
    private final RMQPublisher rmqPublisher;
    private final ObjectMapper objectMapper;
    private final InvoiceService invoiceService;
    private final GlobalUtils gu;

    @PostMapping
    public ResponseEntity<?> receivePayment(@RequestBody Object payment) {

        log.info("receive payment request : {}", payment);

        Map<String, String> response = new HashMap<>();

        // Two payload shapes hit this endpoint:
        //  1. STK push result callback  -> { "Body": { "stkCallback": { ResultCode, CheckoutRequestID, ... } } }
        //     (success, but also cancel / wrong-PIN / timeout — the only signal we get for a failed top-up).
        //  2. C2B confirmation          -> flat { TransID, BillRefNumber, TransAmount, ... } which settles the invoice.
        StkCallbackDto stk = objectMapper.convertValue(payment, StkCallbackDto.class);
        if (stk.isStkCallback()) {
            try {
                rmqPublisher.publishToOutQueue(payment, "MPESA_STK_CALLBACK");
                invoiceService.handleStkCallback(stk);
            } catch (Exception e) {
                log.error("Failed to process STK callback : {}", e.getMessage());
            }
            // Always ack the callback so Safaricom/the gateway stops retrying.
            response.put("ResultCode", "0");
            response.put("ResultDesc", "Accepted");
            return ResponseEntity.ok(response);
        }

        PaymentDto paymentDto = objectMapper.convertValue(payment, PaymentDto.class);
        try {

            rmqPublisher.publishToOutQueue(payment, "MPESA_C2B_TRANSACTION_RECEIVE");

            invoiceService.receivePayment(paymentDto);
            response.put("ResultCode", "0");
            response.put("ResultDesc", "Accepted");
        } catch (Exception e) {
            log.error("Failed to receive payment : {}", e.getMessage());
            response.put("ResultCode", "C2B00016");
            response.put("ResultDesc", "Rejected");
        }


        return ResponseEntity.ok(response);
    }

    @PostMapping("/validate")
    public ResponseEntity<?> receivePaymentValidate(@RequestBody Object obj) {
        log.info("receive Validate payment request : {}", obj);
        try {
            log.info("receive Validate payment request : {}", objectMapper.writeValueAsString(obj));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        rmqPublisher.publishToOutQueue(obj, "MPESA_C2B_TRANSACTION_VALIDATE");
        Map<String, String> response = new HashMap<>();
        response.put("ResultCode", "0");
        response.put("ResultDesc", "Accepted");
        return ResponseEntity.ok(response);
    }
}
