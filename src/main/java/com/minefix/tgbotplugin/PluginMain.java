package com.minefix.tgbotplugin;

import org.bukkit.plugin.java.JavaPlugin;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import com.example.telegramconsole.MovementBlockListener;
import com.example.telegramconsole.TelegramConsolePlugin;

public class PluginMain extends JavaPlugin {

    private static MovementBlockListener movementBlockListener;
    private static TelegramBotImpl telegramBot;

    @Override
    public void onEnable() {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        // ИСПРАВЛЕНО: Передаем экземпляр плагина TelegramConsolePlugin, который ожидает конструктор
        movementBlockListener = new MovementBlockListener(TelegramConsolePlugin.getInstance());
        getServer().getPluginManager().registerEvents(movementBlockListener, this);
        getServer().getPluginManager().registerEvents(new JoinListener(), this);

        AuthCommand authExecutor = new AuthCommand();
        if (this.getCommand("reg") != null) this.getCommand("reg").setExecutor(authExecutor);
        if (this.getCommand("login") != null) this.getCommand("login").setExecutor(authExecutor);
        if (this.getCommand("link") != null) this.getCommand("link").setExecutor(new LinkCommand());

        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            telegramBot = new TelegramBotImpl(this);
            botsApi.registerBot(telegramBot);
            getLogger().info("Telegram-бот был успешно запущен!");
        } catch (Exception e) {
            getLogger().severe("Не удалось запустить Telegram бота!");
            e.printStackTrace();
        }
    }

    public static MovementBlockListener getMovementBlockListener() {
        return movementBlockListener;
    }

    public static TelegramBotImpl getTelegramBot() {
        return telegramBot;
    }
}
