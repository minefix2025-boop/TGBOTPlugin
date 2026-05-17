package example.telegramconsole;

import org.bukkit.plugin.java.JavaPlugin;

public class TelegramConsolePlugin extends JavaPlugin {

    private static TelegramConsolePlugin instance;
    private BotManager botManager;
    private DatabaseManager databaseManager;

    @Override
    public void onEnable() {
        instance = this;

        // Создаем менеджер базы данных для логирования команд консоли
        this.databaseManager = new DatabaseManager(this);
        
        // Создаем и запускаем бота консоли
        this.botManager = new BotManager(this);
        this.botManager.startBot();

        if (this.getCommand("tgconsole") != null) {
            this.getCommand("tgconsole").setExecutor(new TGConsoleCommand());
        }

        getLogger().info("Модуль TelegramConsole успешно запущен!");
    }

    @Override
    public void onDisable() {
        if (botManager != null) {
            botManager.stopBot();
        }
        getLogger().info("Модуль TelegramConsole отключен.");
    }

    public static TelegramConsolePlugin getInstance() {
        return instance;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }
}
