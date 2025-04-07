package com.spa.smart_gate_springboot.payment;

import com.spa.smart_gate_springboot.account_setup.invoice.InvoiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v2/payment")
public class PaymentController{
    private  final InvoiceService invoiceService;

    @PostMapping
    public ThirdPartyResponse receivePayment (@RequestBody PaymentDto payment){
        return invoiceService.receivePayment(payment);
    }
}
