package com.minefix.tgbotplugin;

import java.util.UUID;

public class PendingApproval {
    private String id;
    private UUID playerUuid;
    private Long chatId; // admin who can approve/was targeted (we will send to all admins but store origin chat id null)
    private String ip;
    private long expiresAt; // epoch millis

    public PendingApproval(String id, UUID playerUuid, Long chatId, String ip, long expiresAt) {
        this.id = id;
        this.playerUuid = playerUuid;
        this.chatId = chatId;
        this.ip = ip;
        this.expiresAt = expiresAt;
    }

    public String getId() { return id; }
    public UUID getPlayerUuid() { return playerUuid; }
    public Long getChatId() { return chatId; }
    public String getIp() { return ip; }
    public long getExpiresAt() { return expiresAt; }
}
