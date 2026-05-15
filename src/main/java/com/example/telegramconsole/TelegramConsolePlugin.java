package com.example.telegramconsole;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class TelegramConsolePlugin extends JavaPlugin {

    private BotManager botManager;
    private DatabaseManager databaseManager;
    private final Map<String, UUID> linkCodes = new HashMap<>(); // Код -> UUID игрока

    @Override
    public void onEnable() {
        saveDefaultConfig();
        FileConfiguration config = getConfig();

        String botToken = config.getString("bot-token", "YOUR_TOKEN");
        long adminId = config.getLong("admin-id", 7742036100L);

        // Инициализация базы данных
        databaseManager = new DatabaseManager(this);

        if (botToken == null || botToken.isEmpty() || botToken.contains("YOUR_TOKEN")) {
            getLogger().severe("❌ Токен Telegram бота не настроен! Отредактируйте config.yml");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        // Запуск бота с нужными параметрами
        botManager = new BotManager(botToken, adminId, this);
        if (botManager.start()) {
            getLogger().info("✅ Telegram бот успешно запущен!");
        } else {
            getLogger().severe("❌ Ошибка при запуске Telegram бота!");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        // Регистрация команд Minecraft
        getCommand("reg").setExecutor(new RegCommand(this));
        getCommand("login").setExecutor(new LoginCommand(this));
        getCommand("link").setExecutor(new LinkCommand(this));
        getCommand("tgconsole").setExecutor(new TGConsoleCommand(this));

        // Регистрация слушателя событий
        Bukkit.getPluginManager().registerEvents(new PlayerListener(this), this);

        getLogger().info("═══════════════════════════════════════");
        getLogger().info("✅ TelegramConsoleBot успешно загружен!");
        getLogger().info("📁 Данные хранятся в: plugins/TelegramConsoleBot/");
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
    public Map<String, UUID> getLinkCodes() { return linkCodes; }

    public String generateLinkCode(UUID playerUUID) {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder code = new StringBuilder();
        Random rnd = new Random();
        while (code.length() < 20) {
            code.append(characters.charAt(rnd.nextInt(characters.length())));
        }
        String finalCode = code.toString();
        linkCodes.put(finalCode, playerUUID);
        return finalCode;
    }
}
