package com.snailmailmc.plugin.shells;

import com.snailmailmc.plugin.storage.Database;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

/**
 * Shells balances. Shells are whole numbers and can never go below zero.
 * Every change runs in one SQLite transaction, so a transfer either fully
 * happens or not at all.
 */
public final class ShellsLedger {

    private final Connection db;
    private final long startingBalance;

    public ShellsLedger(Database database, long startingBalance) {
        this.db = database.connection();
        this.startingBalance = startingBalance;
    }

    /** Creates the player's account with the starting balance if they don't have one. Returns true if created. */
    public boolean ensureAccount(UUID player) {
        synchronized (db) {
            try (PreparedStatement ps = db.prepareStatement(
                    "INSERT OR IGNORE INTO balances (player, shells) VALUES (?, ?)")) {
                ps.setString(1, player.toString());
                ps.setLong(2, startingBalance);
                return ps.executeUpdate() == 1;
            } catch (SQLException e) {
                throw new LedgerException(e);
            }
        }
    }

    public boolean hasAccount(UUID player) {
        synchronized (db) {
            try (PreparedStatement ps = db.prepareStatement("SELECT 1 FROM balances WHERE player = ?")) {
                ps.setString(1, player.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next();
                }
            } catch (SQLException e) {
                throw new LedgerException(e);
            }
        }
    }

    public long balance(UUID player) {
        synchronized (db) {
            return read(player);
        }
    }

    public void deposit(UUID player, long amount) {
        requirePositive(amount);
        synchronized (db) {
            transaction(() -> write(player, Math.addExact(read(player), amount)));
        }
    }

    /** Takes shells if the player has enough. Returns false and changes nothing otherwise. */
    public boolean withdraw(UUID player, long amount) {
        requirePositive(amount);
        synchronized (db) {
            return transaction(() -> {
                long current = read(player);
                if (current < amount) {
                    return false;
                }
                write(player, current - amount);
                return true;
            });
        }
    }

    /** Moves shells between players. Returns false and changes nothing if the sender is short. */
    public boolean transfer(UUID from, UUID to, long amount) {
        requirePositive(amount);
        if (from.equals(to)) {
            throw new IllegalArgumentException("Can't pay yourself");
        }
        synchronized (db) {
            return transaction(() -> {
                long fromBalance = read(from);
                if (fromBalance < amount) {
                    return false;
                }
                write(from, fromBalance - amount);
                write(to, Math.addExact(read(to), amount));
                return true;
            });
        }
    }

    public void set(UUID player, long amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Balance can't be negative");
        }
        synchronized (db) {
            transaction(() -> write(player, amount));
        }
    }

    private long read(UUID player) {
        try (PreparedStatement ps = db.prepareStatement("SELECT shells FROM balances WHERE player = ?")) {
            ps.setString(1, player.toString());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(1) : startingBalance;
            }
        } catch (SQLException e) {
            throw new LedgerException(e);
        }
    }

    private boolean write(UUID player, long amount) {
        try (PreparedStatement ps = db.prepareStatement(
                "INSERT INTO balances (player, shells) VALUES (?, ?) "
                        + "ON CONFLICT(player) DO UPDATE SET shells = excluded.shells")) {
            ps.setString(1, player.toString());
            ps.setLong(2, amount);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            throw new LedgerException(e);
        }
    }

    private interface Work {
        boolean run();
    }

    private boolean transaction(Work work) {
        try {
            db.setAutoCommit(false);
            try {
                boolean result = work.run();
                db.commit();
                return result;
            } catch (RuntimeException e) {
                db.rollback();
                throw e;
            } finally {
                db.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new LedgerException(e);
        }
    }

    private static void requirePositive(long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be at least 1");
        }
    }

    public static final class LedgerException extends RuntimeException {
        LedgerException(Throwable cause) {
            super(cause);
        }
    }
}
