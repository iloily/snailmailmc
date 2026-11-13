package com.snailmailmc.plugin.mail;

import com.snailmailmc.plugin.storage.Database;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Letters in the post. Only letters whose delivery time has passed ever show up in a mailbox. */
public final class MailStore {

    private final Connection db;

    public MailStore(Database database) {
        this.db = database.connection();
    }

    public long send(UUID sender, String senderName, UUID recipient, String body, byte[] item,
                     Instant sentAt, Instant deliverAt) {
        synchronized (db) {
            try (PreparedStatement ps = db.prepareStatement("""
                    INSERT INTO letters (sender, sender_name, recipient, body, item, sent_at, deliver_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?)""", Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, sender.toString());
                ps.setString(2, senderName);
                ps.setString(3, recipient.toString());
                ps.setString(4, body);
                ps.setBytes(5, item);
                ps.setLong(6, sentAt.toEpochMilli());
                ps.setLong(7, deliverAt.toEpochMilli());
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    keys.next();
                    return keys.getLong(1);
                }
            } catch (SQLException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    /** Delivered letters, newest first. */
    public List<Letter> mailbox(UUID recipient, Instant now) {
        synchronized (db) {
            try (PreparedStatement ps = db.prepareStatement("""
                    SELECT * FROM letters WHERE recipient = ? AND deliver_at <= ?
                    ORDER BY deliver_at DESC, id DESC""")) {
                ps.setString(1, recipient.toString());
                ps.setLong(2, now.toEpochMilli());
                return readAll(ps);
            } catch (SQLException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    public int unopenedCount(UUID recipient, Instant now) {
        synchronized (db) {
            try (PreparedStatement ps = db.prepareStatement(
                    "SELECT COUNT(*) FROM letters WHERE recipient = ? AND deliver_at <= ? AND opened = 0")) {
                ps.setString(1, recipient.toString());
                ps.setLong(2, now.toEpochMilli());
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? rs.getInt(1) : 0;
                }
            } catch (SQLException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    /** Letters waiting to be posted (sent but not yet delivered) from this sender. */
    public int inTransitFrom(UUID sender, Instant now) {
        synchronized (db) {
            try (PreparedStatement ps = db.prepareStatement(
                    "SELECT COUNT(*) FROM letters WHERE sender = ? AND deliver_at > ?")) {
                ps.setString(1, sender.toString());
                ps.setLong(2, now.toEpochMilli());
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? rs.getInt(1) : 0;
                }
            } catch (SQLException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    /**
     * Recipients with letters delivered since they were last told, and marks those letters as told.
     * Used to announce "you've got mail" once per letter.
     */
    public List<UUID> takeNewlyDelivered(Instant now) {
        synchronized (db) {
            try {
                List<UUID> recipients = new ArrayList<>();
                try (PreparedStatement ps = db.prepareStatement(
                        "SELECT DISTINCT recipient FROM letters WHERE deliver_at <= ? AND notified = 0")) {
                    ps.setLong(1, now.toEpochMilli());
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            recipients.add(UUID.fromString(rs.getString(1)));
                        }
                    }
                }
                try (PreparedStatement ps = db.prepareStatement(
                        "UPDATE letters SET notified = 1 WHERE deliver_at <= ? AND notified = 0")) {
                    ps.setLong(1, now.toEpochMilli());
                    ps.executeUpdate();
                }
                return recipients;
            } catch (SQLException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    public void markOpened(long letterId) {
        update("UPDATE letters SET opened = 1 WHERE id = ?", letterId);
    }

    /**
     * Hands out the attached item exactly once. Returns the item bytes, or empty if
     * there was no item or it was already taken.
     */
    public Optional<byte[]> takeItem(long letterId, UUID recipient) {
        synchronized (db) {
            try (PreparedStatement select = db.prepareStatement(
                    "SELECT item FROM letters WHERE id = ? AND recipient = ? AND item IS NOT NULL AND item_taken = 0");
                 PreparedStatement mark = db.prepareStatement(
                         "UPDATE letters SET item_taken = 1 WHERE id = ? AND item_taken = 0")) {
                select.setLong(1, letterId);
                select.setString(2, recipient.toString());
                byte[] item;
                try (ResultSet rs = select.executeQuery()) {
                    if (!rs.next()) {
                        return Optional.empty();
                    }
                    item = rs.getBytes(1);
                }
                mark.setLong(1, letterId);
                return mark.executeUpdate() == 1 ? Optional.of(item) : Optional.empty();
            } catch (SQLException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    public Optional<Letter> find(long letterId) {
        synchronized (db) {
            try (PreparedStatement ps = db.prepareStatement("SELECT * FROM letters WHERE id = ?")) {
                ps.setLong(1, letterId);
                List<Letter> found = readAll(ps);
                return found.stream().findFirst();
            } catch (SQLException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    private void update(String sql, long letterId) {
        synchronized (db) {
            try (PreparedStatement ps = db.prepareStatement(sql)) {
                ps.setLong(1, letterId);
                ps.executeUpdate();
            } catch (SQLException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    private static List<Letter> readAll(PreparedStatement ps) throws SQLException {
        List<Letter> letters = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                letters.add(new Letter(
                        rs.getLong("id"),
                        UUID.fromString(rs.getString("sender")),
                        rs.getString("sender_name"),
                        UUID.fromString(rs.getString("recipient")),
                        rs.getString("body"),
                        rs.getBytes("item"),
                        Instant.ofEpochMilli(rs.getLong("sent_at")),
                        Instant.ofEpochMilli(rs.getLong("deliver_at")),
                        rs.getInt("opened") == 1,
                        rs.getInt("item_taken") == 1));
            }
        }
        return letters;
    }
}
