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
        // 1. ОБРАБОТКА ТЕКСТОВЫХ КОМАНД
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            String chatId = update.getMessage().getChatId().toString();

            // Игрок вводит: /link <код>
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

            // Команда админа: /profile <Ник>
            if (adminIds.contains(chatId) && messageText.startsWith("/profile ")) {
                String[] parts = messageText.split(" ");
                if (parts.length > 1) {
                    String targetName = parts[1];
                    Player targetPlayer = Bukkit.getPlayer(targetName);
                    
                    if (targetPlayer != null) {
                        sendProfileMenu(chatId, targetPlayer.getUniqueId());
                    } else {
                        // Если оффлайн, ищем в базе данных по имени через SQLite (эмуляция UUID)
                        sendMsg(chatId, "👤 Игрок " + targetName + " сейчас оффлайн.\nПоследний IP: " + 
                                com.example.telegramconsole.TelegramConsolePlugin.getInstance().getDatabaseManager().playerExists(targetName));
                    }
                } else {
                    sendMsg(chatId, "❌ Используйте: /profile <Никнейм>");
                }
                return;
            }

            // ИСПРАВЛЕНО: Команда /cmd для консоли без дублирования текста
            if (adminIds.contains(chatId) && messageText.startsWith("/cmd ")) {
                String consoleCmd = messageText.substring(5); // Вырезаем "/cmd "
                Bukkit.getScheduler().runTask(plugin, () -> {
                    try {
                        com.example.telegramconsole.TelegramConsolePlugin.getInstance().getDatabaseManager().logCommand(chatId, consoleCmd);
                    } catch (Exception ignored) {}

                    boolean success = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), consoleCmd);
                    if (success) {
                        sendMsg(chatId, "💻 Консоль выполнила команду: `" + consoleCmd + "`");
                    } else {
                        sendMsg(chatId, "⚠️ Ошибка: Консоль отклонила команду `" + consoleCmd + "`");
                    }
                });
                return;
            }
        } 
        
        // 2. ОБРАБОТКА НАЖАТИЙ НА КНОПКИ (Inline Callback)
        else if (update.hasCallbackQuery()) {
            String callbackData = update.getCallbackQuery().getData();
            String chatId = update.getCallbackQuery().getMessage().getChatId().toString();
            long messageId = update.getCallbackQuery().getMessage().getMessageId();

            // Обработка кнопок 2FA входа (Принять / Отказать)
            if (callbackData.startsWith("2fa_accept_") || callbackData.startsWith("2fa_deny_")) {
                String[] parts = callbackData.split("_");
                UUID playerUuid = UUID.fromString(parts[2]);
                Player player = Bukkit.getPlayer(playerUuid);

                if (callbackData.startsWith("2fa_accept_")) {
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        if (player != null) {
                            player.sendMessage("§a[TG] Вход успешно подтвержден через Telegram! Приятной игры.");
                            PluginMain.getMovementBlockListener().stopTimer(playerUuid);
                            PendingApproval.remove(playerUuid);
                        }
                    });
                    editMsg(chatId, (int) messageId, "✅ Доступ разрешен. Игрок успешно вошел на сервер.");
                } else {
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        if (player != null) {
                            player.kickPlayer("§cНе потвержден через Telegram");
                            PendingApproval.remove(playerUuid);
                        }
                    });
                    editMsg(chatId, (int) messageId, "❌ Доступ отклонен. Игрок кикнут с сервера.");
                }
                return;
            }

            // Обработка кнопок панели управления (Профиль / Информация / Переключатель 2FA)
            if (callbackData.contains("_")) {
                String[] parts = callbackData.split("_");
                String action = parts[0];
                UUID targetUuid = UUID.fromString(parts[1]);
                Player targetPlayer = Bukkit.getPlayer(targetUuid);

                // Динамическое переключение режима 2FA одной кнопкой
                if (action.equals("toggle2fa")) {
                    boolean current2fa = DataStore.isPlayerBlocked(targetUuid); // Используем как триггер 2FA статуса
                    DataStore.setPlayerBlocked(targetUuid, !current2fa);
                    
                    // Обновляем это же меню, чтобы кнопка визуально изменилась
                    updateProfileMenu(chatId, (int) messageId, targetUuid);
                    return;
                }

                // Кнопка подробной Информации
                if (action.equals("info")) {
                    if (targetPlayer != null && targetPlayer.isOnline()) {
                        sendMsg(chatId, "ℹ️ Информация об игроке (ОНЛАЙН):\nНик: " + targetPlayer.getName() + 
                                "\nUUID: " + targetUuid + "\nТекущий IP: " + targetPlayer.getAddress().getAddress().getHostAddress());
                    } else {
                        sendMsg(chatId, "ℹ️ Информация об игроке (ОФФЛАЙН):\nUUID: " + targetUuid + 
                                "\nСтатус: Игрок покинул сервер.");
                    }
                    return;
                }

                // Действия администратора (Kick / Lock / Unlock)
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    switch (action) {
                        case "kick":
                            if (targetPlayer != null) {
                                targetPlayer.kickPlayer("§cВы были кикнуты администратором через Telegram.");
                                editMsg(chatId, (int) messageId, "🥾 Игрок был успешно кикнут.");
                            } else {
                                sendMsg(chatId, "❌ Операция невозможна: Игрок не в сети.");
                            }
                            break;
                        case "lock":
                            com.example.telegramconsole.TelegramConsolePlugin.getInstance().getDatabaseManager().setPlayerBlocked(targetPlayer != null ? targetPlayer.getName() : targetUuid.toString(), true);
                            if (targetPlayer != null) targetPlayer.kickPlayer("§cВаш аккаунт заблокирован через Telegram.");
                            editMsg(chatId, (int) messageId, "🔒 Аккаунт игрока успешно заблокирован.");
                            break;
                        case "unlock":
                            com.example.telegramconsole.TelegramConsolePlugin.getInstance().getDatabaseManager().setPlayerBlocked(targetPlayer != null ? targetPlayer.getName() : targetUuid.toString(), false);
                            editMsg(chatId, (int) messageId, "🔓 Аккаунт игрока успешно разблокирован.");
                            break;
                    }
                });
            }
        }
    }

    // Запрос подтверждения 2FA при входе (с Никнеймом, IP и UUID)
    public void send2FARequest(Player player) {
        String chatId = DataStore.getTelegramChatId(player.getUniqueId());
        if (chatId == null) return; 

        String ip = player.getAddress().getAddress().getHostAddress();
        UUID uuid = player.getUniqueId();
        String name = player.getName();

        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("🔔 Вход в Аккаунт\n👤 Никнейм: " + name + "\n🌐 IP: " + ip + "\n🆔 UUID: " + uuid.toString());

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

    // Создание меню управления профилем
    private void sendProfileMenu(String chatId, UUID targetUuid) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("📋 Управление профилем игрока:");
        message.setReplyMarkup(getProfileKeyboard(targetUuid));
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    // Обновление существующего меню профиля (для переключателя кнопок)
    private void updateProfileMenu(String chatId, int messageId, UUID targetUuid) {
        EditMessageText edit = new EditMessageText();
        edit.setChatId(chatId);
        edit.setMessageId(messageId);
        edit.setText("📋 Управление профилем игрока (Обновлено):");
        edit.setReplyMarkup(getProfileKeyboard(targetUuid));
        try {
            execute(edit);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    // Конструктор Inline клавиатуры профиля с динамической кнопкой 2FA
    private InlineKeyboardMarkup getProfileKeyboard(UUID targetUuid) {
        InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

        boolean is2faEnabled = DataStore.isPlayerBlocked(targetUuid); 

        // 1 Строка: Блокировка
        List<InlineKeyboardButton> row1 = new ArrayList<>();
        InlineKeyboardButton lockBtn = new InlineKeyboardButton();
        lockBtn.setText("🔒 Заблокировать");
        lockBtn.setCallbackData("lock_" + targetUuid);
        
        InlineKeyboardButton unlockBtn = new InlineKeyboardButton();
        unlockBtn.setText("🔓 Разблокировать");
        unlockBtn.setCallbackData("unlock_" + targetUuid);
        row1.add(lockBtn);
        row1.add(unlockBtn);

        // 2 Строка: Информация и динамическая кнопка 2FA
        List<InlineKeyboardButton> row2 = new ArrayList<>();
        InlineKeyboardButton infoBtn = new InlineKeyboardButton();
        infoBtn.setText("ℹ️ Информация");
        infoBtn.setCallbackData("info_" + targetUuid);

        InlineKeyboardButton tgl2faBtn = new InlineKeyboardButton();
        tgl2faBtn.setText(is2faEnabled ? "🛡️ 2FA: [ВКЛ]" : "❌ 2FA: [ВЫКЛ]");
        tgl2faBtn.setCallbackData("toggle2fa_" + targetUuid);
        row2.add(infoBtn);
        row2.add(tgl2faBtn);

        // 3 Строка: Кикнуть
        List<InlineKeyboardButton> row3 = new ArrayList<>();
        InlineKeyboardButton kickBtn = new InlineKeyboardButton();
        kickBtn.setText("🥾 Кикнуть игрока");
        kickBtn.setCallbackData("kick_" + targetUuid);
        row3.add(kickBtn);

        rowsInline.add(row1);
        rowsInline.add(row2);
        rowsInline.add(row3);
        markupInline.setKeyboard(rowsInline);
        return markupInline;
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
