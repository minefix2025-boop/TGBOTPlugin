package com.minefix.tgbotplugin;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.*;

public class TelegramBotImpl extends TelegramLongPollingBot {
    private final String token;
    private final String username;
    private final DataStore store;
    private final PluginMain plugin;
    private final List<Long> admins;

    public TelegramBotImpl(String token, String username, DataStore store, PluginMain plugin) {
        this.token = token;
        this.username = username;
        this.store = store;
        this.plugin = plugin;
        List<Long> tmp = new ArrayList<>();
        List<?> list = plugin.getConfig().getList("admins");
        if (list != null) for (Object o : list) tmp.add(Long.parseLong(o.toString()));
        this.admins = Collections.unmodifiableList(tmp);
    }

    public void start() {
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(this);
            plugin.getLogger().info("Telegram bot запущен");
        } catch (Exception ex) {
            plugin.getLogger().severe("Не удалось запустить Telegram bot: " + ex.getMessage());
        }
    }

    public void stop() {
        // nothing special
    }

    @Override
    public String getBotUsername() { return username; }

    @Override
    public String getBotToken() { return token; }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage()) {
            Message msg = update.getMessage();
            String text = msg.getText();
            Long chatId = msg.getChatId();
            if (text != null && text.startsWith("/link ")) {
                String code = text.substring(6).trim();
                UUID uuid = store.consumeLinkCode(code);
                if (uuid != null) {
                    // attach chat id to player
                    PlayerData pd = store.getPlayerData(uuid);
                    if (pd != null) {
                        // update sqlite directly
                        try { // simple direct SQL to set telegram_chat
                            ((SqliteDataStore)store).init(); // ensure connection present
                        } catch (Exception ignored) {}
                        // set via SQL
                        try (java.sql.Connection c = java.sql.DriverManager.getConnection("jdbc:sqlite:" + plugin.getDataFolder().toPath().resolve("players.db").toString())) {
                            try (java.sql.PreparedStatement ps = c.prepareStatement("UPDATE players SET telegram_chat = ? WHERE uuid = ?")) {
                                ps.setLong(1, chatId);
                                ps.setString(2, uuid.toString());
                                ps.executeUpdate();
                            }
                        } catch (Exception e) { plugin.getLogger().warning(e.getMessage()); }
                        sendSimpleMessage(chatId, "Успешно привязано к аккаунту " + pd.getName());
                        return;
                    }
                }
                sendSimpleMessage(chatId, "Неверный или просроченный код привязки");
                return;
            }
            if (text != null && text.equalsIgnoreCase("/start")) {
                sendSimpleMessage(chatId, "Привет! Отправьте код привязки: /link <код>");
            }
        }

        if (update.hasCallbackQuery()) {
            CallbackQuery cb = update.getCallbackQuery();
            String data = cb.getData();
            Long from = cb.getFrom().getId();
            if (data != null && data.startsWith("login:")) {
                String id = data.substring(6);
                // only admins may press
                if (!admins.contains(from)) {
                    answerCallback(cb.getId(), "Недостаточно прав");
                    return;
                }
                // if admin clicked, accept for all
                store.completeApproval(id, true);
                answerCallback(cb.getId(), "Вход подтверждён");
                // notify chat
                for (Long a : admins) sendSimpleMessage(a, "Заявка " + id + " подтверждена администратором " + from);
            }
        }
    }

    private void answerCallback(String callbackId, String text) {
        AnswerCallbackQuery acq = new AnswerCallbackQuery();
        acq.setCallbackQueryId(callbackId);
        acq.setText(text);
        try { execute(acq); } catch (TelegramApiException e) { plugin.getLogger().warning(e.getMessage()); }
    }

    private void sendSimpleMessage(Long chatId, String text) {
        SendMessage msg = new SendMessage();
        msg.setChatId(chatId.toString());
        msg.setText(text);
        try { execute(msg); } catch (TelegramApiException e) { plugin.getLogger().warning(e.getMessage()); }
    }

    public void sendLoginApprovalToAdmins(String approvalId, UUID playerUuid, String ip) {
        String text = "Вход в Аккаунт с IP: " + ip + " UUID: " + playerUuid.toString() + "\nНажмите чтобы принять для всех";
        for (Long admin : admins) {
            SendMessage msg = new SendMessage();
            msg.setChatId(admin.toString());
            msg.setText(text);

            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            InlineKeyboardButton accept = new InlineKeyboardButton();
            accept.setText("Принять для всех");
            accept.setCallbackData("login:" + approvalId);
            List<InlineKeyboardButton> row = Collections.singletonList(accept);
            markup.setKeyboard(Collections.singletonList(row));
            msg.setReplyMarkup(markup);
            try { execute(msg); } catch (TelegramApiException e) { plugin.getLogger().warning(e.getMessage()); }
        }
    }
}
