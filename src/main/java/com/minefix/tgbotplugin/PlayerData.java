package com.minefix.tgbotplugin;

import java.util.UUID;

public class PlayerData {
    private final UUID uuid;
    private boolean isAuthenticated;
    private String telegramChatId;

    public PlayerData(UUID uuid) {
        this.uuid = uuid;
        this.isAuthenticated = false;
        this.telegramChatId = null;
    }

    public UUID getUuid() {
        return uuid;
    }

    public boolean isAuthenticated() {
        return isAuthenticated;
    }

    public void setAuthenticated(boolean authenticated) {
        this.isAuthenticated = authenticated;
    }

    public String getTelegramChatId() {
        return telegramChatId;
    }

    public void setTelegramChatId(String telegramChatId) {
        this.telegramChatId = telegramChatId;
    }
}
