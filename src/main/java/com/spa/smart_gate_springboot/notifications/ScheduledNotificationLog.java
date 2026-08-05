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

import java.time.LocalDateTime;
import java.util.UUID;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@ToString
@Table(schema = "msg")
@Entity(name = "msg_scheduled_notification_log")
@Builder
public class ScheduledNotificationLog {

    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_PARTIAL = "PARTIAL";
    public static final String STATUS_FAILED = "FAILED";

    public static final String TRIGGER_CRON = "CRON";

    @Id
    @GeneratedValue
    private UUID snlId;

    private UUID snlNotificationId;
    private String snlNotificationName;
    private LocalDateTime snlRunAt;
    private String snlStatus;
    private int snlSmsSent;
    private int snlSmsFailed;
    private int snlEmailSent;
    private int snlEmailFailed;

    @Column(columnDefinition = "TEXT")
    private String snlDetail;

    private String snlTriggeredBy;
}
