package com.spa.smart_gate_springboot.notifications;

import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
public enum NotificationFrequency {

    DAILY("Daily"),
    WEEKLY("Weekly"),
    MONTHLY("Monthly"),
    EVERY_2_MONTHS("Every 2 Months"),
    EVERY_3_MONTHS("Every 3 Months"),
    EVERY_6_MONTHS("Every 6 Months"),
    YEARLY("Yearly"),
    CUSTOM_DAYS("Custom (every N days)");

    private final String label;

    NotificationFrequency(String label) {
        this.label = label;
    }

    // Steps from the original start, never iteratively: repeated +1 month drifts off month-ends
    // (Jan 31 -> Feb 28 -> Mar 28), while start.plusMonths(k) keeps landing on the 31st.
    public LocalDate advance(LocalDate start, int steps, Integer intervalDays) {
        switch (this) {
            case DAILY:
                return start.plusDays(steps);
            case WEEKLY:
                return start.plusWeeks(steps);
            case MONTHLY:
                return start.plusMonths(steps);
            case EVERY_2_MONTHS:
                return start.plusMonths(2L * steps);
            case EVERY_3_MONTHS:
                return start.plusMonths(3L * steps);
            case EVERY_6_MONTHS:
                return start.plusMonths(6L * steps);
            case YEARLY:
                return start.plusYears(steps);
            case CUSTOM_DAYS:
                return start.plusDays((long) steps * (intervalDays == null || intervalDays < 1 ? 1 : intervalDays));
            default:
                return start.plusDays(steps);
        }
    }

    public LocalDateTime advance(LocalDateTime from, Integer intervalDays) {
        switch (this) {
            case DAILY:
                return from.plusDays(1);
            case WEEKLY:
                return from.plusWeeks(1);
            case MONTHLY:
                return from.plusMonths(1);
            case EVERY_2_MONTHS:
                return from.plusMonths(2);
            case EVERY_3_MONTHS:
                return from.plusMonths(3);
            case EVERY_6_MONTHS:
                return from.plusMonths(6);
            case YEARLY:
                return from.plusYears(1);
            case CUSTOM_DAYS:
                return from.plusDays(intervalDays == null || intervalDays < 1 ? 1 : intervalDays);
            default:
                return from.plusDays(1);
        }
    }

    public static NotificationFrequency parse(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Unknown frequency: " + raw);
        }
        try {
            return valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown frequency: " + raw);
        }
    }
}
