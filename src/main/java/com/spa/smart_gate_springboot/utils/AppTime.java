package com.spa.smart_gate_springboot.utils;

import lombok.extern.slf4j.Slf4j;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.TimeZone;

/**
 * The single source of "now" for this service.
 *
 * <p>Every timestamp this application writes — {@code msg.message_queue_arc} created/delivered dates,
 * invoices, credits, wallets, API keys, schedules, notifications, JWT issued/expiry — is produced here,
 * from the JVM clock in {@link #ZONE}. <b>The database clock is never consulted:</b> no query uses
 * {@code now()} / {@code current_timestamp} / {@code current_date}, and no column carries a
 * {@code DEFAULT now()}. Those read the Postgres session zone, which is UTC on a stock RDS instance —
 * that is exactly how a created date ends up three hours behind East Africa.
 *
 * <p>Call {@link #now()} rather than {@code LocalDateTime.now()}. The bare JDK call reads
 * {@link ZoneId#systemDefault()}, a process-global that a container base image, a stray
 * {@code -Duser.timezone}, or another library can change; {@link #now()} names the zone explicitly and
 * cannot drift.
 *
 * <p>{@link #install()} still pins the JVM default as well, because things that read it cannot be
 * parameterised: Hibernate's {@code @CreationTimestamp}/{@code @UpdateTimestamp} (VM clock, captured at
 * bootstrap), {@code SimpleDateFormat}, pgjdbc's binding of {@link Date}, and log timestamps.
 *
 * <p><b>Storage note.</b> All the Postgres columns involved are {@code timestamp without time zone}
 * and the Java fields are {@link LocalDateTime} or {@link Date}, so pgjdbc writes the wall-clock value
 * verbatim — the server's own {@code TimeZone} setting never re-interprets it. The values in the DB
 * are EAT. The SMPP gateway ({@code sms_smmp_gateway}) pins the same zone and reads
 * {@code msg.message_queue_arc} across the shared database; that contract breaks if this changes.
 */
@Slf4j
public final class AppTime {

    /** East Africa Time. Every timestamp this service produces is wall-clock in this zone. */
    public static final ZoneId ZONE = ZoneId.of("Africa/Nairobi");

    private static final Clock CLOCK = Clock.system(ZONE);

    private AppTime() {
    }

    /**
     * Pin the JVM default time zone to {@link #ZONE}. Must run before Spring starts, i.e. as the first
     * statement of {@code main()} — Hibernate captures the VM clock during bootstrap, so setting it
     * later leaves the auditing timestamps on the old zone.
     */
    public static void install() {
        TimeZone.setDefault(timeZone());
        System.setProperty("user.timezone", ZONE.getId());
        log.info("Application time zone pinned to {} (JVM default now {}, current time {})",
                ZONE, ZoneId.systemDefault(), now());
    }

    /** Current wall-clock time in {@link #ZONE}. */
    public static LocalDateTime now() {
        return LocalDateTime.now(CLOCK);
    }

    /** Current date in {@link #ZONE}. */
    public static LocalDate today() {
        return LocalDate.now(CLOCK);
    }

    /** Current instant as a legacy {@link Date} — for the fields still typed {@code java.util.Date}. */
    public static Date nowDate() {
        return Date.from(CLOCK.instant());
    }

    /** The clock backing {@link #now()} — for anything that takes a {@link Clock} directly. */
    public static Clock clock() {
        return CLOCK;
    }

    /** {@link #ZONE} as a legacy {@link TimeZone} — for {@code Calendar} / {@code SimpleDateFormat}. */
    public static TimeZone timeZone() {
        return TimeZone.getTimeZone(ZONE);
    }

    /** Convert a legacy {@link Date} to EAT wall-clock. Null-safe. */
    public static LocalDateTime toLocal(Date date) {
        return date == null ? null : LocalDateTime.ofInstant(date.toInstant(), ZONE);
    }

    /** Convert an {@link Instant} to EAT wall-clock. Null-safe. */
    public static LocalDateTime toLocal(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, ZONE);
    }

    /** Convert EAT wall-clock back to a legacy {@link Date}. Null-safe. */
    public static Date toDate(LocalDateTime dateTime) {
        return dateTime == null ? null : Date.from(dateTime.atZone(ZONE).toInstant());
    }
}
