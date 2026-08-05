package com.spa.smart_gate_springboot.notifications;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@ToString
@Table(schema = "msg")
@Entity(name = "msg_scheduled_notification")
@Builder
public class ScheduledNotification {

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_PAUSED = "PAUSED";
    public static final String STATUS_DELETED = "DELETED";

    public static final String CHANNEL_SMS = "SMS";
    public static final String CHANNEL_EMAIL = "EMAIL";

    @Id
    @GeneratedValue
    private UUID snId;

    // Immutable identity for seeded rows. snName is user-editable, so it can't be the seed guard.
    @Column(updatable = false)
    private String snSeedKey;

    private String snName;
    private String snSubject;

    @Column(columnDefinition = "TEXT")
    private String snMessage;

    private String snFrequency;
    private Integer snIntervalDays;
    // Comma-separated "HH:mm" list. Reuses the original single-value column so no prod DDL
    // or backfill is needed — an existing "09:00" is already a valid one-entry list.
    @Column(name = "sn_send_time")
    private String snSendTimes;
    private LocalDate snStartDate;
    private String snChannels;

    @Column(columnDefinition = "TEXT")
    private String snRecipients;

    @Column(columnDefinition = "TEXT")
    private String snEmails;

    // Plain String, never @Enumerated: on ddl-auto=update Hibernate never rebuilds the Postgres
    // CHECK constraint, so adding a status later would break every insert in prod.
    private String snStatus;

    private LocalDateTime snNextRunAt;
    private LocalDateTime snLastRunAt;
    private String snLastStatus;

    @Builder.Default
    private Integer snRunCount = 0;

    @Column(updatable = false)
    private UUID snCreatedById;

    @Column(updatable = false)
    private String snCreatedByName;

    @Column(updatable = false)
    private LocalDateTime snCreatedOn;

    @Column(insertable = false)
    private UUID snUpdatedById;

    @Column(insertable = false)
    private LocalDateTime snUpdatedOn;
}
