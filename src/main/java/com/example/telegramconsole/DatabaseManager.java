package com.example.telegramconsole;

import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class DatabaseManager {

    private Connection connection;
    private final Map<String, String> tempCodes = new HashMap<>(); // Имя игрока -> Код

    public DatabaseManager(JavaPlugin plugin) {
        File file = new File(plugin.getDataFolder(), "console_database.db");
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }

        try {
            Class.forName("org.sqlite.JDBC");
            this.connection = DriverManager.getConnection("jdbc:sqlite:" + file.getAbsolutePath());
            createTables();
        } catch (Exception e) {
            plugin.getLogger().severe("Ошибка подключения БД: " + e.getMessage());
        }
    }

    private void createTables() {
        String sql = "CREATE TABLE IF NOT EXISTS players_auth (" +
                     "username TEXT PRIMARY KEY," +
                     "password TEXT," +
                     "tg_id INTEGER DEFAULT 0," +
                     "is_locked INTEGER DEFAULT 0," +
                     "last_ip TEXT" +
                     ");";
        String logSql = "CREATE TABLE IF NOT EXISTS console_logs (" +
                     "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                     "admin_id TEXT," +
                     "command TEXT," +
                     "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP" +
                     ");";
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
            stmt.execute(logSql);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean playerExists(String username) {
        String sql = "SELECT username FROM players_auth WHERE LOWER(username) = LOWER(?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean registerPlayer(String username, String password) {
        String sql = "INSERT INTO players_auth(username, password) VALUES(?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean loginPlayer(String username, String password) {
        String sql = "SELECT password FROM players_auth WHERE LOWER(username) = LOWER(?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("password").equals(password);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean isPlayerLocked(String username) {
        String sql = "SELECT is_locked FROM players_auth WHERE LOWER(username) = LOWER(?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("is_locked") == 1;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public long getTelegramId(String username) {
        String sql = "SELECT tg_id FROM players_auth WHERE LOWER(username) = LOWER(?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getLong("tg_id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public void recordFailedLogin(String username) {
        // Логика записи неудачной попытки (опционально)
    }

    public void recordLoginIP(String username, String ip) {
        String sql = "UPDATE players_auth SET last_ip = ? WHERE LOWER(username) = LOWER(?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, ip);
            pstmt.setString(2, username);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public String generateLinkCode(String username) {
        String code = String.valueOf(100000 + new java.util.Random().nextInt(900000));
        tempCodes.put(username.toLowerCase(), code);
        return code;
    }

    public String getPlayerByLinkCode(String code) {
        for (Map.Entry<String, String> entry : tempCodes.entrySet()) {
            if (entry.getValue().equals(code)) {
                tempCodes.remove(entry.getKey());
                return entry.getKey();
            }
        }
        return null;
    }

    public void bindTelegram(String username, long tgId) {
        String sql = "UPDATE players_auth SET tg_id = ? WHERE LOWER(username) = LOWER(?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, tgId);
            pstmt.setString(2, username);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void setPlayerBlocked(String username, boolean blocked) {
        String sql = "UPDATE players_auth SET is_locked = ? WHERE LOWER(username) = LOWER(?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, blocked ? 1 : 0);
            pstmt.setString(2, username);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void logCommand(String adminId, String command) {
        String sql = "INSERT INTO console_logs(admin_id, command) VALUES(?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, adminId);
            pstmt.setString(2, command);
            pstmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
