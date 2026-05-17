package com.minefix.tgbotplugin;

import java.util.UUID;

public interface DataStore {
    void init();
    void close();

    boolean isRegistered(UUID uuid);
    void savePlayerPassword(UUID uuid, String name, String bcryptHash);
    PlayerData getPlayerData(UUID uuid);
    void setAuthorized(UUID uuid, boolean authorized);
    boolean isAuthorized(UUID uuid);
    void registerFailedAttempt(UUID uuid);

    String createLoginApproval(UUID uuid, String ip, long ttlSeconds);
    PendingApproval getApprovalById(String id);
    void completeApproval(String id, boolean accepted);

    String createLinkCode(UUID uuid, long ttlSeconds);
    UUID consumeLinkCode(String code);
}
