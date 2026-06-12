package com.spa.smart_gate_springboot.crons;

import com.spa.smart_gate_springboot.account_setup.account.Account;
import com.spa.smart_gate_springboot.account_setup.account.AccountRepository;
import com.spa.smart_gate_springboot.account_setup.shortsetup.MsgShortcodeSetup;
import com.spa.smart_gate_springboot.account_setup.shortsetup.MsgShortcodeSetupRepository;
import com.spa.smart_gate_springboot.mailjet.JavaEmailService;
import com.spa.smart_gate_springboot.messaging.send_message.MsgQueue;
import com.spa.smart_gate_springboot.messaging.send_message.QueueMsgService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.util.TextUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Alerts the account admin (email + SMS) when their SMS balance drops to or below a
 * threshold, so they can top up quickly.
 * <p>
 * Alerts are throttled to at most one per account every {@code alertIntervalMinutes}
 * (default 60) via the {@code acc_last_low_bal_alert} column. The threshold (default
 * 100) sits well above the hard send cut-off (balance &lt; 10), so the account still has
 * credit to pay for its own alert SMS, which is sent through its own registered sender.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LowBalanceAlertCron {

    /** Balance at or below which an alert is sent. */
    @Value("${sms.low-balance.threshold:100}")
    private BigDecimal threshold;

    /** Minimum time between alerts for the same account. */
    @Value("${sms.low-balance.alert-interval-minutes:60}")
    private long alertIntervalMinutes;

    private final AccountRepository accountRepository;
    private final JavaEmailService emailService;
    private final MsgShortcodeSetupRepository shortcodeSetupRepository;
    private final QueueMsgService queueMsgService;

    /** Scan every 10 minutes; the per-account throttle keeps it to ~1 alert/hour. */
    @Scheduled(fixedRate = 10 * 60_000)
    public void alertLowBalanceAccounts() {
        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime alertBefore = now.minusMinutes(alertIntervalMinutes);
            List<Account> accounts = accountRepository.findAccountsForLowBalanceAlert(threshold, alertBefore);
            if (accounts.isEmpty()) {
                return;
            }
            log.info("Sending {} low-balance alert(s)", accounts.size());
            for (Account acc : accounts) {
                sendAlert(acc, now);
            }
        } catch (Exception e) {
            log.error("Error running low-balance alert cron: {}", e.getMessage(), e);
        }
    }

    private void sendAlert(Account acc, LocalDateTime now) {
        boolean anySent = false;

        // Email alert
        try {
            if (!TextUtils.isEmpty(acc.getAccAdminEmail())) {
                emailService.sendMail(acc.getAccAdminEmail(), lowBalanceSubject(acc), lowBalanceEmailBody(acc));
                anySent = true;
            }
        } catch (Exception e) {
            log.error("Low-balance email failed acc={} : {}", acc.getAccId(), e.getMessage());
        }

        // SMS alert (quick action) — sent from the account's own balance and sender.
        try {
            if (sendAlertSms(acc)) {
                anySent = true;
            }
        } catch (Exception e) {
            log.error("Low-balance SMS failed acc={} : {}", acc.getAccId(), e.getMessage());
        }

        if (anySent) {
            // Stamp the throttle only once at least one channel was dispatched.
            accountRepository.updateLowBalanceAlertTime(acc.getAccId(), now);
            log.info("Low-balance alert sent acc={} bal={} email={} mobile={}",
                    acc.getAccId(), acc.getAccMsgBal(), acc.getAccAdminEmail(), acc.getAccAdminMobile());
        }
    }

    private boolean sendAlertSms(Account acc) {
        String mobile = normalizeMsisdn(acc.getAccAdminMobile());
        if (TextUtils.isEmpty(mobile)) {
            return false;
        }
        String senderId = resolveSenderId(acc.getAccId());
        if (TextUtils.isEmpty(senderId)) {
            log.warn("No sender id configured for acc={}, skipping low-balance SMS", acc.getAccId());
            return false;
        }

        MsgQueue msgQueue = MsgQueue.builder()
                .msgAccId(acc.getAccId())
                .msgStatus("PENDING_PROCESSING")
                .msgSenderId(senderId)
                .msgMessage(lowBalanceSmsBody(acc))
                .msgSubMobileNo(mobile)
                .msgCreatedDate(new Date())
                .msgCreatedTime(String.valueOf(LocalDateTime.now()))
                .msgCreatedByEmail("AUTO")
                .build();

        // Goes through the normal send pipeline: balance is checked and charged there.
        queueMsgService.publishNewMessageSynq(msgQueue);
        return true;
    }

    /**
     * Any sender id belonging to the account works for the alert, so just pick the first
     * one. Null-safe: returns null (and the SMS is skipped) if the account has none.
     */
    private String resolveSenderId(UUID accId) {
        List<MsgShortcodeSetup> setups = shortcodeSetupRepository.findByShAccId(accId);
        if (setups == null || setups.isEmpty()) {
            return null;
        }
        return setups.stream()
                .map(MsgShortcodeSetup::getShCode)
                .filter(code -> !TextUtils.isEmpty(code))
                .findFirst()
                .orElse(null);
    }

    private String lowBalanceSubject(Account acc) {
        return "Low SMS Balance Alert - " + acc.getAccName();
    }

    private String lowBalanceEmailBody(Account acc) {
        return "Dear " + acc.getAccName() + ",\n\n"
                + "Your SMS account balance is running low.\n"
                + "Current balance: " + acc.getAccMsgBal() + "\n\n"
                + "Please top up to avoid interruption to your messaging.\n\n"
                + "Regards,\nSynq-Africa";
    }

    private String lowBalanceSmsBody(Account acc) {
        return "Synq: Your SMS balance is low (" + acc.getAccMsgBal() + "). Please top up to keep sending messages.";
    }

    /** Normalise a Kenyan mobile number to 2547XXXXXXXX form. */
    private String normalizeMsisdn(String msisdn) {
        if (TextUtils.isEmpty(msisdn)) {
            return null;
        }
        String s = msisdn.trim().replaceAll("[\\s+()\\-]", "");
        if (s.startsWith("0")) {
            s = "254" + s.substring(1);
        } else if (s.length() == 9 && s.startsWith("7")) {
            s = "254" + s;
        }
        return s;
    }
}
