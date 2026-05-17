package com.example.telegramconsole;

import org.bukkit.plugin.java.JavaPlugin;

public class TelegramConsolePlugin extends JavaPlugin {

    private static TelegramConsolePlugin instance;
    private BotManager botManager;
    private DatabaseManager databaseManager;
    private MovementBlockListener movementBlockListener;

    @Override
    public void onEnable() {
        instance = this;

        this.databaseManager = new DatabaseManager(this);
        
        this.movementBlockListener = new MovementBlockListener(this);
        getServer().getPluginManager().registerEvents(movementBlockListener, this);

        this.botManager = new BotManager(this);
        this.botManager.startBot();

        // Передаем инстанс плагина 'this' в конструкторы команд
        if (this.getCommand("login") != null) this.getCommand("login").setExecutor(new LoginCommand(this));
        if (this.getCommand("reg") != null) this.getCommand("reg").setExecutor(new RegCommand(this));
        if (this.getCommand("link") != null) this.getCommand("link").setExecutor(new LinkCommand(this));
        if (this.getCommand("tgconsole") != null) this.getCommand("tgconsole").setExecutor(new TGConsoleCommand());

        getLogger().info("TelegramConsole модуль и системы защиты успешно активированы!");
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

    public BotManager getBotManager() {
        return botManager;
    }

    public MovementBlockListener getMovementBlockListener() {
        return movementBlockListener;
    }
}
