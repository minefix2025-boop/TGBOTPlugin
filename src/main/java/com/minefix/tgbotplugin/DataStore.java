package com.minefix.tgbotplugin;

import java.util.UUID;

public class DataStore {

    private static SqliteDataStore sqlite;

    public static void init(SqliteDataStore instance) {
        sqlite = instance;
    }

    public static boolean isRegistered(UUID uuid) {
        return sqlite.isRegistered(uuid);
    }

    public static boolean registerPlayer(UUID uuid, String password) {
        return sqlite.registerPlayer(uuid, password);
    }

    public static boolean checkPassword(UUID uuid, String password) {
        return sqlite.checkPassword(uuid, password);
    }

    public static void saveTempLinkCode(UUID uuid, String code) {
        sqlite.saveTempLinkCode(uuid, code);
    }

    public static UUID getPlayerByLinkCode(String code) {
        return sqlite.getPlayerByLinkCode(code);
    }

    public static void bindTelegram(UUID uuid, String chatId) {
        sqlite.bindTelegram(uuid, chatId);
    }

    public static String getTelegramChatId(UUID uuid) {
        return sqlite.getTelegramChatId(uuid);
    }

    public static void setPlayerBlocked(UUID uuid, boolean blocked) {
        sqlite.setPlayerBlocked(uuid, blocked);
    }

    public static boolean isPlayerBlocked(UUID uuid) {
        return sqlite.isPlayerBlocked(uuid);
    }
}
