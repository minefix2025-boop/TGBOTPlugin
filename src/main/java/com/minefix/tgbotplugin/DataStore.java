package com.minefix.tgbotplugin;

import java.util.UUID;

public class DataStore {

    public static boolean isRegistered(UUID uuid) {
        return SqliteDataStore.isRegistered(uuid);
    }

    public static void registerPlayer(UUID uuid, String password) {
        SqliteDataStore.registerPlayer(uuid, password);
    }

    public static boolean checkPassword(UUID uuid, String password) {
        return SqliteDataStore.checkPassword(uuid, password);
    }

    public static String getTelegramChatId(UUID uuid) {
        Long chatId = SqliteDataStore.getChatIdByUuid(uuid);
        return chatId != null ? String.valueOf(chatId) : null;
    }
}
