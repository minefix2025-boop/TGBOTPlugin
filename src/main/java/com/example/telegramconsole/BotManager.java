package com.example.telegramconsole;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class BotManager {

    private final JavaPlugin plugin;
    private ConsoleBotInstance botInstance;

    public BotManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void startBot() {
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            this.botInstance = new ConsoleBotInstance(plugin);
            botsApi.registerBot(botInstance);
            plugin.getLogger().info("[ConsoleBot] Успешно запущен!");
        } catch (Exception e) {
            plugin.getLogger().severe("[ConsoleBot] Ошибка старта: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void stopBot() {
        this.botInstance = null;
    }

    private static class ConsoleBotInstance extends TelegramLongPollingBot {

        private final JavaPlugin plugin;
        private final String token = "8629251193:AAErpWdzt_vNpkfhlxN8aiXlLgWkfM7h5QQ";
        private final Set<String> allowedAdmins = new HashSet<>(Arrays.asList("6343309173", "7742036100"));

        public ConsoleBotInstance(JavaPlugin plugin) {
            this.plugin = plugin;
        }

        @Override
        public String getBotUsername() {
            return "MinecraftConsoleBot";
        }

        @Override
        public String getBotToken() {
            return this.token;
        }

        @Override
        public void onUpdateReceived(Update update) {
            if (update.hasMessage() && update.getMessage().hasText()) {
                String text = update.getMessage().getText();
                String chatId = update.getMessage().getChatId().toString();

                if (!allowedAdmins.contains(chatId)) {
                    sendReply(chatId, "❌ Отказано в доступе к консоли сервера.");
                    return;
                }

                Bukkit.getScheduler().runTask(plugin, () -> {
                    try {
                        String commandToExecute = text.startsWith("/") ? text.substring(1) : text;
                        
                        TelegramConsolePlugin.getInstance().getDatabaseManager().logCommand(chatId, commandToExecute);

                        boolean success = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), commandToExecute);
                        
                        if (success) {
                            sendReply(chatId, "💻 Команда `/" + commandToExecute + "` успешно выполнена на сервере.");
                        } else {
                            sendReply(chatId, "⚠️ Сервер не смог обработать команду: `/" + commandToExecute + "`");
                        }
                    } catch (Exception e) {
                        sendReply(chatId, "🛑 Ошибка при выполнении команды: " + e.getMessage());
                    }
                });
            }
        }

        private void sendReply(String chatId, String message) {
            SendMessage sm = new SendMessage();
            sm.setChatId(chatId);
            sm.setText(message);
            try {
                execute(sm);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
