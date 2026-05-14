package com.example.telegramconsole;

import org.bukkit.Bukkit;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import java.util.HashMap;
import java.util.Map;

public class BotManager {
    private final String apiKey;
    private final boolean twoFAEnabled;
    private final String secretCode;
    private MyBot bot;
    private TelegramBotsApi botsApi;

    public BotManager(String apiKey, boolean twoFAEnabled, String secretCode) {
        this.apiKey = apiKey;
        this.twoFAEnabled = twoFAEnabled;
        this.secretCode = secretCode;
    }

    public boolean start() {
        try {
            botsApi = new TelegramBotsApi(DefaultBotSession.class);
            bot = new MyBot(apiKey, twoFAEnabled, secretCode);
            botsApi.registerBot(bot);
            return true;
        } catch (TelegramApiException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void stop() {
        if (bot != null) {
            bot.stop();
        }
    }

    private static class MyBot extends TelegramLongPollingBot {
        private final String apiKey;
        private final boolean twoFAEnabled;
        private final String secretCode;
        private final Map<Long, Boolean> authenticatedUsers = new HashMap<>();
        private boolean running = true;

        public MyBot(String apiKey, boolean twoFAEnabled, String secretCode) {
            this.apiKey = apiKey;
            this.twoFAEnabled = twoFAEnabled;
            this.secretCode = secretCode;
        }

        @Override
        public String getBotUsername() {
            return "MinecraftBot";
        }

        @Override
        public String getBotToken() {
            return apiKey;
        }

        @Override
        public void onUpdateReceived(Update update) {
            if (!running) return;
            
            if (update.hasMessage() && update.getMessage().hasText()) {
                long userId = update.getMessage().getChatId();
                String message = update.getMessage().getText().trim();
                
                if (twoFAEnabled && !authenticatedUsers.getOrDefault(userId, false)) {
                    if (message.startsWith("/auth ")) {
                        String code = message.substring(6);
                        if (code.equals(secretCode)) {
                            authenticatedUsers.put(userId, true);
                            sendMessage(userId, "Authentication successful!");
                        } else {
                            sendMessage(userId, "Wrong secret code!");
                        }
                    } else {
                        sendMessage(userId, "Authentication required! Send: /auth YOUR_CODE");
                    }
                    return;
                }
                
                if (message.startsWith("/")) {
                    executeCommand(userId, message);
                }
            }
        }

        private void executeCommand(long userId, String command) {
            String cmd = command.substring(1);
            
            Bukkit.getScheduler().runTask(TelegramConsolePlugin.getInstance(), () -> {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
            });
            
            sendMessage(userId, "Command executed: " + cmd);
        }

        private void sendMessage(long chatId, String text) {
            SendMessage message = new SendMessage();
            message.setChatId(String.valueOf(chatId));
            message.setText(text);
            try {
                execute(message);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }

        public void stop() {
            running = false;
        }
    }
}
