package com.snailmailmc.plugin.news;

import com.snailmailmc.plugin.storage.Database;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** News posts and the blocks in the world that open the news board. */
public final class NewsStore {

    private final Connection db;

    public NewsStore(Database database) {
        this.db = database.connection();
    }

    public long post(String title, String body, String author, Instant at) {
        synchronized (db) {
            try (PreparedStatement ps = db.prepareStatement(
                    "INSERT INTO news (title, body, author, posted_at) VALUES (?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, title);
                ps.setString(2, body);
                ps.setString(3, author);
                ps.setLong(4, at.toEpochMilli());
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

    public boolean remove(long id) {
        synchronized (db) {
            try (PreparedStatement ps = db.prepareStatement("DELETE FROM news WHERE id = ?")) {
                ps.setLong(1, id);
                return ps.executeUpdate() == 1;
            } catch (SQLException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    /** Newest first. */
    public List<NewsPost> recent(int limit) {
        synchronized (db) {
            try (PreparedStatement ps = db.prepareStatement(
                    "SELECT * FROM news ORDER BY posted_at DESC, id DESC LIMIT ?")) {
                ps.setInt(1, limit);
                List<NewsPost> posts = new ArrayList<>();
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        posts.add(new NewsPost(rs.getLong("id"), rs.getString("title"), rs.getString("body"),
                                rs.getString("author"), Instant.ofEpochMilli(rs.getLong("posted_at"))));
                    }
                }
                return posts;
            } catch (SQLException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    public int countSince(Instant since) {
        synchronized (db) {
            try (PreparedStatement ps = db.prepareStatement("SELECT COUNT(*) FROM news WHERE posted_at > ?")) {
                ps.setLong(1, since.toEpochMilli());
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? rs.getInt(1) : 0;
                }
            } catch (SQLException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    public boolean bindBoard(String world, int x, int y, int z) {
        return boardUpdate("INSERT OR IGNORE INTO news_boards (world, x, y, z) VALUES (?, ?, ?, ?)", world, x, y, z);
    }

    public boolean unbindBoard(String world, int x, int y, int z) {
        return boardUpdate("DELETE FROM news_boards WHERE world = ? AND x = ? AND y = ? AND z = ?", world, x, y, z);
    }

    /** Every board block, as "world:x:y:z" keys. */
    public java.util.Set<String> allBoards() {
        synchronized (db) {
            try (PreparedStatement ps = db.prepareStatement("SELECT world, x, y, z FROM news_boards");
                 ResultSet rs = ps.executeQuery()) {
                java.util.Set<String> keys = new java.util.HashSet<>();
                while (rs.next()) {
                    keys.add(key(rs.getString(1), rs.getInt(2), rs.getInt(3), rs.getInt(4)));
                }
                return keys;
            } catch (SQLException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    public static String key(String world, int x, int y, int z) {
        return world + ":" + x + ":" + y + ":" + z;
    }

    public boolean isBoard(String world, int x, int y, int z) {
        synchronized (db) {
            try (PreparedStatement ps = db.prepareStatement(
                    "SELECT 1 FROM news_boards WHERE world = ? AND x = ? AND y = ? AND z = ?")) {
                fill(ps, world, x, y, z);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next();
                }
            } catch (SQLException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    private boolean boardUpdate(String sql, String world, int x, int y, int z) {
        synchronized (db) {
            try (PreparedStatement ps = db.prepareStatement(sql)) {
                fill(ps, world, x, y, z);
                return ps.executeUpdate() == 1;
            } catch (SQLException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    private static void fill(PreparedStatement ps, String world, int x, int y, int z) throws SQLException {
        ps.setString(1, world);
        ps.setInt(2, x);
        ps.setInt(3, y);
        ps.setInt(4, z);
    }
}
