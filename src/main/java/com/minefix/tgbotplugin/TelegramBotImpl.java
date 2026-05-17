package minefix.tgbotplugin;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

public class TelegramBotImpl extends TelegramLongPollingBot {

    private final PluginMain plugin;
    private final HashSet<Long> allowedAdmins = new HashSet<>();

    public TelegramBotImpl(PluginMain plugin) {
        this.plugin = plugin;
        // Белый список ID главных администраторов
        allowedAdmins.add(6343309173L);
        allowedAdmins.add(7742036100L);
    }

    @Override
    public String getBotUsername() {
        return "Potatogames2fa_bot"; 
    }

    @Override
    public String getBotToken() {
        return "8629251193:AAErpWdzt_vNpkfhlxN8aiXlLgWkfM7h5QQ";
    }

    @Override
    public void onUpdateReceived(Update update) {
        // 1. ОБРАБОТКА НАЖАТИЙ НА КНОПКИ ПОД КАРТОЧКАМИ (Inline Buttons)
        if (update.hasCallbackQuery()) {
            long chatId = update.getCallbackQuery().getMessage().getChatId();
            int messageId = update.getCallbackQuery().getMessage().getMessageId();
            String data = update.getCallbackQuery().getData();

            // Строгая проверка доступа к кнопкам
            if (!allowedAdmins.contains(chatId)) {
                return;
            }

            if (data.startsWith("2fa_accept_")) {
                String uuidStr = data.replace("2fa_accept_", "");
                Player player = Bukkit.getPlayer(UUID.fromString(uuidStr));
                if (player != null) {
                    plugin.getAuthManager().unfreezePlayer(player.getUniqueId());
                    player.sendMessage("§a[2FA] Вход успешно подтвержден через Telegram!");
                    editMsg(chatId, messageId, "✅ Доступ для игрока успешно **подтвержден**.");
                } else {
                    editMsg(chatId, messageId, "❌ Игрок уже вышел с сервера.");
                }
            } 
            else if (data.startsWith("2fa_deny_")) {
                String uuidStr = data.replace("2fa_deny_", "");
                Bukkit.getScheduler().runTask(plugin, () -> {
                    Player player = Bukkit.getPlayer(UUID.fromString(uuidStr));
                    if (player != null) {
                        player.kickPlayer("§cНе подтвержден через Telegram бота!");
                    }
                });
                editMsg(chatId, messageId, "❌ Вход отклонен. Игрок **кикнут** с сервера.");
            }
            return;
        }

        // 2. ОБРАБОТКА ТЕКСТОВЫХ СООБЩЕНИЙ И КОМАНД
        if (!update.hasMessage() || !update.getMessage().hasText()) return;

        long chatId = update.getMessage().getChatId();
        String text = update.getMessage().getText().trim();

        // Полный игнор любых левых пользователей
        if (!allowedAdmins.contains(chatId)) {
            sendMsg(chatId, "🔒 Ошибка доступа. Вас нет в белом списке администраторов бота.");
            return;
        }

        String linkedPlayer = plugin.getTelegramManager().getNickByChatId(chatId);

        // Если админ еще не привязан, принимаем пин-код из игры
        if (linkedPlayer == null) {
            String nick = plugin.getTelegramManager().getPlayerByCode(text);
            if (nick != null) {
                plugin.getTelegramManager().bindAccount(nick, chatId);
                plugin.getTelegramManager().removeCode(text);
                
                sendMenu(chatId, "🎉 Аккаунт успешно привязан к нику **" + nick + "**!\nДобро пожаловать в админ-панель.");
                
                Player p = Bukkit.getPlayer(nick);
                if (p != null) p.sendMessage("§a[Telegram] Ваш аккаунт успешно привязан к боту!");
            } else {
                sendMsg(chatId, "🔒 Доступ ограничен. Введите команду `/link` на сервере Minecraft и отправьте полученный числовой код сюда.");
            }
            return;
        }

        // 3. ОБРАБОТКА ГЛАВНОГО МЕНЮ ПОСЛЕ АВТОРИЗАЦИИ
        if (text.equals("ℹ️ Информация")) {
            Player p = Bukkit.getPlayer(linkedPlayer);
            if (p != null && p.isOnline()) {
                sendMsg(chatId, "🟢 **Статус:** Онлайн\n" +
                        "👤 **Ник:** " + p.getName() + "\n" +
                        "🆔 **UUID:** `" + p.getUniqueId() + "`\n" +
                        "🌐 **IP:** `" + p.getAddress().getAddress().getHostAddress() + "`");
            } else {
                PlayerData data = plugin.getDatabase().getOfflineData(linkedPlayer);
                sendMsg(chatId, "🔴 **Статус:** Офлайн\n" +
                        "👤 **Ник:** " + linkedPlayer + "\n" +
                        "🌐 **Последний IP:** `" + (data != null ? data.getLastIp() : "Нет данных") + "`");
            }
        } 
        else if (text.equals("🛡️ 2FA Статус")) {
            boolean is2faEnabled = plugin.getTelegramManager().is2faEnabled(linkedPlayer);
            plugin.getTelegramManager().set2faStatus(linkedPlayer, !is2faEnabled);
            sendMsg(chatId, "🛡️ Статус 2FA для аккаунта **" + linkedPlayer + "** изменен на: " + (!is2faEnabled ? "**[ВКЛЮЧЕН]** 🟢" : "**[ВЫКЛЮЧЕН]** 🔴"));
        } 
        else if (text.equals("🚪 Кикнуть себя")) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                Player p = Bukkit.getPlayer(linkedPlayer);
                if (p != null) {
                    p.kickPlayer("§cВы кикнули себя через Telegram-бота.");
                    sendMsg(chatId, "✅ Вы успешно кикнули своего персонажа.");
                } else {
                    sendMsg(chatId, "❌ Вы сейчас не на сервере.");
                }
            });
        }
        else if (text.startsWith("/cmd ")) {
            String command = text.substring(5);
            Bukkit.getScheduler().runTask(plugin, () -> {
                boolean success = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                if (success) {
                    sendMsg(chatId, "💻 Команда `" + command + "` успешно выполнена в консоли сервера.");
                } else {
                    sendMsg(chatId, "❌ Не удалось выполнить команду `" + command + "`.");
                }
            });
        }
    }

    // Отправка запроса авторизации 2FA админам при входе на сервер
    public void send2faRequest(long chatId, String nick, String ip, UUID uuid) {
        if (!allowedAdmins.contains(chatId)) return;

        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("⚠️ **Попытка входа в аккаунт!**\n\n" +
                "👤 **Ник:** " + nick + "\n" +
                "🌐 **IP:** `" + ip + "`\n" +
                "🆔 **UUID:** `" + uuid.toString() + "`\n\n" +
                "Подтвердите вход в игру с помощью кнопок ниже:");
        message.setParseMode("Markdown");

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        List<InlineKeyboardButton> row = new ArrayList<>();

        InlineKeyboardButton acceptBtn = new InlineKeyboardButton();
        acceptBtn.setText("✅ Принять");
        acceptBtn.setCallbackData("2fa_accept_" + uuid.toString());

        InlineKeyboardButton denyBtn = new InlineKeyboardButton();
        denyBtn.setText("❌ Отказать");
        denyBtn.setCallbackData("2fa_deny_" + uuid.toString());

        row.add(acceptBtn);
        row.add(denyBtn);
        rows.add(row);
        markup.setKeyboard(rows);
        message.setReplyMarkup(markup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void sendMenu(long chatId, String text) {
        SendMessage msg = new SendMessage();
        msg.setChatId(String.valueOf(chatId));
        msg.setText(text);
        msg.setParseMode("Markdown");

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setSelective(true);
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(false);

        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow row1 = new KeyboardRow();
        row1.add(new KeyboardButton("ℹ️ Информация"));
        row1.add(new KeyboardButton("🛡️ 2FA Статус"));
        
        KeyboardRow row2 = new KeyboardRow();
        row2.add(new KeyboardButton("🚪 Кикнуть себя"));

        keyboard.add(row1);
        keyboard.add(row2);
        keyboardMarkup.setKeyboard(keyboard);
        msg.setReplyMarkup(keyboardMarkup);

        try {
            execute(msg);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void sendMsg(long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        message.setParseMode("Markdown");
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void editMsg(long chatId, int messageId, String text) {
        EditMessageText message = new EditMessageText();
        message.setChatId(String.valueOf(chatId));
        message.setMessageId(messageId);
        message.setText(text);
        message.setParseMode("Markdown");
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
