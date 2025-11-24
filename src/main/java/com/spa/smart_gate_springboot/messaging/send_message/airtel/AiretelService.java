package com.spa.smart_gate_springboot.messaging.send_message.airtel;

import com.spa.smart_gate_springboot.messaging.send_message.MsgMessageQueueArc;
import com.spa.smart_gate_springboot.messaging.send_message.MsgMessageQueueArcRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AiretelService {

    private final MsgMessageQueueArcRepository arcRepository;

    @Async
    public void callback(@Valid CallBackResp callBackResp) {
        MsgMessageQueueArc arc = arcRepository.findByMsgCode(callBackResp.getMessageId()).orElseThrow(() -> new RuntimeException("Message Not Found with code:  " + callBackResp.getMessageId()));
        arc.setMsgStatus(callBackResp.deliveryDescription);
        arc.setMsgClientDeliveryStatus("PENDING");
        arc.setMsgRetryCount(0);
        arcRepository.save(arc);
    }
}
