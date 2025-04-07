package com.spa.smart_gate_springboot.messaging.sender;

import com.spa.smart_gate_springboot.messaging.send_message.MsgMessageQueueArc;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

@RequiredArgsConstructor
@Service
public class MsgSenderService {

    private final MsgSenderRepository msgSenderRepository;

    public MsgSender saveMsgSender(MsgMessageQueueArc msgMessageQueueArc) {
        MsgSender record = MsgSender.builder().msgSender(msgMessageQueueArc.getMsgSenderLevel()).msgMsgId(msgMessageQueueArc.getMsgId()).msgDesc(msgMessageQueueArc.getMsgErrorCode()).msgWhyResent(msgMessageQueueArc.getMsgWhyResent()).msgDelStatus(msgMessageQueueArc.getMsgStatus()).msgTime(LocalDateTime.now()).msgDate(LocalDate.now()).build();
        return msgSenderRepository.saveAndFlush(record);
    }
}
