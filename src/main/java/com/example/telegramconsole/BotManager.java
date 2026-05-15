package com.example.telegramconsole;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BotManager extends TelegramLongPollingBot {

    private final String token;
    private final long adminId;
    private final TelegramConsolePlugin plugin;
    private TelegramBotsApi botsApi;

    public BotManager(String token, long adminId, TelegramConsolePlugin plugin) {
        this.token = token;
        this.adminId = adminId;
        this.plugin = plugin;
    }

    public boolean start() {
        try {
            this.botsApi = new TelegramBotsApi(DefaultBotSession.class);
            this.botsApi.registerBot(this);
            return true;
        } catch (TelegramApiException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void stop() {}

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
            long chatId = update.getMessage().getChatId();

            if (chatId == adminId && text.startsWith("/cmd ")) {
                String cmd = text.substring(5);
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    boolean success = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
                    sendMsg(adminId, success ? "✅ Команда передана в консоль!" : "❌ Ошибка при отправке команды.");
                });
                return;
            }

            if (text.equals("/link")) {
                sendMsg(chatId, "Перейдите на игровой сервер и пропишите команду /link для генерации ключа связки.");
                return;
            }

            if (plugin.getLinkCodes().containsKey(text.toUpperCase())) {
                UUID pUUID = plugin.getLinkCodes().get(text.toUpperCase());
                Player player = Bukkit.getPlayer(pUUID);
                if (player != null && player.isOnline()) {
                    plugin.getDatabaseManager().setTelegramId(pUUID, chatId);
                    plugin.getLinkCodes().remove(text.toUpperCase());
                    sendMsg(chatId, "✅ Подключение успешно выполнено к аккаунту: " + player.getName());
                    player.sendMessage("§a[TG] Профиль успешно подключен к вашему Telegram!");
                    showAccountMenu(chatId, pUUID);
                } else {
                    sendMsg(chatId, "❌ Игрок не найден в сети сервера.");
                }
                return;
            }
            sendMsg(chatId, "🤖 Доступные операции: /link\nАдминистраторам: /cmd <команда>");
        } else if (update.hasCallbackQuery()) {
            String data = update.getCallbackQuery().getData();
            long chatId = update.getCallbackQuery().getMessage().getChatId();

            if (data.startsWith("status_")) {
                UUID uuid = UUID.fromString(data.substring(7));
                String name = Bukkit.getOfflinePlayer(uuid).getName();
                String ip = plugin.getDatabaseManager().getLastIp(uuid);
                sendMsg(chatId, "📊 Информация об аккаунте " + name + ":\n• Последний IP: " + ip + "\n• Статус защиты: 2FA Активна");
            } else if (data.startsWith("lock_")) {
                UUID uuid = UUID.fromString(data.substring(5));
                plugin.getDatabaseManager().setLocked(uuid, true);
                sendMsg(chatId, "🔒 Сессия заморожена. Доступ на сервер закрыт.");
            } else if (data.startsWith("unlock_")) {
                UUID uuid = UUID.fromString(data.substring(7));
                plugin.getDatabaseManager().setLocked(uuid, false);
                sendMsg(chatId, "🔓 Сессия разморожена. Доступ на сервер восстановлен.");
            } else if (data.startsWith("kick_")) {
                UUID uuid = UUID.fromString(data.substring(5));
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    Player p = Bukkit.getPlayer(uuid);
                    if (p != null) p.kickPlayer("§cВы исключены из игры удаленно через Telegram.");
                });
                sendMsg(chatId, "⚡ Запрос на отключение игрока обработан.");
            }
        }
    }

    public void sendMsg(long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        try { execute(message); } catch (TelegramApiException e) { e.printStackTrace(); }
    }

    public void showAccountMenu(long chatId, UUID uuid) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("⚙️ Панель управления аккаунтом Minecraft:");

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        List<InlineKeyboardButton> row1 = new ArrayList<>();
        InlineKeyboardButton btn1 = new InlineKeyboardButton();
        btn1.setText("📊 Инфо/IP");
        btn1.setCallbackData("status_" + uuid);
        
        InlineKeyboardButton btn2 = new InlineKeyboardButton();
        btn2.setText("⚡ Кикнуть");
        btn2.setCallbackData("kick_" + uuid);
        
        row1.add(btn1); row1.add(btn2);

        List<InlineKeyboardButton> row2 = new ArrayList<>();
        InlineKeyboardButton btn3 = new InlineKeyboardButton();
        btn3.setText("🔒 Залочить");
        btn3.setCallbackData("lock_" + uuid);
        
        InlineKeyboardButton btn4 = new InlineKeyboardButton();
        btn4.setText("🔓 Разлочить");
        btn4.setCallbackData("unlock_" + uuid);
        
        row2.add(btn3); row2.add(btn4);

        rows.add(row1); rows.add(row2);
        markup.setKeyboard(rows);
        message.setReplyMarkup(markup);
        try { execute(message); } catch (TelegramApiException e) { e.printStackTrace(); }
    }
}
