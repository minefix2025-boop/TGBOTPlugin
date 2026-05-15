package com.example.telegramconsole;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        String playerName = event.getPlayer().getName();
        String ip = event.getPlayer().getAddress() != null ? 
                event.getPlayer().getAddress().getAddress().getHostAddress() : "Unknown";

        DatabaseManager db = TelegramConsolePlugin.getInstance().getDatabaseManager();

        if (!db.playerExists(playerName)) {
            event.getPlayer().sendMessage("═══════════════════════════════════════");
            event.getPlayer().sendMessage("🎉 Добро пожаловать на сервер!");
            event.getPlayer().sendMessage("═══════════════════════════════════════");
            event.getPlayer().sendMessage("📝 Сначала зарегистрируйся: /reg <пароль>");
            event.getPlayer().sendMessage("════════════════════════════════════════");
            return;
        }

        if (db.isPlayerLocked(playerName)) {
            event.getPlayer().kickPlayer("🚫 Ваш аккаунт заблокирован!\n📧 Свяжитесь с админом");
            return;
        }

        event.getPlayer().sendMessage("✅ С возвращением! Используй /login <пароль>");
        db.recordLoginIP(playerName, ip);

        TelegramConsolePlugin.getInstance().getLogger().info("👤 Игрок " + playerName + " присоединился (IP: " + ip + ")");
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        String playerName = event.getPlayer().getName();
        TelegramConsolePlugin.getInstance().getLogger().info("👤 Игрок " + playerName + " отключился");
    }
}
