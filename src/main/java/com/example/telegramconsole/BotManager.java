package com.example.telegramconsole;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.util.*;

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
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stopBot() {
        this.botInstance = null;
    }

    public void sendMsg(long chatId, String text) {
        if (botInstance != null) botInstance.sendReply(String.valueOf(chatId), text);
    }

    public void showAccountMenu(long chatId, String playerName) {}

    public void send2FARequest(Player player, long chatId) {
        if (botInstance != null) botInstance.send2FACard(player, chatId);
    }

    private static class ConsoleBotInstance extends TelegramLongPollingBot {
        private final JavaPlugin plugin;
        private final String token = "8629251193:AAErpWdzt_vNpkfhlxN8aiXlLgWkfM7h5QQ";
        private final Set<String> adminIds = new HashSet<>(Arrays.asList("6343309173", "7742036100"));

        public ConsoleBotInstance(JavaPlugin plugin) {
            this.plugin = plugin;
        }

        @Override
        public String getBotUsername() { return "MinecraftConsoleBot"; }

        @Override
        public String getBotToken() { return this.token; }

        @Override
        public void onUpdateReceived(Update update) {
            if (update.hasMessage() && update.getMessage().hasText()) {
                String text = update.getMessage().getText();
                String chatId = update.getMessage().getChatId().toString();

                if (!adminIds.contains(chatId)) return;

                Bukkit.getScheduler().runTask(plugin, () -> {
                    String cmd = text.startsWith("/") ? text.substring(1) : text;
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
                    sendReply(chatId, "💻 Консоль выполнила команду.");
                });
            } 
            else if (update.hasCallbackQuery()) {
                String callbackData = update.getCallbackQuery().getData();
                String chatId = update.getCallbackQuery().getMessage().getChatId().toString();
                int messageId = update.getCallbackQuery().getMessage().getMessageId();

                if (callbackData.startsWith("2fa_accept_") || callbackData.startsWith("2fa_deny_")) {
                    String[] parts = callbackData.split("_");
                    UUID playerUuid = UUID.fromString(parts[2]);
                    Player player = Bukkit.getPlayer(playerUuid);

                    if (callbackData.startsWith("2fa_accept_")) {
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            if (player != null) {
                                player.sendMessage("§aВход успешно подтвержден через Telegram!");
                                TelegramConsolePlugin.getInstance().getMovementBlockListener().stopTimer(playerUuid);
                                PendingApproval.remove(playerUuid);
                            }
                        });
                        editReply(chatId, messageId, "✅ Вход в аккаунт одобрен.");
                    } else {
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            if (player != null) {
                                player.kickPlayer("§cНЕ потвержден через Telegram");
                                PendingApproval.remove(playerUuid);
                            }
                        });
                        editReply(chatId, messageId, "❌ Вход отклонен. Игрок кикнут.");
                    }
                }
            }
        }

        public void send2FACard(Player player, long chatId) {
            String ip = player.getAddress().getAddress().getHostAddress();
            UUID uuid = player.getUniqueId();
            PendingApproval.add(uuid);

            SendMessage sm = new SendMessage();
            sm.setChatId(String.valueOf(chatId));
            sm.setText("🔔 Вход в Аккаунт с IP: " + ip + "\nUUID: " + uuid.toString());

            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> rows = new ArrayList<>();
            List<InlineKeyboardButton> row = new ArrayList<>();

            InlineKeyboardButton accept = new InlineKeyboardButton();
            accept.setText("✅ Принять");
            accept.setCallbackData("2fa_accept_" + uuid.toString());

            InlineKeyboardButton deny = new InlineKeyboardButton();
            deny.setText("❌ Отказать");
            deny.setCallbackData("2fa_deny_" + uuid.toString());

            row.add(accept);
            row.add(deny);
            rows.add(row);
            markup.setKeyboard(rows);
            sm.setReplyMarkup(markup);

            try { execute(sm); } catch (Exception e) { e.printStackTrace(); }
        }

        public void sendReply(String chatId, String message) {
            SendMessage sm = new SendMessage();
            sm.setChatId(chatId);
            sm.setText(message);
            try { execute(sm); } catch (Exception e) { e.printStackTrace(); }
        }

        private void editReply(String chatId, int messageId, String text) {
            EditMessageText em = new EditMessageText();
            em.setChatId(chatId);
            em.setMessageId(messageId);
            em.setText(text);
            try { execute(em); } catch (Exception e) { e.printStackTrace(); }
        }
    }
}
