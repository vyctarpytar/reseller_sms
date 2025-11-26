package com.spa.smart_gate_springboot.messaging.send_message.airtel;

import com.spa.smart_gate_springboot.messaging.send_message.MsgMessageQueueArc;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v2/public")
@RequiredArgsConstructor
@Slf4j
public class AirtelController {

    private final AiretelService airetelService;

    @PostMapping("/switch-port/callback")
    public Object callback(@RequestBody @Valid CallBackResp callBackResp) {
        log.info("Airtel Callback received: {}", callBackResp);
        MsgMessageQueueArc msgQueue =  airetelService.callback(callBackResp);

        Map<String, Object> respData = new HashMap<>();
        respData.put("messageId", msgQueue.getMsgExternalId());
        respData.put("message", msgQueue.getMsgMessage());
        respData.put("senderId", msgQueue.getMsgSenderIdName());
        respData.put("mobileNo", msgQueue.getMsgSubMobileNo());
        respData.put("msgStatus", msgQueue.getMsgStatus());
        respData.put("errorDesc", null);

        respData.put("status", 200);
        respData.put("success", true);

        return respData;
    }

}
