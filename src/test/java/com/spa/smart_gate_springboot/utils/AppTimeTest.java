package com.spa.smart_gate_springboot.utils;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Locks in the property the whole platform depends on: every timestamp we write is Africa/Nairobi
 * wall-clock, produced by the JVM, independent of the host's or the database's time zone. The bug this
 * guards against is a created date landing three hours behind EAT (i.e. UTC).
 */
class AppTimeTest {

    private final TimeZone original = TimeZone.getDefault();

    @AfterEach
    void restoreDefaultZone() {
        TimeZone.setDefault(original);
    }

    @Test
    void nowIsNairobiWallClockEvenWhenTheJvmDefaultIsWrong() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

        LocalDateTime expected = ZonedDateTime.now(ZoneId.of("Africa/Nairobi")).toLocalDateTime();

        assertTrue(Duration.between(AppTime.now(), expected).abs().toSeconds() < 2,
                "AppTime.now() must not follow the JVM default zone");
        assertEquals(expected.toLocalDate(), AppTime.today());
    }

    @Test
    void installPinsTheJvmDefaultForHibernateAndPgjdbc() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

        AppTime.install();

        assertEquals(ZoneId.of("Africa/Nairobi"), ZoneId.systemDefault());
        assertEquals("Africa/Nairobi", System.getProperty("user.timezone"));
    }

    /**
     * The legacy {@code java.util.Date} fields (MsgQueue, ApiKey, report filters) must round-trip to the
     * same EAT wall-clock the LocalDateTime fields carry — no silent three-hour shift between the two.
     */
    @Test
    void legacyDateRoundTripsToTheSameWallClock() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

        Date now = AppTime.nowDate();

        assertTrue(Duration.between(AppTime.toLocal(now), AppTime.now()).abs().toSeconds() < 2,
                "nowDate() and now() must denote the same instant in EAT");
        assertEquals(LocalDateTime.of(2026, 8, 28, 14, 35),
                AppTime.toLocal(AppTime.toDate(LocalDateTime.of(2026, 8, 28, 14, 35))));
    }
}
