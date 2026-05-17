package com.minefix.tgbotplugin;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.*;

import com.example.telegramconsole.PendingApproval;

public class TelegramBotImpl extends TelegramLongPollingBot {

    private final JavaPlugin plugin;
    private final String botToken = "8629251193:AAErpWdzt_vNpkfhlxN8aiXlLgWkfM7h5QQ";
    private final Set<String> adminIds = new HashSet<>(Arrays.asList("6343309173", "7742036100"));

    public TelegramBotImpl(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getBotUsername() {
        return "MinecraftTgAuthBot"; 
    }

    @Override
    public String getBotToken() {
        return this.botToken;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            String chatId = update.getMessage().getChatId().toString();

            if (messageText.startsWith("/link ")) {
                String[] parts = messageText.split(" ");
                if (parts.length > 1) {
                    String code = parts[1];
                    UUID playerUUID = DataStore.getPlayerByLinkCode(code);

                    if (playerUUID != null) {
                        DataStore.bindTelegram(playerUUID, chatId);
                        sendMsg(chatId, "✅ Ваш Telegram успешно привязан к аккаунту Minecraft!");
                        sendProfileMenu(chatId, playerUUID);
                    } else {
                        sendMsg(chatId, "❌ Неверный или истекший код привязки.");
                    }
                } else {
                    sendMsg(chatId, "❌ Используйте: /link <код>");
                }
                return;
            }

            if (adminIds.contains(chatId) && messageText.startsWith("/profile ")) {
                String[] parts = messageText.split(" ");
                if (parts.length > 1) {
                    String targetName = parts[1];
                    Player targetPlayer = Bukkit.getPlayer(targetName);
                    
                    if (targetPlayer != null) {
                        sendProfileMenu(chatId, targetPlayer.getUniqueId());
                    } else {
                        sendMsg(chatId, "❌ Игрок " + targetName + " сейчас оффлайн. Не удалось загрузить точный профиль.");
                    }
                } else {
                    sendMsg(chatId, "❌ Используйте: /profile <Никнейм>");
                }
                return;
            }

            if (adminIds.contains(chatId) && !messageText.startsWith("/profile") && !messageText.startsWith("/link")) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    String cleanCommand = messageText.startsWith("/") ? messageText.substring(1) : messageText;
                    
                    try {
                        com.example.telegramconsole.TelegramConsolePlugin.getInstance().getDatabaseManager().logCommand(chatId, cleanCommand);
                    } catch (NoClassDefFoundError | Exception ignored) {}

                    boolean success = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cleanCommand);
                    if (success) {
                        sendMsg(chatId, "💻 Консоль: Команда `/" + cleanCommand + "` выполнена.");
                    } else {
                        sendMsg(chatId, "⚠️ Консоль не смогла обработать команду: `/" + cleanCommand + "`");
                    }
                });
                return; 
            }
        } 
        
        else if (update.hasCallbackQuery()) {
            String callbackData = update.getCallbackQuery().getData();
            String chatId = update.getCallbackQuery().getMessage().getChatId().toString();
            long messageId = update.getCallbackQuery().getMessage().getMessageId();

            if (callbackData.startsWith("2fa_accept_") || callbackData.startsWith("2fa_deny_")) {
                String[] parts = callbackData.split("_");
                UUID playerUuid = UUID.fromString(parts[2]);
                Player player = Bukkit.getPlayer(playerUuid);

                if (callbackData.startsWith("2fa_accept_")) {
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        if (player != null) {
                            player.sendMessage("§aВход успешно подтвержден через Telegram!");
                            PluginMain.getMovementBlockListener().stopTimer(playerUuid);
                            PendingApproval.remove(playerUuid);
                        }
                    });
                    editMsg(chatId, (int) messageId, "✅ Вход в аккаунт был одобрен.");
                } else {
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        if (player != null) {
                            player.kickPlayer("§cНЕ потвержден через Telegram");
                            PendingApproval.remove(playerUuid);
                        }
                    });
                    editMsg(chatId, (int) messageId, "❌ Вход в аккаунт был отклонен. Игрок кикнут.");
                }
            }
            
            else if (adminIds.contains(chatId) && callbackData.contains("_")) {
                String[] parts = callbackData.split("_");
                String action = parts[0];
                UUID targetUuid = UUID.fromString(parts[1]);
                Player targetPlayer = Bukkit.getPlayer(targetUuid);

                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    switch (action) {
                        case "kick":
                            if (targetPlayer != null) {
                                targetPlayer.kickPlayer("§cВы были кикнуты администратором через Telegram.");
                                editMsg(chatId, (int) messageId, "👤 Действие выполнено: Игрок кикнут.");
                            } else {
                                sendMsg(chatId, "❌ Игрок уже вышел с сервера.");
                            }
                            break;
                        case "lock":
                            DataStore.setPlayerBlocked(targetUuid, true);
                            if (targetPlayer != null) {
                                targetPlayer.kickPlayer("§cВаш аккаунт заблокирован через Telegram.");
                            }
                            editMsg(chatId, (int) messageId, "🔒 Действие выполнено: Аккаунт заблокирован.");
                            break;
                        case "unlock":
                            DataStore.setPlayerBlocked(targetUuid, false);
                            editMsg(chatId, (int) messageId, "🔓 Действие выполнено: Аккаунт разблокирован.");
                            break;
                    }
                });
            }
        }
    }

    public void send2FARequest(Player player) {
        String chatId = DataStore.getTelegramChatId(player.getUniqueId());
        if (chatId == null) return; 

        String ip = player.getAddress().getAddress().getHostAddress();
        UUID uuid = player.getUniqueId();

        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("🔔 Вход в Аккаунт\n🌐 IP: " + ip + "\n🆔 UUID: " + uuid.toString());

        InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
        List<InlineKeyboardButton> rowInline = new ArrayList<>();

        InlineKeyboardButton acceptBtn = new InlineKeyboardButton();
        acceptBtn.setText("✅ Принять");
        acceptBtn.setCallbackData("2fa_accept_" + uuid.toString());

        InlineKeyboardButton denyBtn = new InlineKeyboardButton();
        denyBtn.setText("❌ Отказать");
        denyBtn.setCallbackData("2fa_deny_" + uuid.toString());

        rowInline.add(acceptBtn);
        rowInline.add(denyBtn);
        rowsInline.add(rowInline);
        markupInline.setKeyboard(rowsInline);
        message.setReplyMarkup(markupInline);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void sendProfileMenu(String chatId, UUID targetUuid) {
        Player target = Bukkit.getPlayer(targetUuid);
        boolean isOnline = (target != null && target.isOnline());
        String ip = isOnline ? target.getAddress().getAddress().getHostAddress() : "Неизвестен (Оффлайн)";
        String status = isOnline ? "🟢 Онлайн" : "🔴 Оффлайн";

        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("👤 Профиль игрока:\n🆔 UUID: " + targetUuid + "\n🌐 IP: " + ip + "\n📊 Статус: " + status);

        InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

        List<InlineKeyboardButton> row1 = new ArrayList<>();
        InlineKeyboardButton lockBtn = new InlineKeyboardButton();
        lockBtn.setText("🔒 Заблокировать");
        lockBtn.setCallbackData("lock_" + targetUuid);
        
        InlineKeyboardButton unlockBtn = new InlineKeyboardButton();
        unlockBtn.setText("🔓 Разблокировать");
        unlockBtn.setKeyboard(null); // Будет обновлено динамически кнопками
        unlockBtn.setCallbackData("unlock_" + targetUuid);
        
        row1.add(lockBtn);
        row1.add(unlockBtn);

        List<InlineKeyboardButton> row2 = new ArrayList<>();
        InlineKeyboardButton kickBtn = new InlineKeyboardButton();
        kickBtn.setText("🥾 Кикнуть");
        kickBtn.setCallbackData("kick_" + targetUuid);
        row2.add(kickBtn);

        rowsInline.add(row1);
        rowsInline.add(row2);
        markupInline.setKeyboard(rowsInline);
        message.setReplyMarkup(markupInline);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public void sendMsg(String chatId, String text) {
        SendMessage msg = new SendMessage();
        msg.setChatId(chatId);
        msg.setText(text);
        try {
            execute(msg);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void editMsg(String chatId, int messageId, String newText) {
        EditMessageText edit = new EditMessageText();
        edit.setChatId(chatId);
        edit.setMessageId(messageId);
        edit.setText(newText);
        try {
            execute(edit);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
