package com.snailmailmc.plugin.mail;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.Test;

class DeliveryClockTest {

    private static final ZoneId NY = ZoneId.of("America/New_York");
    private final DeliveryClock clock = new DeliveryClock(NY, 6);

    private static Instant ny(int year, int month, int day, int hour, int minute) {
        return ZonedDateTime.of(year, month, day, hour, minute, 0, 0, NY).toInstant();
    }

    @Test
    void eveningLetterArrivesNextMorning() {
        assertEquals(ny(2026, 11, 13, 6, 0), clock.deliveryTime(ny(2026, 11, 12, 21, 0)));
    }

    @Test
    void earlyMorningLetterArrivesThatMorning() {
        assertEquals(ny(2026, 11, 13, 6, 0), clock.deliveryTime(ny(2026, 11, 13, 3, 0)));
    }

    @Test
    void letterSentExactlyAtDeliveryTimeWaitsForTomorrow() {
        assertEquals(ny(2026, 11, 14, 6, 0), clock.deliveryTime(ny(2026, 11, 13, 6, 0)));
    }

    @Test
    void handlesDaylightSavingChange() {
        // Clocks go back on Nov 1 2026 in New York; delivery is still 6 AM local.
        assertEquals(ny(2026, 11, 1, 6, 0), clock.deliveryTime(ny(2026, 10, 31, 22, 0)));
    }

    @Test
    void rejectsBadHour() {
        assertThrows(IllegalArgumentException.class, () -> new DeliveryClock(NY, 24));
    }
}
