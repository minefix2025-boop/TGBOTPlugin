package example.telegramconsole;

import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;

public class DatabaseManager {

    private Connection connection;

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
            plugin.getLogger().severe("Ошибка подключения БД консоли: " + e.getMessage());
        }
    }

    private void createTables() {
        String sql = "CREATE TABLE IF NOT EXISTS console_logs (" +
                     "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                     "admin_id TEXT," +
                     "command TEXT," +
                     "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP" +
                     ");";
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        } catch (Exception e) {
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
