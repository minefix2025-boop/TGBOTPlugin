package com.example.telegramconsole;
import com.example.telegramconsole.commands.RegCommand;
import com.example.telegramconsole.commands.LoginCommand;
import com.example.telegramconsole.commands.TGConsoleCommand;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;

public class TelegramConsolePlugin extends JavaPlugin {
    private static TelegramConsolePlugin instance;
    private BotManager botManager;
    private DatabaseManager databaseManager;

    @Override
    public void onEnable() {
        instance = this;

        // Создать папку для конфигурации
        File dataFolder = new File(getDataFolder(), "ATGCON");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        // Загрузить конфигурацию
        saveDefaultConfig();

        // Инициализировать БД
        databaseManager = new DatabaseManager(dataFolder);

        // Получить параметры из конфига
        String botToken = getConfig().getString("telegram.bot-token");
        long adminId = getConfig().getLong("admin-id", 7742036100L);

        if (botToken == null || botToken.isEmpty() || botToken.contains("YOUR_TOKEN")) {
            getLogger().severe("❌ Токен Telegram бота не настроен! Отредактируйте config.yml");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

// Передайте нужные значения для boolean и String (например, false и "TelegramConsoleBot")
botManager = new BotManager(botToken, adminId, false, "TelegramConsoleBot");


        if (botManager.start()) {
            getLogger().info("✅ Telegram бот запущен!");
            getLogger().info("👑 Администратор ID: " + adminId);
        } else {
            getLogger().severe("❌ Ошибка при запуске Telegram бота!");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        // Регистрировать команды
        getCommand("reg").setExecutor(new RegCommand());
        getCommand("login").setExecutor(new LoginCommand());
        getCommand("link").setExecutor(new LinkCommand());
        getCommand("tgconsole").setExecutor(new TGConsoleCommand());

        // Регистрировать слушатели событий
        Bukkit.getPluginManager().registerEvents(new PlayerListener(), this);

        getLogger().info("═══════════════════════════════════════");
        getLogger().info("✅ TelegramConsoleBot успешно загружен!");
        getLogger().info("📁 Данные хранятся в: ATGCON/");
        getLogger().info("═══════════════════════════════════════");
    }

    @Override
    public void onDisable() {
        if (botManager != null) {
            botManager.stop();
        }
        getLogger().info("❌ TelegramConsoleBot отключен");
    }

    public static TelegramConsolePlugin getInstance() {
        return instance;
    }

    public BotManager getBotManager() {
        return botManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }
}
