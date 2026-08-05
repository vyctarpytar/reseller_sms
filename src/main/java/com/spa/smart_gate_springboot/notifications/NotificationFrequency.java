package com.spa.smart_gate_springboot.notifications;

import lombok.Getter;

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
