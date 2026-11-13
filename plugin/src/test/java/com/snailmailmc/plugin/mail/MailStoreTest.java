package com.snailmailmc.plugin.mail;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.snailmailmc.plugin.storage.Database;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MailStoreTest {

    @TempDir
    Path dir;

    private Database db;
    private MailStore store;
    private final UUID ilo = UUID.randomUUID();
    private final UUID cookie = UUID.randomUUID();
    private final Instant now = Instant.parse("2026-11-13T02:00:00Z");
    private final Instant morning = Instant.parse("2026-11-13T11:00:00Z");

    @BeforeEach
    void setUp() throws Exception {
        db = new Database(dir.resolve("mail.db"));
        store = new MailStore(db);
    }

    @AfterEach
    void tearDown() throws Exception {
        db.close();
    }

    @Test
    void lettersStayHiddenUntilDelivered() {
        store.send(ilo, "ilo", cookie, "Hi Cookie!", null, now, morning);
        assertTrue(store.mailbox(cookie, now).isEmpty());
        assertEquals(1, store.inTransitFrom(ilo, now));
        List<Letter> box = store.mailbox(cookie, morning);
        assertEquals(1, box.size());
        assertEquals("Hi Cookie!", box.get(0).body());
        assertEquals(0, store.inTransitFrom(ilo, morning));
    }

    @Test
    void giftCanOnlyBeTakenOnceAndOnlyByRecipient() {
        byte[] item = {1, 2, 3};
        long id = store.send(ilo, "ilo", cookie, "A gift", item, now, morning);
        assertTrue(store.takeItem(id, ilo).isEmpty());
        assertArrayEquals(item, store.takeItem(id, cookie).orElseThrow());
        assertTrue(store.takeItem(id, cookie).isEmpty());
        assertTrue(!store.find(id).orElseThrow().hasGift());
    }

    @Test
    void deliveryNoticeFiresOncePerLetter() {
        store.send(ilo, "ilo", cookie, "One", null, now, morning);
        store.send(ilo, "ilo", cookie, "Two", null, now, morning.plus(Duration.ofDays(1)));
        assertEquals(List.of(cookie), store.takeNewlyDelivered(morning));
        assertTrue(store.takeNewlyDelivered(morning).isEmpty());
        assertEquals(List.of(cookie), store.takeNewlyDelivered(morning.plus(Duration.ofDays(1))));
    }

    @Test
    void unopenedCountDropsWhenRead() {
        long id = store.send(ilo, "ilo", cookie, "Read me", null, now, morning);
        assertEquals(1, store.unopenedCount(cookie, morning));
        store.markOpened(id);
        assertEquals(0, store.unopenedCount(cookie, morning));
    }
}
