package com.spa.smart_gate_springboot.messaging.send_message;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.spa.smart_gate_springboot.MQRes.MQConfig;
import com.spa.smart_gate_springboot.MQRes.RMQPublisher;
import com.spa.smart_gate_springboot.account_setup.account.Account;
import com.spa.smart_gate_springboot.account_setup.account.AccountService;
import com.spa.smart_gate_springboot.account_setup.reseller.Reseller;
import com.spa.smart_gate_springboot.account_setup.reseller.ResellerService;
import com.spa.smart_gate_springboot.messaging.operatorPrefix.OperatorPrefix;
import com.spa.smart_gate_springboot.messaging.operatorPrefix.OperatorPrefixService;
import com.spa.smart_gate_springboot.messaging.send_message.safaricom_sdp.SafBulkService;
import com.spa.smart_gate_springboot.user.UserService;
import com.spa.smart_gate_springboot.utils.GlobalUtils;
import com.spa.smart_gate_springboot.utils.UniqueCodeGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.util.TextUtils;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.DuplicateFormatFlagsException;
import java.util.UUID;
import java.util.concurrent.RejectedExecutionException;

@Component
@RequiredArgsConstructor
@EnableRabbit
@Slf4j
public class MQReceiverSynq {
    private final AccountService accountService;
    private final OperatorPrefixService operatorPrefixService;
    private final RMQPublisher rmqPublisher;
    private final MsgMessageQueueArcRepository msgQueueRepository;
    private final ResellerService resellerService;
    private final ThreadPoolTaskExecutor taskExecutor;
    private final ObjectMapper objectMapper;
    private final UserService userService;
    private final GlobalUtils gu;
    private final SafBulkService safBulkService;
    private final MsgMessageQueueArcRepository msgMessageQueueArcRepository;

    private static String getMobilePrefix(String phone) {
        String s = phone.substring(3, 6).replaceAll("[^a-zA-Z0-9\\s+]", "");
        s = s.replaceAll("\\.", "");
        return s;
    }

    @RabbitListener(queues = {MQConfig.SYNQ_QUEUE,MQConfig.QUEUE}, containerFactory = "rabbitListenerContainerFactory")
    public void consumeMessage(Channel channel, Message message) {

        try {
            taskExecutor.execute(() -> receiver(message, channel));
        } catch (RejectedExecutionException e) {
            new Thread(() -> receiver(message, channel)).start(); // Execute in the caller's thread as a last resort
        }

    }




    @RabbitListener(queues = MQConfig.OUT_OF_CREDIT_QUEUE_SYNQ, containerFactory = "rabbitListenerContainerFactory")
    public void synqHandleOutOfCredit(Channel channel, Message message) {

        try {
            taskExecutor.execute(() ->{
                byte[] payload = message.getBody();
                MsgQueue   msgQueue = null;
                try {
                    msgQueue = objectMapper.readValue(payload, MsgQueue.class);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                try {
                    channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                handleOutOfCredit(msgQueue);

            });
        } catch (RejectedExecutionException e) {
            new Thread(() -> receiver(message, channel)).start(); // Execute in the caller's thread as a last resort
        }

    }
//
//    @RabbitListener(queues = MQConfig.ERROR_QUEUE)
//    public void receiverErrorQueue(MsgQueue msgQueue) {
//        receiver(msgQueue);
//    }


    public void handleOutOfCredit(MsgQueue msgQueue) {
        MsgMessageQueueArc arcQueue = new MsgMessageQueueArc();
        BeanUtils.copyProperties(msgQueue, arcQueue);
        arcQueue.setMsgExternalId(msgQueue.getMsgExternalId());
        arcQueue.setMsgId(null);
        arcQueue.setMsgClientDeliveryStatus("PENDING");
        msgQueueRepository.saveAndFlush(arcQueue);
    }


    private void receiver(Message message, Channel channel) {
        MsgQueue msgQueue = null;
        try {
            byte[] payload = message.getBody();
            msgQueue = objectMapper.readValue(payload, MsgQueue.class);


            if (TextUtils.isEmpty(msgQueue.getMsgMessage())) {
                if (channel.isOpen()) channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
                return;
            }
            if (TextUtils.isEmpty(msgQueue.getMsgSubMobileNo())) {
                if (channel.isOpen()) channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
                return;
            }
            msgQueue.setMsgExternalId(String.valueOf(msgQueue.getMsgExternalId()));
            msgQueue.setMsgStatus("PENDING_PROCESSING");
            msgQueue.setMsgClientDeliveryStatus("PENDING");
            Account acc = accountService.findByAccId(msgQueue.getMsgAccId());


            //todo handle transactional to not use stop
            if (!msgQueue.getMsgSenderId().contains("WasteCo_Ltd") && !msgQueue.getMsgSenderId().equalsIgnoreCase("WARETECH")){
                if(!msgQueue.getMsgMessage().contains("STOP*")) {
                    log.info("add stop to message : {}",msgQueue.getMsgSenderId());
                    msgQueue.setMsgMessage(msgQueue.getMsgMessage() + "\nSTOP*456*9*5#");
                }
            }
            msgQueue.setMsgAccName(acc.getAccName());

            int no_of_msg = getNoOfMessage(msgQueue);

            BigDecimal cost_per_sms = getCostPerSMS(msgQueue.getMsgAccId());
            BigDecimal totalCost = cost_per_sms.multiply(new BigDecimal(no_of_msg));
            msgQueue.setMsgPage(no_of_msg);
            msgQueue.setMsgCostId(totalCost);

            Reseller reseller = resellerService.findById(acc.getAccResellerId());
            msgQueue.setMsgResellerId(reseller.getRsId());
            msgQueue.setMsgResellerName(reseller.getRsCompanyName());

            var rsMsgBal = reseller.getRsAllocatableUnit();

            if (rsMsgBal.compareTo(BigDecimal.TEN) < 1) {
                msgQueue.setMsgStatus("RS_CREDIT_ISSUE");
                rmqPublisher.publishToErrorQueue(msgQueue, MQConfig.OUT_OF_CREDIT_QUEUE_SYNQ);
            } else if (acc.getAccMsgBal().compareTo(BigDecimal.TEN) < 1) {
                msgQueue.setMsgStatus("PENDING_CREDIT");
                rmqPublisher.publishToErrorQueue(msgQueue, MQConfig.OUT_OF_CREDIT_QUEUE_SYNQ);
            } else {
                msgQueue.setMsgStatus("PENDING_PROCESSING");

                MsgMessageQueueArc msgMessageQueueArc = new MsgMessageQueueArc();
                BeanUtils.copyProperties(msgQueue, msgMessageQueueArc, gu.getNullPropertyNames(msgQueue));

                String msgId = msgQueue.getMsgExternalId();
                msgMessageQueueArc.setMsgExternalId(msgId);
                msgMessageQueueArc.setMsgRetryCount(0);

                msgMessageQueueArc.setMsgClientDeliveryStatus("PENDING");
                msgMessageQueueArc.setMsgSenderIdName(msgQueue.getMsgSenderId());
                UUID rsId = accountService.findByAccId(msgQueue.getMsgAccId()).getAccResellerId();

                msgMessageQueueArc.setMsgResellerId(rsId);
                msgMessageQueueArc.setMsgResellerName(reseller.getRsCompanyName());
                if(msgMessageQueueArc.getMsgCreatedBy() != null){
                    try {
                        msgMessageQueueArc.setMsgCreatedByEmail(userService.findById(msgMessageQueueArc.getMsgCreatedBy()).getEmail());
                    }catch (Exception e){
                        log.error("Error while creating email address : {}", e.getLocalizedMessage());
                    }
                }
                msgMessageQueueArc.setMsgSenderLevel("WEISER");
                UniqueCodeGenerator ug = new UniqueCodeGenerator();
                msgMessageQueueArc.setMsgCode(ug.generateSecureApiKey());
                try {
                    msgMessageQueueArcRepository.save(msgMessageQueueArc);
                    safBulkService.sendArcSms(msgMessageQueueArc);
                } catch (DataIntegrityViolationException die) {
                    log.error("Dublicate violation : {}",die.getLocalizedMessage());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

            }

            if (channel.isOpen()) {
                channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
            }

        } catch (Exception em) {
            em.printStackTrace();
            try {
                if (channel.isOpen()) {
                    channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
                }
            } catch (Exception eb) {
                eb.printStackTrace();
            }
            rmqPublisher.publishToErrorQueue(msgQueue, MQConfig.ERROR_QUEUE);
        }


    }



    private boolean checkifAirtelIsAllowed(String msgSubMobileNo, String accActivateNonSaf) {
        String _prefix = getMobilePrefix(msgSubMobileNo);
        long opPrefix = _prefix == null ? 1L : Long.parseLong(_prefix);
        OperatorPrefix operatorPrefix = this.operatorPrefixService.findByOpPrefixAndOpOperator(opPrefix, "Airtel");
        if (operatorPrefix != null) {
            if (accActivateNonSaf != null) {
                return accActivateNonSaf.equalsIgnoreCase("TRUE");
            }
        }
        return false;
    }

    private BigDecimal getCostPerSMS(UUID msgAccId) {
        Account acc = this.accountService.findByAccId(msgAccId);
        BigDecimal _sms_price = acc.getAccSmsPrice();
        return _sms_price == null ? new BigDecimal("1.50") : _sms_price;
    }

    private String getPrsp(MsgQueue msgQueue) {
        // return this.messageService.getMessagePRSP(msgQueue.getMsgAccId(), msgQueue.getMsgSubMobileNo(), msgQueue.getId());
        return "WEISER";
    }


    private int getNoOfMessage(MsgQueue msgQueue) {
        String PRSP = getPrsp(msgQueue);
        if (PRSP == null) {
            PRSP = "WEISER";
        }


        int MSG_LENGTH = msgQueue.getMsgMessage().length();

        int NO_OF_MSG = 1;
        int no_of_characters_per_message = 160;
        if (PRSP.equalsIgnoreCase("INFOBIP")) {
            no_of_characters_per_message = 150;
        }
        return (int) Math.ceil((double) MSG_LENGTH / no_of_characters_per_message);


    }

}


