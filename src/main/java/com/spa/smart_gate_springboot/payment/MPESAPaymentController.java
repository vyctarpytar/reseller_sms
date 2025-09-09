package com.spa.smart_gate_springboot.payment;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spa.smart_gate_springboot.MQRes.RMQPublisher;
import com.spa.smart_gate_springboot.account_setup.invoice.InvoiceService;
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
    private  final RMQPublisher rmqPublisher;
    private final ObjectMapper objectMapper;

    private  final InvoiceService invoiceService;

    @PostMapping
    public ResponseEntity<?> receivePayment (@RequestBody PaymentDto payment){
        Map<String,String> response = new HashMap<>();
        try{
            invoiceService.receivePayment(payment);
            response.put("ResultCode", "0");
            response.put("ResultDesc", "Accepted");
        }catch (Exception e){
            log.error("Failed to receive payment : {}", e.getMessage());
            response.put("ResultCode", "C2B00016");
            response.put("ResultDesc", "Rejected");
        }


        return ResponseEntity.ok(response);
    }

    @PostMapping("/validate")
    public ResponseEntity<?> receivePaymentValidate (@RequestBody  Object obj){
        log.info("receive Validate payment request : {}", obj);
        try {
            log.info("receive Validate payment request : {}", objectMapper.writeValueAsString(obj));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        rmqPublisher.publishToOutQueue(obj, "MPESA_C2B_TRANSACTION_VALIDATE");
        Map<String,String> response = new HashMap<>();
        response.put("ResultCode", "0");
        response.put("ResultDesc", "Accepted");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/confirmation")
    public ResponseEntity<?> receivePaymentConfirmation (@RequestBody  Object obj){
        log.info("receive Confirmation payment request : {}", obj);
        try {
            log.info("receive Confirmation payment request : {}", objectMapper.writeValueAsString(obj));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        rmqPublisher.publishToOutQueue(obj, "MPESA_C2B_TRANSACTION_RECEIVE");
        Map<String,String> response = new HashMap<>();
        response.put("ResultCode", "0");
        response.put("ResultDesc", "Accepted");
        return ResponseEntity.ok(response);
    }
}
