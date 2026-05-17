package com.minefix.tgbotplugin;

import java.sql.*;

public class SqliteDataStore {
    private static Connection connection;

    public static void initialize(String path) {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + path);
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("CREATE TABLE IF NOT EXISTS tg_users (" +
                        "nickname TEXT PRIMARY KEY, " +
                        "chat_id LONG, " +
                        "last_ip TEXT, " +
                        "two_fa INT DEFAULT 0)");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Connection getConnection() { return connection; }

    public static String getNickByChatId(long chatId) {
        String sql = "SELECT nickname FROM tg_users WHERE chat_id = ?";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setLong(1, chatId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getString("nickname");
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    public static void bindAccount(String nickname, long chatId) {
        String sql = "INSERT INTO tg_users(nickname, chat_id) VALUES(?, ?) ON CONFLICT(nickname) DO UPDATE SET chat_id = ?";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, nickname);
            pstmt.setLong(2, chatId);
            pstmt.setLong(3, chatId);
            pstmt.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public static String getLastIp(String nickname) {
        String sql = "SELECT last_ip FROM tg_users WHERE nickname = ?";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, nickname);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getString("last_ip");
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    public static void updateLastIp(String nickname, String ip) {
        String sql = "INSERT INTO tg_users(nickname, last_ip) VALUES(?, ?) ON CONFLICT(nickname) DO UPDATE SET last_ip = ?";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, nickname);
            pstmt.setString(2, ip);
            pstmt.setString(3, ip);
            pstmt.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public static boolean is2faEnabled(String nickname) {
        String sql = "SELECT two_fa FROM tg_users WHERE nickname = ?";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, nickname);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getInt("two_fa") == 1;
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    public static void set2faStatus(String nickname, boolean enabled) {
        String sql = "INSERT INTO tg_users(nickname, two_fa) VALUES(?, ?) ON CONFLICT(nickname) DO UPDATE SET two_fa = ?";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, nickname);
            maxInt(pstmt, enabled ? 1 : 0);
            pstmt.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private static void maxInt(PreparedStatement p, int val) throws SQLException {
        p.setInt(2, val);
        p.setInt(3, val);
    }

    public static Long getChatIdByNick(String nickname) {
        String sql = "SELECT chat_id FROM tg_users WHERE nickname = ?";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, nickname);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    long id = rs.getLong("chat_id");
                    return id == 0 ? null : id;
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }
}
