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
        boolean twoFAEnabled = getConfig().getBoolean("two_factor_auth.enabled");
        String secretCode = getConfig().getString("two_factor_auth.secret_code");
        
        if (apiKey == null || apiKey.equals("YOUR_BOT_TOKEN_HERE")) {
            getLogger().severe("Telegram API key not configured!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        botManager = new BotManager(apiKey, twoFAEnabled, secretCode);
        
        if (botManager.start()) {
            getLogger().info("Telegram bot started!");
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
