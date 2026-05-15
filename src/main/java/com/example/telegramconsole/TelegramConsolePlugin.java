package com.example.telegramconsole;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class TelegramConsolePlugin extends JavaPlugin {

    private BotManager botManager;
    private DatabaseManager databaseManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        FileConfiguration config = getConfig();

        String botToken = config.getString("bot-token", "8629251193:AAErpWdzt_vNpkfhlxN8aiXlLgWkfM7h5QQ");
        long adminId = config.getLong("admin-id", 7742036100L);

        // Инициализация базы JSON в папке плагина ATGCON
        databaseManager = new DatabaseManager(getDataFolder());

        botManager = new BotManager(botToken, adminId, this);
        if (botManager.start()) {
            getLogger().info("✅ Telegram бот успешно запущен!");
        } else {
            getLogger().severe("❌ Ошибка при запуске Telegram бота! Проверьте токен.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        getCommand("reg").setExecutor(new RegCommand(this));
        getCommand("login").setExecutor(new LoginCommand(this));
        getCommand("link").setExecutor(new LinkCommand(this));
        getCommand("tgconsole").setExecutor(new TGConsoleCommand(this));

        Bukkit.getPluginManager().registerEvents(new PlayerListener(this), this);

        getLogger().info("═══════════════════════════════════════");
        getLogger().info("✅ TelegramConsoleBot успешно загружен!");
        getLogger().info("═══════════════════════════════════════");
    }

    @Override
    public void onDisable() {
        if (botManager != null) {
            botManager.stop();
        }
    }

    public BotManager getBotManager() { return botManager; }
    public DatabaseManager getDatabaseManager() { return databaseManager; }
}
