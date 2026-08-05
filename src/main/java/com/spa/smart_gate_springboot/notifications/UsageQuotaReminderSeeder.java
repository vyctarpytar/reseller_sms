package com.spa.smart_gate_springboot.notifications;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
@Order(45)
public class UsageQuotaReminderSeeder implements ApplicationRunner {

    static final String SEED_KEY = "USAGE_QUOTA_EXPIRY";
    static final String NAME = "Usage Quota Expiring This Week Reminder";
    private static final String SUBJECT = "Usage Quota Expiring This Week";
    private static final String MESSAGE =
            "Reminder: your Synq Africa usage quota expires this week. "
                    + "Please review your balance and top up to avoid interruption to your messaging.";
    private static final String SEED_MSISDN = "254716177880";
    private static final String SEED_EMAIL = "server@synqafrica.co.ke";

    private final ScheduledNotificationRepository repository;
    private final ScheduledNotificationService service;

    @Override
    public void run(ApplicationArguments args) {
        try {
            if (repository.existsBySnSeedKey(SEED_KEY)) return;

            ScheduledNotification notification = ScheduledNotification.builder()
                    .snSeedKey(SEED_KEY)
                    .snName(NAME)
                    .snSubject(SUBJECT)
                    .snMessage(MESSAGE)
                    .snFrequency(NotificationFrequency.EVERY_2_MONTHS.name())
                    .snSendTimes("09:00")
                    .snStartDate(LocalDate.now())
                    .snChannels(ScheduledNotification.CHANNEL_SMS + "," + ScheduledNotification.CHANNEL_EMAIL)
                    .snRecipients(SEED_MSISDN)
                    .snEmails(SEED_EMAIL)
                    // Seeded PAUSED on purpose: the placeholder recipients are ours, not the customer's,
                    // so nothing goes out until someone edits them and activates it from the UI.
                    .snStatus(ScheduledNotification.STATUS_PAUSED)
                    .snRunCount(0)
                    .snCreatedByName("SYSTEM")
                    .snCreatedOn(LocalDateTime.now())
                    .build();
            notification.setSnNextRunAt(service.computeNextRun(notification));

            repository.saveAndFlush(notification);
            log.info("[NOTIF] Seeded '{}' (PAUSED, next run {})", NAME, notification.getSnNextRunAt());
        } catch (Exception e) {
            log.error("[NOTIF] Could not seed '{}': {}", NAME, e.getMessage(), e);
        }
    }
}
