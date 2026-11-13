package com.snailmailmc.plugin.news;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.snailmailmc.plugin.storage.Database;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class NewsStoreTest {

    @TempDir
    Path dir;

    private Database db;
    private NewsStore store;

    @BeforeEach
    void setUp() throws Exception {
        db = new Database(dir.resolve("news.db"));
        store = new NewsStore(db);
    }

    @AfterEach
    void tearDown() throws Exception {
        db.close();
    }

    @Test
    void postsComeBackNewestFirstAndCanBeRemoved() {
        long first = store.post("Welcome", "The island is open!", "ilo", Instant.parse("2026-11-13T10:00:00Z"));
        long second = store.post("Fishing contest", "Saturday at the dock", "Cookie", Instant.parse("2026-11-14T10:00:00Z"));
        assertEquals(second, store.recent(10).get(0).id());
        assertEquals(1, store.recent(1).size());
        assertTrue(store.remove(first));
        assertFalse(store.remove(first));
        assertEquals(1, store.recent(10).size());
    }

    @Test
    void countsRecentPosts() {
        store.post("Old", "x", "ilo", Instant.parse("2026-11-01T10:00:00Z"));
        store.post("New", "y", "ilo", Instant.parse("2026-11-13T10:00:00Z"));
        assertEquals(1, store.countSince(Instant.parse("2026-11-12T10:00:00Z")));
    }

    @Test
    void boardBlocksBindOnceAndUnbind() {
        assertTrue(store.bindBoard("world", 10, 64, -3));
        assertFalse(store.bindBoard("world", 10, 64, -3));
        assertTrue(store.isBoard("world", 10, 64, -3));
        assertEquals(Set.of("world:10:64:-3"), store.allBoards());
        assertTrue(store.unbindBoard("world", 10, 64, -3));
        assertTrue(store.allBoards().isEmpty());
    }
}
