package com.snailmailmc.plugin.mail;

import java.time.Instant;
import java.util.UUID;

/** One letter. {@code item} is a serialized ItemStack, or null when nothing is attached. */
public record Letter(
        long id,
        UUID sender,
        String senderName,
        UUID recipient,
        String body,
        byte[] item,
        Instant sentAt,
        Instant deliverAt,
        boolean opened,
        boolean itemTaken) {

    public boolean hasGift() {
        return item != null && !itemTaken;
    }
}
