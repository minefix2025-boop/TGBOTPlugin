package com.example.telegramconsole;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;

public class PlayerListener implements Listener {
    private final TelegramConsolePlugin plugin;
    public PlayerListener(TelegramConsolePlugin plugin) { this.plugin = plugin; }

    @EventHandler
    public void onLogin(PlayerLoginEvent event) {
        Player p = event.getPlayer();
        // Используем проверку блокировки по имени
        if (plugin.getDatabaseManager().isPlayerLocked(p.getName())) {
            event.disallow(PlayerLoginEvent.Result.KICK_OTHER, "§c[TG] Ваш аккаунт заблокирован владельцем через Telegram или из-за подбора пароля!");
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player p = event.getPlayer();
        String ip = p.getAddress().getAddress().getHostAddress();
        // Записываем IP по имени
        plugin.getDatabaseManager().recordLoginIP(p.getName(), ip);

        // Проверяем существование аккаунта по имени
        if (!plugin.getDatabaseManager().playerExists(p.getName())) {
            p.sendMessage("§eДобро пожаловать! Пожалуйста, зарегистрируйтесь: /reg <пароль>");
        } else {
            p.sendMessage("§eПожалуйста, авторизуйтесь: /login <пароль>");
        }
    }
}
