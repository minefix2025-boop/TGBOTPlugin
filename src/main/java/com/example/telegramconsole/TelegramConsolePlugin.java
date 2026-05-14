package com.example.telegramconsole;

import org.bukkit.plugin.java.JavaPlugin;

public class TelegramConsolePlugin extends JavaPlugin {
    private static TelegramConsolePlugin instance;
    private BotManager botManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        
        String apiKey = getConfig().getString("telegram.api_key");
        long adminId = getConfig().getLong("admin_telegram_id");
        boolean twoFAEnabled = getConfig().getBoolean("two_factor_auth.enabled");
        String secretCode = getConfig().getString("two_factor_auth.secret_code");
        
        if (apiKey == null || apiKey.equals("8629251193:AAGlBusPJyY5ra_5ndEBrFuMXh6Khm_ospk")) {
            getLogger().severe("Telegram API key not configured!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        getCommand("link").setExecutor(new LinkCommand());
        
        botManager = new BotManager(apiKey, adminId, twoFAEnabled, secretCode);
        
        if (botManager.start()) {
            getLogger().info("Telegram bot started! Admin ID: " + adminId);
        } else {
            getLogger().severe("Failed to start Telegram bot!");
        }
    }

    @Override
    public void onDisable() {
        if (botManager != null) {
            botManager.stop();
        }
    }

    public static TelegramConsolePlugin getInstance() {
        return instance;
    }
}
