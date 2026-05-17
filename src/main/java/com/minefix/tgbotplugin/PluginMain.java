package com.minefix.tgbotplugin;

import org.bukkit.plugin.java.JavaPlugin;

public class PluginMain extends JavaPlugin {
    private static PluginMain instance;
    private DataStore dataStore;
    private TelegramBotImpl telegramBot;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        dataStore = new SqliteDataStore(this);
        dataStore.init();

        String botToken = getConfig().getString("telegram.token", "");
        String botUsername = getConfig().getString("telegram.username", "");
        if (!botToken.isEmpty() && !botUsername.isEmpty()) {
            telegramBot = new TelegramBotImpl(botToken, botUsername, dataStore, this);
            telegramBot.start();
        } else {
            getLogger().warning("Telegram bot token/username не настроены в config.yml");
        }

        getCommand("reg").setExecutor(new AuthCommand(dataStore));
        getCommand("login").setExecutor(new AuthCommand(dataStore));
        getCommand("link").setExecutor(new LinkCommand(dataStore, telegramBot));

        getServer().getPluginManager().registerEvents(new MovementBlockListener(dataStore), this);
        getServer().getPluginManager().registerEvents(new JoinListener(dataStore), this);

        getLogger().info("TGBOTPlugin включён");
    }

    @Override
    public void onDisable() {
        if (telegramBot != null) telegramBot.stop();
        if (dataStore != null) dataStore.close();
    }

    public static PluginMain getInstance() { return instance; }
    public DataStore getDataStore() { return dataStore; }
    public TelegramBotImpl getTelegramBot() { return telegramBot; }
}
