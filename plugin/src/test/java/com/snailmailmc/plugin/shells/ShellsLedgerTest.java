package com.snailmailmc.plugin.shells;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.snailmailmc.plugin.storage.Database;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ShellsLedgerTest {

    @TempDir
    Path dir;

    private Database db;
    private ShellsLedger ledger;
    private final UUID ilo = UUID.randomUUID();
    private final UUID cookie = UUID.randomUUID();

    @BeforeEach
    void setUp() throws Exception {
        db = new Database(dir.resolve("test.db"));
        ledger = new ShellsLedger(db, 5);
    }

    @AfterEach
    void tearDown() throws Exception {
        db.close();
    }

    @Test
    void newAccountGetsStartingBalanceOnce() {
        assertTrue(ledger.ensureAccount(ilo));
        ledger.deposit(ilo, 10);
        assertFalse(ledger.ensureAccount(ilo));
        assertEquals(15, ledger.balance(ilo));
    }

    @Test
    void withdrawRefusesToGoNegative() {
        ledger.ensureAccount(ilo);
        assertFalse(ledger.withdraw(ilo, 6));
        assertEquals(5, ledger.balance(ilo));
        assertTrue(ledger.withdraw(ilo, 5));
        assertEquals(0, ledger.balance(ilo));
    }

    @Test
    void transferMovesShellsAtomically() {
        ledger.ensureAccount(ilo);
        ledger.ensureAccount(cookie);
        assertTrue(ledger.transfer(ilo, cookie, 3));
        assertEquals(2, ledger.balance(ilo));
        assertEquals(8, ledger.balance(cookie));
        assertFalse(ledger.transfer(ilo, cookie, 3));
        assertEquals(2, ledger.balance(ilo));
        assertEquals(8, ledger.balance(cookie));
    }

    @Test
    void rejectsZeroNegativeAndSelfPayments() {
        ledger.ensureAccount(ilo);
        assertThrows(IllegalArgumentException.class, () -> ledger.deposit(ilo, 0));
        assertThrows(IllegalArgumentException.class, () -> ledger.withdraw(ilo, -1));
        assertThrows(IllegalArgumentException.class, () -> ledger.transfer(ilo, ilo, 1));
        assertThrows(IllegalArgumentException.class, () -> ledger.set(ilo, -1));
    }

    @Test
    void balancesSurviveReopen() throws Exception {
        ledger.ensureAccount(ilo);
        ledger.set(ilo, 42);
        db.close();
        db = new Database(dir.resolve("test.db"));
        assertEquals(42, new ShellsLedger(db, 5).balance(ilo));
    }

    @Test
    void parsesAmounts() {
        assertEquals(1500, ShellsFormat.parseAmount("1,500"));
        assertEquals(-1, ShellsFormat.parseAmount("0"));
        assertEquals(-1, ShellsFormat.parseAmount("2.5"));
        assertEquals(-1, ShellsFormat.parseAmount("lots"));
        assertEquals("1 Shell", new ShellsFormat("Shell", "Shells").format(1));
        assertEquals("12 Shells", new ShellsFormat("Shell", "Shells").format(12));
    }
}
