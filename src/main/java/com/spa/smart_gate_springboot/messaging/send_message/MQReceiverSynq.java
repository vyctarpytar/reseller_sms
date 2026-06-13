package com.spa.smart_gate_springboot.messaging.send_message;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.spa.smart_gate_springboot.MQRes.MQConfig;
import com.spa.smart_gate_springboot.account_setup.account.Account;
import com.spa.smart_gate_springboot.account_setup.account.AccountService;
import com.spa.smart_gate_springboot.account_setup.reseller.Reseller;
import com.spa.smart_gate_springboot.account_setup.reseller.ResellerService;
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
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@EnableRabbit
@Slf4j
public class MQReceiverSynq {
    private final AccountService accountService;
    private final ResellerService resellerService;
    private final ObjectMapper objectMapper;
    private final UserService userService;
    private final GlobalUtils gu;
    private final SmsDispatchService smsDispatchService;

    @RabbitListener(queues = {MQConfig.SYNQ_QUEUE,MQConfig.QUEUE}, containerFactory = "smsListenerContainerFactory")
    public void consumeMessage(Channel channel, Message message) {
        // Process SYNCHRONOUSLY on the container consumer thread that owns this Channel. RabbitMQ
        // Channels are not thread-safe, so the ack must be issued on this very thread — the previous
        // taskExecutor hand-off acked from a foreign worker thread and, whenever that worker outlived
        // the channel (a slow carrier send vs. a recycled/closed channel), the `if (channel.isOpen())`
        // guard silently skipped the ack and the message sat unacked forever. receiver() now issues
        // exactly one ack in a finally block, on this thread, so a delivery always leaves the broker.
        // Throughput comes from the container's consumer concurrency (smsListenerContainerFactory),
        // not from a separate pool; prefetch=1 there gives true backpressure under a slow carrier.
        receiver(message, channel);
    }


    /**
     * Build the archive row for a send: copy the payload, stamp the stable idempotency key
     * ({@code msgDedupKey}, set at publish) and a fresh per-attempt carrier id ({@code msgCode}), and
     * fill the reseller/sender fields. {@code msgId} is left null so this is an INSERT; the unique index
     * on {@code msgDedupKey} is what dedupes a redelivery when {@link SmsDispatchService} saves it.
     */
    private MsgMessageQueueArc buildArc(MsgQueue msgQueue, Reseller reseller) {
        MsgMessageQueueArc arc = new MsgMessageQueueArc();
        BeanUtils.copyProperties(msgQueue, arc, gu.getNullPropertyNames(msgQueue));
        arc.setMsgId(null);
        arc.setMsgExternalId(msgQueue.getMsgExternalId());
        arc.setMsgDedupKey(msgQueue.getMsgDedupKey());
        arc.setMsgRetryCount(0);
        arc.setMsgClientDeliveryStatus("PENDING");
        arc.setMsgSenderIdName(msgQueue.getMsgSenderId());
        arc.setMsgResellerId(reseller.getRsId());
        arc.setMsgResellerName(reseller.getRsCompanyName());
        if (arc.getMsgCreatedBy() != null) {
            try {
                arc.setMsgCreatedByEmail(userService.findById(arc.getMsgCreatedBy()).getEmail());
            } catch (Exception e) {
                log.error("Error while creating email address : {}", e.getLocalizedMessage());
            }
        }
        arc.setMsgSenderLevel("WEISER");
        arc.setMsgCode(new UniqueCodeGenerator().generateSecureApiKey());
        return arc;
    }


    private void receiver(Message message, Channel channel) {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        MsgQueue msgQueue = null;
        MsgMessageQueueArc arc = null;
        try {
            byte[] payload = message.getBody();
            msgQueue = objectMapper.readValue(payload, MsgQueue.class);


            if (TextUtils.isEmpty(msgQueue.getMsgMessage())) {
                return; // nothing to send; the finally block still acks so it leaves the broker
            }
            if (TextUtils.isEmpty(msgQueue.getMsgSubMobileNo())) {
                return; // no recipient; acked in finally
            }
            msgQueue.setMsgExternalId(String.valueOf(msgQueue.getMsgExternalId()));
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
            BigDecimal totalCost = getCostPerSMS(msgQueue.getMsgAccId()).multiply(new BigDecimal(no_of_msg));
            msgQueue.setMsgPage(no_of_msg);
            msgQueue.setMsgCostId(totalCost);

            Reseller reseller = resellerService.findById(acc.getAccResellerId());
            msgQueue.setMsgResellerId(reseller.getRsId());
            msgQueue.setMsgResellerName(reseller.getRsCompanyName());

            arc = buildArc(msgQueue, reseller);

            // Reserve-then-send, made idempotent: reserveAndDebit inserts the arc keyed by its unique
            // msgDedupKey AND debits in one transaction. A RabbitMQ redelivery of the same message fails
            // that insert (DataIntegrityViolationException) and is skipped here — no second debit, no
            // second send. NO_CREDIT persists the arc as PENDING_CREDIT (cost recorded) so the resend-on-
            // top-up flow can fund and send it in place; we never publish to the out-of-credit queue or
            // write a second row. The carrier send runs OUTSIDE the reserve transaction.
            try {
                SmsDispatchService.Reservation outcome = smsDispatchService.reserveAndDebit(arc);
                if (outcome == SmsDispatchService.Reservation.RESERVED) {
                    smsDispatchService.dispatchSend(arc);
                } else {
                    log.info("[SMS] insufficient credit acc={} cost={} — persisted PENDING_CREDIT (dedup {})",
                            arc.getMsgAccId(), totalCost, arc.getMsgDedupKey());
                }
            } catch (DataIntegrityViolationException duplicate) {
                // Redelivery of an already-processed message — the arc already exists for this dedup key.
                log.info("[SMS] duplicate delivery (dedup {}) — already processed, skipping", arc.getMsgDedupKey());
            }

        } catch (Exception em) {
            // Once an arc is persisted, the DB-status retry cron owns the retry. Before that (an
            // undeserializable payload or a failure during enrichment) there's no row to retry and
            // nothing useful to requeue — a redelivery would just loop on the same bad payload — so we
            // log loudly and let the finally block ack it off the broker.
            log.error("SMS processing failed for delivery {} (arc persisted: {}): {}",
                    deliveryTag, arc != null, em.getMessage(), em);
        } finally {
            // Always ack exactly once, on this consumer thread (which owns the Channel). The design is
            // "ack-and-record; never requeue" — failures live on as DB rows for the retry cron, so a
            // requeue would only double-debit/double-send (units are reserved up-front, not idempotent).
            ack(channel, deliveryTag);
        }
    }

    /** Ack a delivery on the consumer thread that owns the Channel — exactly once, never throwing. */
    private void ack(Channel channel, long deliveryTag) {
        try {
            if (channel.isOpen()) {
                channel.basicAck(deliveryTag, false);
            } else {
                // With synchronous processing the container holds this channel open for the whole
                // listener invocation, so this should not happen; if it ever does the broker redelivers.
                log.warn("Channel closed before ack of delivery {} — broker will redeliver", deliveryTag);
            }
        } catch (IOException e) {
            log.error("Failed to ack delivery {}: {}", deliveryTag, e.getMessage());
        }
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


