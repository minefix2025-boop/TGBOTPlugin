package com.example.telegramconsole;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonArray;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.UUID;

public class DatabaseManager {
    private final File dataFolder;
    private final File playersFile;
    private final File accountsFile;
    private final Gson gson;
    private JsonObject playersData;
    private JsonObject accountsData;

    public DatabaseManager(File dataFolder) {
        this.dataFolder = dataFolder;
        this.playersFile = new File(dataFolder, "players.json");
        this.accountsFile = new File(dataFolder, "accounts.json");
        this.gson = new GsonBuilder().setPrettyPrinting().create();

        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        loadData();
    }

    private void loadData() {
        if (playersFile.exists()) {
            try (FileReader reader = new FileReader(playersFile)) {
                playersData = JsonParser.parseReader(reader).getAsJsonObject();
            } catch (IOException e) {
                e.printStackTrace();
                playersData = new JsonObject();
            }
        } else {
            playersData = new JsonObject();
            savePlayersData();
        }

        if (accountsFile.exists()) {
            try (FileReader reader = new FileReader(accountsFile)) {
                accountsData = JsonParser.parseReader(reader).getAsJsonObject();
            } catch (IOException e) {
                e.printStackTrace();
                accountsData = new JsonObject();
            }
        } else {
            accountsData = new JsonObject();
            saveAccountsData();
        }
    }

    public void registerPlayer(String playerName, String password, String uuid) {
        JsonObject playerObj = new JsonObject();
        playerObj.addProperty("uuid", uuid);
        playerObj.addProperty("password", hashPassword(password));
        playerObj.addProperty("registered_at", System.currentTimeMillis());
        playerObj.addProperty("logged_in", false);
        playerObj.addProperty("locked", false);
        playerObj.addProperty("locked_until", 0);
        playerObj.addProperty("telegram_id", 0);
        playerObj.add("ips", new JsonArray());
        playerObj.add("telegrams", new JsonArray());

        playersData.add(playerName, playerObj);
        savePlayersData();
    }

    public boolean playerExists(String playerName) {
        return playersData.has(playerName);
    }

    public boolean loginPlayer(String playerName, String password) {
        if (!playerExists(playerName)) {
            return false;
        }

        JsonObject playerObj = playersData.getAsJsonObject(playerName);
        String storedHash = playerObj.get("password").getAsString();

        if (hashPassword(password).equals(storedHash)) {
            playerObj.addProperty("logged_in", true);
            playerObj.addProperty("last_login", System.currentTimeMillis());
            playerObj.addProperty("failed_attempts", 0);
            savePlayersData();
            return true;
        }

        return false;
    }

    public void setPlayerLoggedIn(String playerName, boolean loggedIn) {
        if (playerExists(playerName)) {
            playersData.getAsJsonObject(playerName).addProperty("logged_in", loggedIn);
            savePlayersData();
        }
    }

    public boolean isPlayerLoggedIn(String playerName) {
        if (!playerExists(playerName)) {
            return false;
        }
        return playersData.getAsJsonObject(playerName).get("logged_in").getAsBoolean();
    }

    public String generateLinkCode(String playerName) {
        String code = UUID.randomUUID().toString().substring(0, 20).toUpperCase();
        JsonObject codeObj = new JsonObject();
        codeObj.addProperty("player", playerName);
        codeObj.addProperty("created_at", System.currentTimeMillis());
        codeObj.addProperty("expires_at", System.currentTimeMillis() + 3600000); // 1 час

        accountsData.add(code, codeObj);
        saveAccountsData();

        return code;
    }

    public boolean linkPlayerToTelegram(String playerName, String code) {
        if (!accountsData.has(code)) {
            return false;
        }

        JsonObject codeObj = accountsData.getAsJsonObject(code);
        long expiresAt = codeObj.get("expires_at").getAsLong();

        if (System.currentTimeMillis() > expiresAt) {
            accountsData.remove(code);
            saveAccountsData();
            return false;
        }

        String codedPlayer = codeObj.get("player").getAsString();
        if (!codedPlayer.equals(playerName)) {
            return false;
        }

        accountsData.remove(code);
        saveAccountsData();

        return true;
    }

    // --- Новые добавленные методы для работы интеграции с ботом Telegram ---

    public void setTelegramId(String playerName, long chatId) {
        if (playerExists(playerName)) {
            JsonObject playerObj = playersData.getAsJsonObject(playerName);
            playerObj.addProperty("telegram_id", chatId);
            
            JsonArray telegrams = playerObj.getAsJsonArray("telegrams");
            if (telegrams == null) {
                telegrams = new JsonArray();
                playerObj.add("telegrams", telegrams);
            }
            telegrams.add(chatId);
            savePlayersData();
        }
    }

    public long getTelegramId(String playerName) {
        if (playerExists(playerName)) {
            JsonObject playerObj = playersData.getAsJsonObject(playerName);
            if (playerObj.has("telegram_id")) {
                return playerObj.get("telegram_id").getAsLong();
            }
        }
        return 0;
    }

    public void setLocked(String playerName, boolean lock) {
        if (playerExists(playerName)) {
            JsonObject playerObj = playersData.getAsJsonObject(playerName);
            playerObj.addProperty("locked", lock);
            if (lock) {
                playerObj.addProperty("locked_until", System.currentTimeMillis() + 315360000000L); // Почти навсегда (10 лет)
            } else {
                playerObj.addProperty("locked_until", 0);
                playerObj.addProperty("failed_attempts", 0);
            }
            savePlayersData();
        }
    }

    public String getLastIp(String playerName) {
        if (playerExists(playerName)) {
            JsonObject playerObj = playersData.getAsJsonObject(playerName);
            JsonArray ips = playerObj.getAsJsonArray("ips");
            if (ips != null && ips.size() > 0) {
                return ips.get(ips.size() - 1).getAsJsonObject().get("ip").getAsString();
            }
        }
        return "Неизвестен";
    }

    public String getPlayerByTgId(long chatId) {
        for (String name : playersData.keySet()) {
            JsonObject obj = playersData.getAsJsonObject(name);
            if (obj.has("telegram_id") && obj.get("telegram_id").getAsLong() == chatId) {
                return name;
            }
        }
        return null;
    }

    // --- Конец добавленных методов ---

    public void recordLoginIP(String playerName, String ip) {
        if (playerExists(playerName)) {
            JsonObject playerObj = playersData.getAsJsonObject(playerName);
            JsonArray ips = playerObj.getAsJsonArray("ips");
            
            JsonObject ipEntry = new JsonObject();
            ipEntry.addProperty("ip", ip);
            ipEntry.addProperty("timestamp", System.currentTimeMillis());
            
            ips.add(ipEntry);
            if (ips.size() > 20) {
                JsonArray newIps = new JsonArray();
                for (int i = ips.size() - 20; i < ips.size(); i++) {
                    newIps.add(ips.get(i));
                }
                playerObj.add("ips", newIps);
            }
            savePlayersData();
        }
    }

    public void recordFailedLogin(String playerName) {
        if (playerExists(playerName)) {
            JsonObject playerObj = playersData.getAsJsonObject(playerName);
            int attempts = playerObj.has("failed_attempts") ? playerObj.get("failed_attempts").getAsInt() : 0;
            playerObj.addProperty("failed_attempts", attempts + 1);

            if (attempts >= 2) {
                playerObj.addProperty("locked", true);
                playerObj.addProperty("locked_until", System.currentTimeMillis() + 1800000); // 30 минут
            }

            savePlayersData();
        }
    }

    public boolean isPlayerLocked(String playerName) {
        if (!playerExists(playerName)) {
            return false;
        }

        JsonObject playerObj = playersData.getAsJsonObject(playerName);
        if (!playerObj.has("locked") || !playerObj.get("locked").getAsBoolean()) {
            return false;
        }

        long lockedUntil = playerObj.get("locked_until").getAsLong();
        if (System.currentTimeMillis() > lockedUntil) {
            playerObj.addProperty("locked", false);
            playerObj.addProperty("failed_attempts", 0);
            savePlayersData();
            return false;
        }

        return true;
    }

    private String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return password;
        }
    }

    private void savePlayersData() {
        try (FileWriter writer = new FileWriter(playersFile)) {
            gson.toJson(playersData, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveAccountsData() {
        try (FileWriter writer = new FileWriter(accountsFile)) {
            gson.toJson(accountsData, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
