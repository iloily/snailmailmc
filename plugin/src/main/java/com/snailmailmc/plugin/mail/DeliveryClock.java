package com.snailmailmc.plugin.mail;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Snail mail: a letter arrives at the next delivery hour (6 AM by default)
 * after it was sent, in the server's town time zone. A letter sent at 9 PM
 * arrives the next morning; one sent at 3 AM arrives at 6 AM that same morning.
 */
public final class DeliveryClock {

    private final ZoneId zone;
    private final int deliveryHour;

    public DeliveryClock(ZoneId zone, int deliveryHour) {
        if (deliveryHour < 0 || deliveryHour > 23) {
            throw new IllegalArgumentException("delivery-hour must be 0-23");
        }
        this.zone = zone;
        this.deliveryHour = deliveryHour;
    }

    public Instant deliveryTime(Instant sentAt) {
        ZonedDateTime sent = sentAt.atZone(zone);
        ZonedDateTime sameDay = sent.toLocalDate().atTime(LocalTime.of(deliveryHour, 0)).atZone(zone);
        ZonedDateTime delivery = sameDay.isAfter(sent) ? sameDay : sameDay.plusDays(1);
        return delivery.toInstant();
    }

    public ZoneId zone() {
        return zone;
    }
}
