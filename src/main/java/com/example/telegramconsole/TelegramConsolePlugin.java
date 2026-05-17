package com.example.telegramconsole;

import org.bukkit.plugin.java.JavaPlugin;

public class TelegramConsolePlugin extends JavaPlugin {

    private static TelegramConsolePlugin instance;
    private BotManager botManager;
    private DatabaseManager databaseManager;

    @Override
    public void onEnable() {
        instance = this;

        this.databaseManager = new DatabaseManager(this);
        
        this.botManager = new BotManager(this);
        this.botManager.startBot();

        if (this.getCommand("tgconsole") != null) {
            this.getCommand("tgconsole").setExecutor(new TGConsoleCommand());
        }

        getLogger().info("TelegramConsole модуль успешно активирован!");
    }

    @Override
    public void onDisable() {
        if (botManager != null) {
            botManager.stopBot();
        }
        getLogger().info("TelegramConsole модуль выключен.");
    }

    public static TelegramConsolePlugin getInstance() {
        return instance;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }
}
