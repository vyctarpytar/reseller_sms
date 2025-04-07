package com.spa.smart_gate_springboot.payment;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentService {
    private final PaymentRepository paymentRepository;

    public Payment save(Payment payment) {
       return  paymentRepository.saveAndFlush(payment);
    }
}


