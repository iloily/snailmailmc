package com.snailmailmc.plugin.storage;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * The plugin's SQLite file. One shared connection; callers synchronize on it.
 * Paper ships the SQLite driver, so nothing extra is bundled.
 */
public final class Database implements AutoCloseable {

    private final Connection connection;

    public Database(Path file) throws SQLException {
        this.connection = DriverManager.getConnection("jdbc:sqlite:" + file.toAbsolutePath());
        try (Statement st = connection.createStatement()) {
            st.execute("PRAGMA journal_mode=WAL");
            st.execute("""
                    CREATE TABLE IF NOT EXISTS balances (
                        player TEXT PRIMARY KEY,
                        shells INTEGER NOT NULL DEFAULT 0
                    )""");
            st.execute("""
                    CREATE TABLE IF NOT EXISTS letters (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        sender TEXT NOT NULL,
                        sender_name TEXT NOT NULL,
                        recipient TEXT NOT NULL,
                        body TEXT NOT NULL,
                        item BLOB,
                        sent_at INTEGER NOT NULL,
                        deliver_at INTEGER NOT NULL,
                        opened INTEGER NOT NULL DEFAULT 0,
                        item_taken INTEGER NOT NULL DEFAULT 0,
                        notified INTEGER NOT NULL DEFAULT 0
                    )""");
            st.execute("CREATE INDEX IF NOT EXISTS letters_recipient ON letters (recipient, deliver_at)");
        }
    }

    public Connection connection() {
        return connection;
    }

    @Override
    public void close() throws SQLException {
        connection.close();
    }
}
