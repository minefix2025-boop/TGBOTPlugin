package com.minefix.tgbotplugin;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.io.File;

public class PluginMain extends JavaPlugin {

    private static PluginMain instance;
    private TelegramBotImpl bot;

    @Override
    public void onEnable() {
        instance = this;

        // Создаем папку плагина и конфиг
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }
        saveDefaultConfig();

        // Инициализируем базу данных SQLite
        File dbFile = new File(getDataFolder(), "database.db");
        SqliteDataStore.initialize(dbFile.getAbsolutePath());

        // Запуск Telegram Бота
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            this.bot = new TelegramBotImpl(this);
            botsApi.registerBot(this.bot);
            getLogger().info("Telegram бот успешно запущен!");
        } catch (TelegramApiException e) {
            getLogger().severe("Не удалось запустить Telegram бота: " + e.getMessage());
            e.printStackTrace();
        }

        // Регистрация команд в Minecraft
        getCommand("link").setExecutor(new LinkCommand());
        if (getCommand("auth") != null) {
            getCommand("auth").setExecutor(new AuthCommand(this));
        }

        // Регистрация ивентов блокировки движений и входа
        getServer().getPluginManager().registerEvents(new JoinListener(), this);
        getServer().getPluginManager().registerEvents(new MovementBlockListener(), this);

        getLogger().info("TGBOTPlugin успешно включен!");
    }

    @Override
    public void onDisable() {
        PendingApproval.clear();
        getLogger().info("TGBOTPlugin выключен.");
    }

    public static PluginMain getInstance() {
        return instance;
    }

    public TelegramBotImpl getBot() {
        return bot;
    }
}
