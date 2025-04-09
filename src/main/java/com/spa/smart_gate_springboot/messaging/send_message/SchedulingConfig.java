package com.spa.smart_gate_springboot.messaging.send_message;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.spa.smart_gate_springboot.account_setup.account.Account;
import com.spa.smart_gate_springboot.account_setup.account.AccountService;
import com.spa.smart_gate_springboot.account_setup.reseller.Reseller;
import com.spa.smart_gate_springboot.account_setup.reseller.ResellerRepo;
import com.spa.smart_gate_springboot.messaging.delivery.MsgDelivery;
import com.spa.smart_gate_springboot.messaging.delivery.MsgDeliveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.util.TextUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Component
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class SchedulingConfig {
    private final MsgMessageQueueArcRepository arcRepository;
    private final AccountService accountService;
    private final QueueMsgService queueMsgService;
    private final ResellerRepo resellerRepo;
    private final ObjectMapper objectMapper;
    private final MsgDeliveryService msgDeliveryService;








    public void resendToWeiser(UUID rsId, String msgStatus) {
        Set<UUID> accountList = accountService.findAccountByResellerId(rsId).stream().map(Account::getAccId).collect(Collectors.toSet());
        PageRequest pageRequest = PageRequest.of(0, 1000);
        Page<MsgMessageQueueArc> pageData = arcRepository.resendSmsPagable(accountList, msgStatus, pageRequest);
        List<MsgMessageQueueArc> pdRsCredit = pageData.getContent();
        pdRsCredit.forEach(m -> {
            try {
                MsgQueue msgQueue = new MsgQueue();
                BeanUtils.copyProperties(m, msgQueue);
                arcRepository.deleteById(m.getMsgId());
                if (!TextUtils.isEmpty(m.getMsgErrorCode()) && m.getMsgErrorCode().equalsIgnoreCase("200")) {
                    // refund the customer
                    accountService.refundCostCharged(msgQueue.getMsgAccId(), msgQueue.getMsgCostId());
                }
                msgQueue.setMsgSentRetried(true);
                msgQueue.setMsgCreatedDate(new Date());
                queueMsgService.publishNewMessage(msgQueue);
            } catch (Exception e) {
                log.error("ResendToWeiser error {}", e.getMessage(), e);
            }
        });
    }

    public void resendToSynq(UUID rsId, String msgStatus) {
        Set<UUID> accountList = accountService.findAccountByResellerId(rsId).stream().map(Account::getAccId).collect(Collectors.toSet());
        PageRequest pageRequest = PageRequest.of(0, 500);
        Page<MsgMessageQueueArc> pageData = arcRepository.resendSmsPagable(accountList, msgStatus, pageRequest);
        List<MsgMessageQueueArc> pdRsCredit = pageData.getContent();
        pdRsCredit.forEach(m -> {
            new Thread(() -> {
                try {
                    MsgQueue msgQueue = new MsgQueue();
                    BeanUtils.copyProperties(m, msgQueue);
                    arcRepository.delete(m);
                    if (!TextUtils.isEmpty(m.getMsgErrorCode()) && m.getMsgErrorCode().equalsIgnoreCase("200")) {
                        // refund the customer
                        Account acc = accountService.findByAccId(msgQueue.getMsgAccId());
                        BigDecimal newBal = msgQueue.getMsgCostId().add(acc.getAccMsgBal());
                        acc.setAccMsgBal(newBal);
                        accountService.save(acc);
                    }
                    msgQueue.setMsgSentRetried(true); //todo reset to true
                    msgQueue.setMsgCreatedDate(new Date());
                    msgQueue.setMsgSenderId(m.getMsgSenderIdName());
                    queueMsgService.publishNewMessageSynq(msgQueue);


                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        });
    }


    @Scheduled(fixedRate = 5000)
    public void loopResellers() {
//        Reseller reseller = resellerRepo.findById(UUID.fromString("d9221e5a-f7d2-4be9-b29f-0fa85dbe30e4")).orElse(null);
        List<Reseller> resellerList = resellerRepo.findAll();
//        if (reseller != null) {
        for (Reseller reseller : resellerList) {
//            log.error("resending message {}------({})", reseller.getRsCompanyName(), reseller.getRsId());

            resendToWeiser(reseller.getRsId(), "Exception sending");
            resendToWeiser(reseller.getRsId(), "ERROR");
            resendToWeiser(reseller.getRsId(), "ERRORR");

        }

    }



    //disable temporarily
//    @Scheduled(fixedRate = 1000 * 60)
    public void resendSentStatusWithinHrs() {
        try {
            PageRequest pageRequest = PageRequest.of(0, 100);
            Page<MsgMessageQueueArc> pagedData = arcRepository.resendSentStatusAfter4hrs(pageRequest);
            List<MsgMessageQueueArc> resend = pagedData.getContent();
            resend.forEach(m -> {
                new Thread(() -> {
                    MsgQueue msgQueue = new MsgQueue();
                    BeanUtils.copyProperties(m, msgQueue);
                    arcRepository.delete(m);

                    accountService.refundCostCharged(msgQueue.getMsgAccId(), msgQueue.getMsgCostId());
                    msgQueue.setMsgCreatedDate(new Date());
                    msgQueue.setMsgSentRetried(true);
                    queueMsgService.publishNewMessage(msgQueue);
                }).start();
            });
        } catch (Exception e) {
            log.error(e.getLocalizedMessage());
        }

    }



    @Scheduled(fixedRate = 1000*5)
    public void reconDeliveryNotes() {

        List<MsgDelivery> deliveries = msgDeliveryService.reconDeliveryNotes();
        deliveries.forEach(m->{
            queueMsgService.updateArcDnR(m);
            log.info("Delivery Notes Recons: {} ", m);
        });
    }

    @Scheduled(fixedRate = 1000*60*5)
    public void health() {
        log.info("Health Check Cron:   {}", new Date());
    }


}



