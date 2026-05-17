package com.minefix.tgbotplugin;

public class PlayerData {
    private final String nickname;
    private final String lastIp;

    public PlayerData(String nickname, String lastIp) {
        this.nickname = nickname;
        this.lastIp = lastIp;
    }
    public String getNickname() { return nickname; }
    public String getLastIp() { return lastIp; }
}
