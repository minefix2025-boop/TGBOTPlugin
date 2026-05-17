package com.minefix.tgbotplugin;

import java.util.UUID;

public class PlayerData {
    private UUID uuid;
    private String name;
    private String passwordHash; // bcrypt
    private Long telegramChatId; // nullable
    private boolean locked;

    public PlayerData(UUID uuid, String name, String passwordHash, Long telegramChatId, boolean locked) {
        this.uuid = uuid;
        this.name = name;
        this.passwordHash = passwordHash;
        this.telegramChatId = telegramChatId;
        this.locked = locked;
    }

    public UUID getUuid() { return uuid; }
    public String getName() { return name; }
    public String getPasswordHash() { return passwordHash; }
    public Long getTelegramChatId() { return telegramChatId; }
    public boolean isLocked() { return locked; }

    public void setTelegramChatId(Long telegramChatId) { this.telegramChatId = telegramChatId; }
    public void setLocked(boolean locked) { this.locked = locked; }
}
