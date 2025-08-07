package com.spa.smart_gate_springboot.payment;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spa.smart_gate_springboot.MQRes.RMQPublisher;
import com.spa.smart_gate_springboot.account_setup.invoice.InvoiceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v2/payment")
@Slf4j
public class MPESAPaymentController {
    private  final RMQPublisher rmqPublisher;
    private final ObjectMapper objectMapper;

    @PostMapping("/validate")
    public ThirdPartyResponse receivePaymentValidate (@RequestBody  Object obj){
        log.info("receive Validate payment request : {}", obj);
        try {
            log.info("receive Validate payment request : {}", objectMapper.writeValueAsString(obj));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        rmqPublisher.publishToOutQueue(obj, "MPESA_C2B_TRANSACTION_VALIDATE");
        ThirdPartyResponse response = new ThirdPartyResponse();
        response.setResultCode("200");
        response.setResultDesc("success");
        return response;
    }

    @PostMapping("/confirmation")
    public ThirdPartyResponse receivePaymentConfirmation (@RequestBody  Object obj){
        log.info("receive Confirmation payment request : {}", obj);
        try {
            log.info("receive Confirmation payment request : {}", objectMapper.writeValueAsString(obj));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        rmqPublisher.publishToOutQueue(obj, "MPESA_C2B_TRANSACTION_RECEIVE");
        ThirdPartyResponse response = new ThirdPartyResponse();
        response.setResultCode("200");
        response.setResultDesc("success");
        return response;
    }
}
