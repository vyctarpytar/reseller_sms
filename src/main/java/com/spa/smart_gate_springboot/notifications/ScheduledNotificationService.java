package com.spa.smart_gate_springboot.notifications;

import com.spa.smart_gate_springboot.utils.AppTime;

import com.spa.smart_gate_springboot.mailjet.JavaEmailService;
import com.spa.smart_gate_springboot.mailjet.SynqEmailTemplate;
import com.spa.smart_gate_springboot.messaging.send_message.SystemSmsService;
import com.spa.smart_gate_springboot.user.User;
import com.spa.smart_gate_springboot.utils.StandardJsonResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduledNotificationService {

    private static final Pattern TIME_PATTERN = Pattern.compile("^([01]\\d|2[0-3]):[0-5]\\d$");
    private static final Pattern MSISDN_PATTERN = Pattern.compile("^254[71]\\d{8}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");
    private static final String DEFAULT_SEND_TIME = "09:00";
    private static final int MAX_ADVANCE_STEPS = 2000;

    private final ScheduledNotificationRepository notificationRepository;
    private final ScheduledNotificationLogRepository logRepository;
    private final SystemSmsService systemSmsService;
    private final JavaEmailService javaEmailService;

    public StandardJsonResponse list(NotificationFilterDto dto) {
        var resp = new StandardJsonResponse();
        NotificationFilterDto filter = dto == null ? new NotificationFilterDto() : dto;

        Pageable pageable = PageRequest.of(Math.max(filter.getStart(), 0),
                filter.getLimit() <= 0 ? 10 : filter.getLimit(),
                Sort.by("snCreatedOn").descending());
        Page<ScheduledNotification> page = notificationRepository.findAll(
                ScheduledNotificationSpecifications.filter(filter.getSnStatus(), filter.getSearch()), pageable);

        resp.setData("result", page.getContent(), resp);
        resp.setTotal((int) page.getTotalElements());
        return resp;
    }

    public StandardJsonResponse save(NotificationDto dto, User user) {
        var resp = new StandardJsonResponse();

        ScheduledNotification notification = new ScheduledNotification();
        apply(notification, dto);
        notification.setSnStatus(ScheduledNotification.STATUS_ACTIVE);
        notification.setSnRunCount(0);
        notification.setSnCreatedById(user.getUsrId());
        notification.setSnCreatedByName(user.getEmail());
        notification.setSnCreatedOn(AppTime.now());
        notification.setSnNextRunAt(computeNextRun(notification));

        ScheduledNotification saved = notificationRepository.saveAndFlush(notification);
        log.info("[NOTIF] Created reminder {} '{}' nextRun={}", saved.getSnId(), saved.getSnName(), saved.getSnNextRunAt());

        resp.setData("result", saved, resp);
        resp.setMessage("message", "Scheduled notification created successfully", resp);
        return resp;
    }

    public StandardJsonResponse update(UUID snId, NotificationDto dto, User user) {
        var resp = new StandardJsonResponse();

        ScheduledNotification notification = require(snId);
        apply(notification, dto);
        notification.setSnNextRunAt(computeNextRun(notification));
        notification.setSnUpdatedById(user.getUsrId());
        notification.setSnUpdatedOn(AppTime.now());

        ScheduledNotification saved = notificationRepository.saveAndFlush(notification);
        resp.setData("result", saved, resp);
        resp.setMessage("message", "Scheduled notification updated successfully", resp);
        return resp;
    }

    public StandardJsonResponse toggle(UUID snId, User user) {
        var resp = new StandardJsonResponse();

        ScheduledNotification notification = require(snId);
        if (ScheduledNotification.STATUS_DELETED.equals(notification.getSnStatus())) {
            throw new IllegalArgumentException("A deleted scheduled notification cannot be toggled");
        }

        boolean resuming = !ScheduledNotification.STATUS_ACTIVE.equals(notification.getSnStatus());
        notification.setSnStatus(resuming ? ScheduledNotification.STATUS_ACTIVE : ScheduledNotification.STATUS_PAUSED);
        if (resuming) {
            // Roll the schedule forward past now, so a long-paused reminder doesn't fire a backlog.
            notification.setSnNextRunAt(computeNextRun(notification));
        }
        notification.setSnUpdatedById(user.getUsrId());
        notification.setSnUpdatedOn(AppTime.now());

        ScheduledNotification saved = notificationRepository.saveAndFlush(notification);
        resp.setData("result", saved, resp);
        resp.setMessage("message", resuming ? "Scheduled notification resumed" : "Scheduled notification paused", resp);
        return resp;
    }

    public StandardJsonResponse delete(UUID snId, User user) {
        var resp = new StandardJsonResponse();

        ScheduledNotification notification = require(snId);
        notification.setSnStatus(ScheduledNotification.STATUS_DELETED);
        notification.setSnUpdatedById(user.getUsrId());
        notification.setSnUpdatedOn(AppTime.now());
        notificationRepository.saveAndFlush(notification);

        resp.setMessage("message", "Scheduled notification deleted successfully", resp);
        return resp;
    }

    public StandardJsonResponse runNow(UUID snId, User user) {
        var resp = new StandardJsonResponse();

        ScheduledNotification notification = require(snId);
        // A manual test fire must not advance snNextRunAt, or it would skip the real schedule.
        ScheduledNotificationLog runLog = dispatch(notification, user.getEmail());
        notification.setSnLastRunAt(runLog.getSnlRunAt());
        notification.setSnLastStatus(runLog.getSnlStatus());
        notificationRepository.saveAndFlush(notification);

        resp.setData("result", runLog, resp);
        resp.setMessage("message", "Scheduled notification dispatched", resp);
        return resp;
    }

    public StandardJsonResponse logs(UUID snId, int start, int limit) {
        var resp = new StandardJsonResponse();

        Pageable pageable = PageRequest.of(Math.max(start, 0), limit <= 0 ? 10 : limit);
        Page<ScheduledNotificationLog> page =
                logRepository.findBySnlNotificationIdOrderBySnlRunAtDesc(snId, pageable);

        resp.setData("result", page.getContent(), resp);
        resp.setTotal((int) page.getTotalElements());
        return resp;
    }

    public void dispatchDue() {
        LocalDateTime now = AppTime.now();
        List<ScheduledNotification> due = notificationRepository
                .findBySnStatusAndSnNextRunAtLessThanEqual(ScheduledNotification.STATUS_ACTIVE, now);
        if (due == null || due.isEmpty()) {
            return;
        }

        log.info("[NOTIF] {} scheduled notification(s) due", due.size());
        for (ScheduledNotification notification : due) {
            try {
                ScheduledNotificationLog runLog = dispatch(notification, ScheduledNotificationLog.TRIGGER_CRON);
                notification.setSnLastRunAt(runLog.getSnlRunAt());
                notification.setSnLastStatus(runLog.getSnlStatus());
            } catch (Exception e) {
                log.error("[NOTIF] Dispatch failed for {} : {}", notification.getSnId(), e.getMessage(), e);
                notification.setSnLastRunAt(AppTime.now());
                notification.setSnLastStatus(ScheduledNotificationLog.STATUS_FAILED);
            }
            // Always roll forward, even after a failed dispatch, or the row stays due every tick.
            try {
                Integer runCount = notification.getSnRunCount();
                notification.setSnRunCount(runCount == null ? 1 : runCount + 1);
                notification.setSnNextRunAt(nextOccurrenceAfter(notification, AppTime.now()));
                notificationRepository.saveAndFlush(notification);
            } catch (Exception e) {
                log.error("[NOTIF] Could not roll schedule forward for {} : {}", notification.getSnId(), e.getMessage(), e);
            }
        }
    }

    private ScheduledNotificationLog dispatch(ScheduledNotification notification, String triggeredBy) {
        LocalDateTime runAt = AppTime.now();
        String channels = notification.getSnChannels() == null ? "" : notification.getSnChannels();
        int smsSent = 0;
        int smsFailed = 0;
        int emailSent = 0;
        int emailFailed = 0;

        if (channels.contains(ScheduledNotification.CHANNEL_SMS)) {
            for (String msisdn : split(notification.getSnRecipients())) {
                try {
                    systemSmsService.sendSms(msisdn, notification.getSnMessage());
                    smsSent++;
                } catch (Exception e) {
                    smsFailed++;
                    log.error("[NOTIF] SMS handoff failed sn={} msisdn={} : {}", notification.getSnId(), msisdn, e.getMessage());
                }
            }
        }

        if (channels.contains(ScheduledNotification.CHANNEL_EMAIL)) {
            List<String> emails = split(notification.getSnEmails());
            if (!emails.isEmpty()) {
                try {
                    javaEmailService.sendBrandedMail(String.join(",", emails), subjectOf(notification),
                            notification.getSnName(), emailBody(notification, runAt));
                    emailSent = emails.size();
                } catch (Exception e) {
                    emailFailed = emails.size();
                    log.error("[NOTIF] Email dispatch failed sn={} : {}", notification.getSnId(), e.getMessage());
                }
            }
        }

        int sent = smsSent + emailSent;
        int failed = smsFailed + emailFailed;
        String status;
        if (sent == 0) {
            status = ScheduledNotificationLog.STATUS_FAILED;
        } else if (failed > 0) {
            status = ScheduledNotificationLog.STATUS_PARTIAL;
        } else {
            status = ScheduledNotificationLog.STATUS_SUCCESS;
        }

        String detail = "SMS sent " + smsSent + ", failed " + smsFailed
                + "; Email sent " + emailSent + ", failed " + emailFailed;
        log.info("[NOTIF] Ran '{}' ({}) by {} -> {} [{}]", notification.getSnName(), notification.getSnId(),
                triggeredBy, status, detail);

        ScheduledNotificationLog runLog = ScheduledNotificationLog.builder()
                .snlNotificationId(notification.getSnId())
                .snlNotificationName(notification.getSnName())
                .snlRunAt(runAt)
                .snlStatus(status)
                .snlSmsSent(smsSent)
                .snlSmsFailed(smsFailed)
                .snlEmailSent(emailSent)
                .snlEmailFailed(emailFailed)
                .snlDetail(detail)
                .snlTriggeredBy(triggeredBy)
                .build();
        return logRepository.saveAndFlush(runLog);
    }

    private String emailBody(ScheduledNotification notification, LocalDateTime runAt) {
        String rows = SynqEmailTemplate.infoRow("Reminder", notification.getSnName())
                + SynqEmailTemplate.infoRow("Frequency", NotificationFrequency.parse(notification.getSnFrequency()).getLabel())
                + SynqEmailTemplate.infoRow("Sent", runAt.format(STAMP));
        return SynqEmailTemplate.paragraph(notification.getSnMessage()) + SynqEmailTemplate.infoTable(rows);
    }

    private String subjectOf(ScheduledNotification notification) {
        String subject = trimToNull(notification.getSnSubject());
        return subject == null ? notification.getSnName() : subject;
    }

    private void apply(ScheduledNotification notification, NotificationDto dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Request body is required");
        }
        String name = trimToNull(dto.getSnName());
        if (name == null) {
            throw new IllegalArgumentException("Notification name is required");
        }
        String message = trimToNull(dto.getSnMessage());
        if (message == null) {
            throw new IllegalArgumentException("Notification message is required");
        }

        NotificationFrequency frequency = NotificationFrequency.parse(dto.getSnFrequency());
        Integer intervalDays = dto.getSnIntervalDays();
        if (frequency == NotificationFrequency.CUSTOM_DAYS && (intervalDays == null || intervalDays < 1)) {
            throw new IllegalArgumentException("Interval days must be at least 1 when frequency is CUSTOM_DAYS");
        }

        String raw = trimToNull(dto.getSnSendTimes()) == null ? dto.getSnSendTime() : dto.getSnSendTimes();
        String sendTimes = normalizeSendTimes(raw);

        String channels = normalizeChannels(dto.getSnChannels());
        String recipients = normalizeRecipients(dto.getSnRecipients());
        String emails = normalizeEmails(dto.getSnEmails());
        if (channels.contains(ScheduledNotification.CHANNEL_SMS) && recipients.isEmpty()) {
            throw new IllegalArgumentException("At least one phone number is required for the SMS channel");
        }
        if (channels.contains(ScheduledNotification.CHANNEL_EMAIL) && emails.isEmpty()) {
            throw new IllegalArgumentException("At least one email address is required for the EMAIL channel");
        }

        String subject = trimToNull(dto.getSnSubject());
        notification.setSnName(name);
        notification.setSnSubject(subject == null ? name : subject);
        notification.setSnMessage(message);
        notification.setSnFrequency(frequency.name());
        notification.setSnIntervalDays(frequency == NotificationFrequency.CUSTOM_DAYS ? intervalDays : null);
        notification.setSnSendTimes(sendTimes);
        notification.setSnStartDate(dto.getSnStartDate() == null ? AppTime.today() : dto.getSnStartDate());
        notification.setSnChannels(channels);
        notification.setSnRecipients(recipients);
        notification.setSnEmails(emails);
    }

    LocalDateTime computeNextRun(ScheduledNotification notification) {
        return nextOccurrenceAfter(notification, AppTime.now());
    }

    // Occurrences are every cycle date x every send time; the next run is the earliest one after `after`.
    private LocalDateTime nextOccurrenceAfter(ScheduledNotification notification, LocalDateTime after) {
        NotificationFrequency frequency = NotificationFrequency.parse(notification.getSnFrequency());
        List<LocalTime> times = parseTimes(notification.getSnSendTimes());
        LocalDate start = notification.getSnStartDate() == null ? AppTime.today() : notification.getSnStartDate();
        Integer intervalDays = notification.getSnIntervalDays();

        // Runaway guard: a years-old start date on a daily cadence would otherwise loop unbounded.
        for (int step = 0; step < MAX_ADVANCE_STEPS; step++) {
            LocalDate date = frequency.advance(start, step, intervalDays);
            for (LocalTime time : times) {
                LocalDateTime candidate = LocalDateTime.of(date, time);
                if (candidate.isAfter(after)) {
                    return candidate;
                }
            }
        }
        return frequency.advance(after, intervalDays);
    }

    private List<LocalTime> parseTimes(String raw) {
        List<LocalTime> times = new ArrayList<>();
        for (String part : String.valueOf(raw == null ? DEFAULT_SEND_TIME : raw).split(",")) {
            String value = part.trim();
            if (TIME_PATTERN.matcher(value).matches()) {
                times.add(LocalTime.parse(value));
            }
        }
        if (times.isEmpty()) {
            times.add(LocalTime.parse(DEFAULT_SEND_TIME));
        }
        Collections.sort(times);
        return times;
    }

    private String normalizeSendTimes(String raw) {
        Set<String> times = new TreeSet<>();
        if (raw != null) {
            for (String part : raw.split(",")) {
                String value = part.trim();
                if (value.isEmpty()) {
                    continue;
                }
                if (!TIME_PATTERN.matcher(value).matches()) {
                    throw new IllegalArgumentException("Send time must be in HH:mm 24-hour format: " + value);
                }
                times.add(value);
            }
        }
        if (times.isEmpty()) {
            times.add(DEFAULT_SEND_TIME);
        }
        return String.join(",", times);
    }

    private ScheduledNotification require(UUID snId) {
        if (snId == null) {
            throw new IllegalArgumentException("Scheduled notification id is required");
        }
        return notificationRepository.findById(snId)
                .orElseThrow(() -> new IllegalArgumentException("Scheduled notification not found: " + snId));
    }

    private String normalizeChannels(String raw) {
        Set<String> channels = new LinkedHashSet<>();
        if (raw != null) {
            for (String part : raw.split(",")) {
                String channel = part.trim().toUpperCase();
                if (ScheduledNotification.CHANNEL_SMS.equals(channel) || ScheduledNotification.CHANNEL_EMAIL.equals(channel)) {
                    channels.add(channel);
                }
            }
        }
        if (channels.isEmpty()) {
            throw new IllegalArgumentException("Channels must include at least one of SMS or EMAIL");
        }
        return String.join(",", channels);
    }

    private String normalizeRecipients(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        List<String> numbers = new ArrayList<>();
        for (String part : raw.split(",")) {
            if (part.isBlank()) {
                continue;
            }
            String msisdn = normalizeMsisdn(part);
            if (!numbers.contains(msisdn)) {
                numbers.add(msisdn);
            }
        }
        return String.join(",", numbers);
    }

    private String normalizeMsisdn(String raw) {
        String digits = raw.trim().replaceAll("[\\s+()\\-]", "");
        if (digits.startsWith("0")) {
            digits = "254" + digits.substring(1);
        } else if (digits.length() == 9 && (digits.startsWith("7") || digits.startsWith("1"))) {
            digits = "254" + digits;
        }
        if (!MSISDN_PATTERN.matcher(digits).matches()) {
            throw new IllegalArgumentException("Invalid phone number: " + raw);
        }
        return digits;
    }

    private String normalizeEmails(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        List<String> emails = new ArrayList<>();
        for (String part : raw.split(",")) {
            String email = part.trim();
            if (email.isEmpty()) {
                continue;
            }
            if (!EMAIL_PATTERN.matcher(email).matches()) {
                throw new IllegalArgumentException("Invalid email address: " + email);
            }
            if (!emails.contains(email)) {
                emails.add(email);
            }
        }
        return String.join(",", emails);
    }

    private List<String> split(String csv) {
        List<String> values = new ArrayList<>();
        if (csv == null || csv.isBlank()) {
            return values;
        }
        for (String part : csv.split(",")) {
            String value = part.trim();
            if (!value.isEmpty()) {
                values.add(value);
            }
        }
        return values;
    }

    private String trimToNull(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
