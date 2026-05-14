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
import java.util.Random;
import java.util.UUID;

public class BotManager {
    private static final Map<String, LinkRequest> pendingLinks = new HashMap<>();
    private static final Map<String, Long> linkedAccounts = new HashMap<>();
    private static BotManager instance;
    
    private final String apiKey;
    private final long adminId;
    private final boolean twoFAEnabled;
    private final String secretCode;
    private final Map<Long, Boolean> authenticatedUsers = new HashMap<>();
    private MyBot bot;
    private TelegramBotsApi botsApi;

    public BotManager(String apiKey, long adminId, boolean twoFAEnabled, String secretCode) {
        this.apiKey = apiKey;
        this.adminId = adminId;
        this.twoFAEnabled = twoFAEnabled;
        this.secretCode = secretCode;
        instance = this;
    }

    public boolean start() {
        try {
            botsApi = new TelegramBotsApi(DefaultBotSession.class);
            bot = new MyBot(apiKey, adminId, twoFAEnabled, secretCode, authenticatedUsers);
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
    
    public static String generateLinkCode(String playerUuid, String playerName) {
        String code = String.format("%06d", new Random().nextInt(999999));
        pendingLinks.put(code, new LinkRequest(playerUuid, playerName, System.currentTimeMillis()));
        return code;
    }
    
    public static boolean linkAccount(String playerUuid, String playerName, String code) {
        LinkRequest request = pendingLinks.remove(code);
        if (request == null) return false;
        if (System.currentTimeMillis() - request.timestamp > 300000) return false;
        if (!request.playerUuid.equals(playerUuid)) return false;
        
        linkedAccounts.put(playerName, request.telegramId != 0 ? request.telegramId : 0);
        return true;
    }
    
    public static void setTelegramIdForCode(String code, long telegramId) {
        LinkRequest request = pendingLinks.get(code);
        if (request != null) {
            request.telegramId = telegramId;
        }
    }
    
    public static boolean isLinked(String playerName, long telegramId) {
        return linkedAccounts.containsKey(playerName);
    }
    
    private static class LinkRequest {
        String playerUuid;
        String playerName;
        long timestamp;
        long telegramId;
        
        LinkRequest(String playerUuid, String playerName, long timestamp) {
            this.playerUuid = playerUuid;
            this.playerName = playerName;
            this.timestamp = timestamp;
            this.telegramId = 0;
        }
    }

    private static class MyBot extends TelegramLongPollingBot {
        private final String apiKey;
        private final long adminId;
        private final boolean twoFAEnabled;
        private final String secretCode;
        private final Map<Long, Boolean> authenticatedUsers;
        private final Map<Long, String> pendingLinkCodes = new HashMap<>();
        private boolean running = true;

        public MyBot(String apiKey, long adminId, boolean twoFAEnabled, String secretCode, Map<Long, Boolean> authenticatedUsers) {
            this.apiKey = apiKey;
            this.adminId = adminId;
            this.twoFAEnabled = twoFAEnabled;
            this.secretCode = secretCode;
            this.authenticatedUsers = authenticatedUsers;
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
                String userName = update.getMessage().getFrom().getFirstName();
                
                if (twoFAEnabled && !authenticatedUsers.getOrDefault(userId, false) && userId != adminId) {
                    if (message.startsWith("/auth ")) {
                        String code = message.substring(6);
                        if (code.equals(secretCode)) {
                            authenticatedUsers.put(userId, true);
                            sendMessage(userId, "✅ Authentication successful!");
                        } else {
                            sendMessage(userId, "❌ Wrong secret code!");
                        }
                    } else {
                        sendMessage(userId, "🔐 Authentication required! Send: /auth YOUR_CODE");
                    }
                    return;
                }
                
                if (message.equals("/start")) {
                    sendMessage(userId, "🤖 Minecraft Server Bot\n\nCommands:\n/link - Get code to link your account\n/cmd <command> - Execute console command (Admin only)");
                    return;
                }
                
                if (message.equals("/link")) {
                    String code = generateCode();
                    pendingLinkCodes.put(userId, code);
                    sendMessage(userId, "🔗 Your link code: " + code + "\nType in Minecraft: /link " + code);
                    return;
                }
                
                if (message.startsWith("/cmd ")) {
                    if (userId != adminId && !authenticatedUsers.getOrDefault(userId, false)) {
                        sendMessage(userId, "❌ Only server admin can use /cmd!");
                        return;
                    }
                    
                    String command = message.substring(5);
                    executeCommand(userId, command);
                    return;
                }
                
                sendMessage(userId, "Unknown command. Use /start for help.");
            }
        }
        
        private String generateCode() {
            return String.format("%06d", new Random().nextInt(999999));
        }
        
        private void executeCommand(long userId, String command) {
            Bukkit.getScheduler().runTask(TelegramConsolePlugin.getInstance(), () -> {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
            });
            sendMessage(userId, "✅ Command executed: " + command);
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
