package com.spa.smart_gate_springboot.account_setup.majibyte.sms;


import com.spa.smart_gate_springboot.utils.StandardJsonResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v2/public/")
@RequiredArgsConstructor
public class SmsTopUpController {


    private final SmsTopUpService smsTopUpService;


    @PostMapping("mabibyte-topup-sms")
    public StandardJsonResponse createSMSInvoice(@RequestBody @Valid TopUpDto credit) {
        return smsTopUpService.majiByteLoadAccount(credit);
    }

}


