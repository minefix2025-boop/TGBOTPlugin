package minefix.tgbotplugin;

import org.bukkit.plugin.java.JavaPlugin;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

public class PluginMain extends JavaPlugin {

    private static MovementBlockListener movementBlockListener;
    private static TelegramBotImpl telegramBot;
    private SqliteDataStore sqliteDataStore;

    @Override
    public void onEnable() {
        // Создаем папку плагина, если её нет
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        // 1. Инициализация базы данных SQLite
        this.sqliteDataStore = new SqliteDataStore(this);
        DataStore.init(this.sqliteDataStore);

        // 2. Инициализация слушателей (Эвентов)
        movementBlockListener = new MovementBlockListener(this);
        getServer().getPluginManager().registerEvents(movementBlockListener, this);
        getServer().getPluginManager().registerEvents(new JoinListener(), this);

        // 3. Регистрация команд авторизации и привязки
        AuthCommand authExecutor = new AuthCommand();
        if (this.getCommand("reg") != null) this.getCommand("reg").setExecutor(authExecutor);
        if (this.getCommand("login") != null) this.getCommand("login").setExecutor(authExecutor);
        if (this.getCommand("link") != null) this.getCommand("link").setExecutor(new LinkCommand());

        // 4. Запуск Telegram бота
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            telegramBot = new TelegramBotImpl(this);
            botsApi.registerBot(telegramBot);
            getLogger().info("Telegram-бот был успешно запущен с вашим токеном!");
        } catch (Exception e) {
            getLogger().severe("Не удалось запустить Telegram бота! Проверьте библиотеки и сеть.");
            e.printStackTrace();
        }
    }

    @Override
    public void onDisable() {
        getLogger().info("Плагин TgBotPlugin выключен.");
    }

    public static MovementBlockListener getMovementBlockListener() {
        return movementBlockListener;
    }

    public static TelegramBotImpl getTelegramBot() {
        return telegramBot;
    }
}
