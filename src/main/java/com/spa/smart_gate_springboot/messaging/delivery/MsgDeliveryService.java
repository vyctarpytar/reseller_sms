package com.spa.smart_gate_springboot.messaging.delivery;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.spa.smart_gate_springboot.messaging.send_message.MsgMessageQueueArc;
import com.spa.smart_gate_springboot.utils.GlobalUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MsgDeliveryService {

    private final MsgDeliveryRepository msgDeliveryRepository;
    private final GlobalUtils globalUtils;


    @Transactional
    public void saveMsgDelivery(MsgMessageQueueArc msgMessageQueueArc) {
        MsgDelivery msgDelivery = MsgDelivery.builder().msgdDate(LocalDate.now())
                .msgdDelCode(msgMessageQueueArc.getMsgCode()).
                msgdMsgId(msgMessageQueueArc.getMsgId())
                .msgdStatus("PENDING")
                .msgdLogs("PENDING")
                .msgdIsRetry("FIRST")
                .msgdPrsp(msgMessageQueueArc.getMsgSenderLevel())
                .msgdTimestamp(LocalDateTime.now()).build();
        msgDeliveryRepository.saveAndFlush(msgDelivery);
    }

    public MsgDelivery findMsgDeliveryByCode(String code) {
        List<MsgDelivery> byMsgdDelCode = msgDeliveryRepository.findByMsgdDelCode(code);
        if (byMsgdDelCode.isEmpty()) { return null;}
        if (byMsgdDelCode.size() > 1) { return byMsgdDelCode.get(0); }
        return byMsgdDelCode.get(0);

    }

    public MsgDelivery save(MsgDelivery msgDelivery) {
        return msgDeliveryRepository.saveAndFlush(msgDelivery);
    }


    public List<MsgDelivery> reconDeliveryNotes() {
return msgDeliveryRepository.reconDeliveryNotes();
    }
}
