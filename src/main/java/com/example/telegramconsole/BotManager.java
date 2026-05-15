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
                    sendMsg(adminId, success ? "✅ Команда передана в консоль!" : "❌ Ошибка выполнения.");
                });
                return;
            }

            if (text.equals("/link")) {
                sendMsg(chatId, "Пропишите /link на сервере Майнкрафт и отправьте сгенерированный код сюда.");
                return;
            }

            // Обработка текстового кода привязки через JSON-метод
            String linkedPlayer = plugin.getDatabaseManager().getPlayerByTgId(chatId);
            if (linkedPlayer == null && text.length() == 20) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (plugin.getDatabaseManager().linkPlayerToTelegram(player.getName(), text.toUpperCase())) {
                        plugin.getDatabaseManager().setTelegramId(player.getName(), chatId);
                        sendMsg(chatId, "✅ Успешное подключение к вашему аккаунту: " + player.getName());
                        player.sendMessage("§a[TG] Профиль привязан к Telegram!");
                        showAccountMenu(chatId, player.getName());
                        return;
                    }
                }
                sendMsg(chatId, "❌ Неверный или истекший код привязки.");
                return;
            }

            sendMsg(chatId, "🤖 Доступные команды: /link\nАдминистраторам: /cmd <команда>");
        } else if (update.hasCallbackQuery()) {
            String data = update.getCallbackQuery().getData();
            long chatId = update.getCallbackQuery().getMessage().getChatId();

            if (data.startsWith("status_")) {
                String name = data.substring(7);
                String ip = plugin.getDatabaseManager().getLastIp(name);
                sendMsg(chatId, "📊 Аккаунт " + name + ":\n• Последний IP: " + ip + "\n• Защита 2FA: Активна");
            } else if (data.startsWith("lock_")) {
                String name = data.substring(5);
                plugin.getDatabaseManager().setLocked(name, true);
                sendMsg(chatId, "🔒 Аккаунт " + name + " заморожен. Вход невозможен.");
            } else if (data.startsWith("unlock_")) {
                String name = data.substring(7);
                plugin.getDatabaseManager().setLocked(name, false);
                sendMsg(chatId, "🔓 Аккаунт " + name + " разблокирован.");
            } else if (data.startsWith("kick_")) {
                String name = data.substring(5);
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    Player p = Bukkit.getPlayer(name);
                    if (p != null) p.kickPlayer("§cВы были кикнуты владельцем через Telegram!");
                });
                sendMsg(chatId, "⚡ Игрок " + name + " кикнут.");
            }
        }
    }

    public void sendMsg(long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        try { execute(message); } catch (TelegramApiException e) { e.printStackTrace(); }
    }

    public void showAccountMenu(long chatId, String playerName) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("⚙️ Управление аккаунтом " + playerName + ":");

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        List<InlineKeyboardButton> row1 = new ArrayList<>();
        InlineKeyboardButton btn1 = new InlineKeyboardButton(); btn1.setText("📊 Инфо/IP"); btn1.setCallbackData("status_" + playerName);
        InlineKeyboardButton btn2 = new InlineKeyboardButton(); btn2.setText("⚡ Кикнуть"); btn2.setCallbackData("kick_" + playerName);
        row1.add(btn1); row1.add(btn2);

        List<InlineKeyboardButton> row2 = new ArrayList<>();
        InlineKeyboardButton btn3 = new InlineKeyboardButton(); btn3.setText("🔒 Залочить"); btn3.setCallbackData("lock_" + playerName);
        InlineKeyboardButton btn4 = new InlineKeyboardButton(); btn4.setText("🔓 Разлочить"); btn4.setCallbackData("unlock_" + playerName);
        row2.add(btn3); row2.add(btn4);

        rows.add(row1); rows.add(row2);
        markup.setKeyboard(rows);
        message.setReplyMarkup(markup);
        try { execute(message); } catch (TelegramApiException e) { e.printStackTrace(); }
    }
}
