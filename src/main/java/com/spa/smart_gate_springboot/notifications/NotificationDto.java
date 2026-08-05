package com.spa.smart_gate_springboot.notifications;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDto {

    private String snName;
    private String snSubject;
    private String snMessage;
    private String snFrequency;
    private Integer snIntervalDays;
    private String snSendTime;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate snStartDate;

    private String snChannels;
    private String snRecipients;
    private String snEmails;
}
