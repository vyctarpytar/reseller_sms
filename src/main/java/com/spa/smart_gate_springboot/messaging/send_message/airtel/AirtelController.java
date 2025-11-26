package com.spa.smart_gate_springboot.messaging.send_message.airtel;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v2/public")
@RequiredArgsConstructor
@Slf4j
public class AirtelController {

    private final AiretelService airetelService;

    @PostMapping("/switch-port/callback")
    public void callback(@RequestBody @Valid CallBackResp callBackResp) {
        log.info("Airtel Callback received: {}", callBackResp);
        airetelService.callback(callBackResp);
    }

}
