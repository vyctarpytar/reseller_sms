package com.spa.smart_gate_springboot.crons;

import com.spa.smart_gate_springboot.notifications.ScheduledNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationCron {

    private final ScheduledNotificationService scheduledNotificationService;

    @Scheduled(fixedDelayString = "${notifications.scheduler.interval-ms:60000}")
    public void dispatchDueNotifications() {
        try {
            scheduledNotificationService.dispatchDue();
        } catch (Exception e) {
            log.error("[NOTIF] Scheduled notification tick failed: {}", e.getMessage(), e);
        }
    }
}
