package com.minefix.tgbotplugin;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.*;
import java.time.Instant;
import java.util.UUID;

public class SqliteDataStore implements DataStore {
    private final JavaPlugin plugin;
    private Connection conn;

    public SqliteDataStore(JavaPlugin plugin) { this.plugin = plugin; }

    @Override
    public void init() {
        try {
            File dbFile = new File(plugin.getDataFolder(), "players.db");
            plugin.getDataFolder().mkdirs();
            String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();
            conn = DriverManager.getConnection(url);
            try (Statement s = conn.createStatement()) {
                s.execute("CREATE TABLE IF NOT EXISTS players(uuid TEXT PRIMARY KEY, name TEXT, password TEXT, telegram_chat BIGINT, authorized INTEGER, locked INTEGER, last_ip TEXT)");
                s.execute("CREATE TABLE IF NOT EXISTS approvals(id TEXT PRIMARY KEY, uuid TEXT, ip TEXT, expires_at INTEGER, created_at INTEGER)");
                s.execute("CREATE TABLE IF NOT EXISTS link_codes(code TEXT PRIMARY KEY, uuid TEXT, expires_at INTEGER)");
            }
        } catch (SQLException ex) {
            plugin.getLogger().severe("Не удалось подключиться к SQLite: " + ex.getMessage());
        }
    }

    @Override
    public void close() {
        try { if (conn != null) conn.close(); } catch (SQLException ignored) {}
    }

    @Override
    public synchronized boolean isRegistered(UUID uuid) {
        try (PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM players WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        } catch (SQLException e) { plugin.getLogger().warning(e.getMessage()); }
        return false;
    }

    @Override
    public synchronized void savePlayerPassword(UUID uuid, String name, String bcryptHash) {
        try (PreparedStatement ps = conn.prepareStatement("INSERT OR REPLACE INTO players(uuid,name,password,authorized,locked) VALUES(?,?,?,?,?)")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, name);
            ps.setString(3, bcryptHash);
            ps.setInt(4, 1);
            ps.setInt(5, 0);
            ps.executeUpdate();
        } catch (SQLException e) { plugin.getLogger().warning(e.getMessage()); }
    }

    @Override
    public synchronized PlayerData getPlayerData(UUID uuid) {
        try (PreparedStatement ps = conn.prepareStatement("SELECT uuid,name,password,telegram_chat,locked FROM players WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                String name = rs.getString("name");
                String pass = rs.getString("password");
                long chat = rs.getLong("telegram_chat");
                boolean hasChat = !rs.wasNull();
                boolean locked = rs.getInt("locked") != 0;
                return new PlayerData(uuid, name, pass, hasChat ? chat : null, locked);
            }
        } catch (SQLException e) { plugin.getLogger().warning(e.getMessage()); }
        return null;
    }

    @Override
    public synchronized void setAuthorized(UUID uuid, boolean authorized) {
        try (PreparedStatement ps = conn.prepareStatement("UPDATE players SET authorized = ? WHERE uuid = ?")) {
            ps.setInt(1, authorized ? 1 : 0);
            ps.setString(2, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) { plugin.getLogger().warning(e.getMessage()); }
    }

    @Override
    public synchronized boolean isAuthorized(UUID uuid) {
        try (PreparedStatement ps = conn.prepareStatement("SELECT authorized FROM players WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return rs.getInt("authorized") != 0; }
        } catch (SQLException e) { plugin.getLogger().warning(e.getMessage()); }
        return false;
    }

    @Override
    public void registerFailedAttempt(UUID uuid) {
        // stub: could implement counters and lock
    }

    @Override
    public synchronized String createLoginApproval(UUID uuid, String ip, long ttlSeconds) {
        String id = UUID.randomUUID().toString();
        long expires = Instant.now().getEpochSecond() + ttlSeconds;
        try (PreparedStatement ps = conn.prepareStatement("INSERT INTO approvals(id,uuid,ip,expires_at,created_at) VALUES(?,?,?,?,?)")) {
            ps.setString(1, id);
            ps.setString(2, uuid.toString());
            ps.setString(3, ip);
            ps.setLong(4, expires);
            ps.setLong(5, Instant.now().getEpochSecond());
            ps.executeUpdate();
            return id;
        } catch (SQLException e) { plugin.getLogger().warning(e.getMessage()); }
        return null;
    }

    @Override
    public synchronized PendingApproval getApprovalById(String id) {
        try (PreparedStatement ps = conn.prepareStatement("SELECT id,uuid,ip,expires_at FROM approvals WHERE id = ?")) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                UUID uuid = UUID.fromString(rs.getString("uuid"));
                String ip = rs.getString("ip");
                long expires = rs.getLong("expires_at");
                return new PendingApproval(id, uuid, null, ip, expires);
            }
        } catch (SQLException e) { plugin.getLogger().warning(e.getMessage()); }
        return null;
    }

    @Override
    public synchronized void completeApproval(String id, boolean accepted) {
        try {
            if (accepted) {
                PendingApproval p = getApprovalById(id);
                if (p != null) setAuthorized(p.getPlayerUuid(), true);
            }
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM approvals WHERE id = ?")) {
                ps.setString(1, id);
                ps.executeUpdate();
            }
        } catch (SQLException e) { plugin.getLogger().warning(e.getMessage()); }
    }

    @Override
    public synchronized String createLinkCode(UUID uuid, long ttlSeconds) {
        String code = UUID.randomUUID().toString().substring(0, 8);
        long expires = Instant.now().getEpochSecond() + ttlSeconds;
        try (PreparedStatement ps = conn.prepareStatement("INSERT INTO link_codes(code,uuid,expires_at) VALUES(?,?,?)")) {
            ps.setString(1, code);
            ps.setString(2, uuid.toString());
            ps.setLong(3, expires);
            ps.executeUpdate();
            return code;
        } catch (SQLException e) { plugin.getLogger().warning(e.getMessage()); }
        return null;
    }

    @Override
    public synchronized UUID consumeLinkCode(String code) {
        try (PreparedStatement ps = conn.prepareStatement("SELECT uuid,expires_at FROM link_codes WHERE code = ?")) {
            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                long expires = rs.getLong("expires_at");
                if (Instant.now().getEpochSecond() > expires) {
                    // expired
                    try (PreparedStatement d = conn.prepareStatement("DELETE FROM link_codes WHERE code = ?")) { d.setString(1, code); d.executeUpdate(); }
                    return null;
                }
                UUID uuid = UUID.fromString(rs.getString("uuid"));
                try (PreparedStatement d = conn.prepareStatement("DELETE FROM link_codes WHERE code = ?")) { d.setString(1, code); d.executeUpdate(); }
                return uuid;
            }
        } catch (SQLException e) { plugin.getLogger().warning(e.getMessage()); }
        return null;
    }
}
