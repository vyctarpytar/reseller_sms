package com.spa.smart_gate_springboot.crons;

import com.spa.smart_gate_springboot.utils.AppTime;

import com.spa.smart_gate_springboot.account_setup.account.Account;
import com.spa.smart_gate_springboot.account_setup.account.AccountRepository;
import com.spa.smart_gate_springboot.account_setup.reseller.Reseller;
import com.spa.smart_gate_springboot.account_setup.reseller.ResellerRepo;
import com.spa.smart_gate_springboot.account_setup.shortsetup.MsgShortcodeSetup;
import com.spa.smart_gate_springboot.account_setup.shortsetup.MsgShortcodeSetupRepository;
import com.spa.smart_gate_springboot.mailjet.JavaEmailService;
import com.spa.smart_gate_springboot.messaging.send_message.MsgQueue;
import com.spa.smart_gate_springboot.messaging.send_message.QueueMsgService;
import com.spa.smart_gate_springboot.messaging.send_message.SystemSmsService;
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
 * Alerts the account admin (email + SMS) when their SMS balance drops to or below the
 * account's reorder level ({@code acc_reorder_level}, set on the account edit form), so they
 * can top up quickly. Accounts that have not set one use {@code defaultThreshold}; a level of
 * 0 turns alerts off for that account.
 * <p>
 * Alerts are throttled to at most one per account every {@code alertIntervalMinutes}
 * (default 180) via the {@code acc_last_low_bal_alert} column. The alert SMS is sent through
 * the account's own registered sender and paid from its own balance, so a level below the
 * hard send cut-off (balance &lt; 10) gets the email only.
 * <p>
 * Resellers get the same treatment on their allocatable units ({@code rs_reorder_level}, same
 * default and throttle). Their alert SMS goes out through {@link SystemSmsService}, since a
 * reseller has no sender or unit balance of its own to send from.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LowBalanceAlertCron {

    /** Reorder level for accounts that have not set their own. */
    @Value("${sms.low-balance.threshold:100}")
    private BigDecimal defaultThreshold;

    /** Minimum time between alerts for the same account. */
    @Value("${sms.low-balance.alert-interval-minutes:180}")
    private long alertIntervalMinutes;

    private final AccountRepository accountRepository;
    private final JavaEmailService emailService;
    private final MsgShortcodeSetupRepository shortcodeSetupRepository;
    private final QueueMsgService queueMsgService;
    private final ResellerRepo resellerRepo;
    private final SystemSmsService systemSmsService;

    /** Scan every 10 minutes; the per-account throttle keeps it to ~1 alert/3 hours. */
    @Scheduled(fixedRate = 10 * 60_000)
    public void alertLowBalanceAccounts() {
        try {
            LocalDateTime now = AppTime.now();
            LocalDateTime alertBefore = now.minusMinutes(alertIntervalMinutes);
            List<Account> accounts = accountRepository.findAccountsForLowBalanceAlert(
                    defaultThreshold, alertBefore, now.toLocalDate().atStartOfDay());
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

    /** Same cadence and throttle as the account pass, on the reseller's allocatable units. */
    @Scheduled(fixedRate = 10 * 60_000)
    public void alertLowBalanceResellers() {
        try {
            LocalDateTime now = AppTime.now();
            LocalDateTime alertBefore = now.minusMinutes(alertIntervalMinutes);
            List<Reseller> resellers = resellerRepo.findResellersForLowBalanceAlert(
                    defaultThreshold, alertBefore, now.toLocalDate().atStartOfDay());
            if (resellers.isEmpty()) {
                return;
            }
            log.info("Sending {} reseller low-balance alert(s)", resellers.size());
            for (Reseller rs : resellers) {
                sendResellerAlert(rs, now);
            }
        } catch (Exception e) {
            log.error("Error running reseller low-balance alert cron: {}", e.getMessage(), e);
        }
    }

    private void sendResellerAlert(Reseller rs, LocalDateTime now) {
        boolean anySent = false;
        BigDecimal units = rs.getRsAllocatableUnit() == null ? BigDecimal.ZERO : rs.getRsAllocatableUnit();
        BigDecimal level = reorderLevel(rs);

        try {
            if (!TextUtils.isEmpty(rs.getRsEmail())) {
                emailService.sendMail(rs.getRsEmail(), "Low SMS Unit Balance Alert - " + rs.getRsCompanyName(),
                        "Dear " + rs.getRsCompanyName() + ",\n\n"
                                + "Your allocatable SMS units are running low.\n"
                                + "Current units: " + units + "\n"
                                + "Your reorder level: " + level + "\n\n"
                                + "Please top up so your accounts can keep buying units.\n\n"
                                + "Regards,\nSynq-Africa");
                anySent = true;
            }
        } catch (Exception e) {
            log.error("Reseller low-balance email failed rs={} : {}", rs.getRsId(), e.getMessage());
        }

        String mobile = normalizeMsisdn(rs.getRsPhoneNumber());
        if (!TextUtils.isEmpty(mobile)) {
            // Fire-and-forget on its own thread; it never throws back here.
            systemSmsService.sendSms(mobile, "Synq: Your allocatable SMS units are low (" + units
                    + "). Please top up so your accounts can keep buying units.");
            anySent = true;
        }

        if (anySent) {
            resellerRepo.updateLowBalanceAlertTime(rs.getRsId(), now);
            log.info("Reseller low-balance alert sent rs={} units={} level={} email={} mobile={}",
                    rs.getRsId(), units, level, rs.getRsEmail(), rs.getRsPhoneNumber());
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
            log.info("Low-balance alert sent acc={} bal={} level={} email={} mobile={}",
                    acc.getAccId(), acc.getAccMsgBal(), reorderLevel(acc), acc.getAccAdminEmail(), acc.getAccAdminMobile());
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
                .msgCreatedDate(AppTime.nowDate())
                .msgCreatedTime(String.valueOf(AppTime.now()))
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
                + "Current balance: " + acc.getAccMsgBal() + "\n"
                + "Your reorder level: " + reorderLevel(acc) + "\n\n"
                + "Please top up to avoid interruption to your messaging.\n\n"
                + "Regards,\nSynq-Africa";
    }

    private String lowBalanceSmsBody(Account acc) {
        return "Synq: Your SMS balance is low (" + acc.getAccMsgBal() + "). Please top up to keep sending messages.";
    }

    private BigDecimal reorderLevel(Account acc) {
        return acc.getAccReorderLevel() != null ? BigDecimal.valueOf(acc.getAccReorderLevel()) : defaultThreshold;
    }

    private BigDecimal reorderLevel(Reseller rs) {
        String level = rs.getRsReorderLevel() == null ? "" : rs.getRsReorderLevel().trim();
        return level.matches("[0-9]+") ? new BigDecimal(level) : defaultThreshold;
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
