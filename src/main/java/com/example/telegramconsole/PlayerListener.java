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
        if (plugin.getDatabaseManager().isLocked(p.getUniqueId())) {
            event.disallow(PlayerLoginEvent.Result.KICK_OTHER, "§c[TG] Ваш аккаунт заблокирован владельцем через Telegram!");
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player p = event.getPlayer();
        String ip = p.getAddress().getAddress().getHostAddress();
        plugin.getDatabaseManager().setLastIp(p.getUniqueId(), ip);

        if (!plugin.getDatabaseManager().isRegistered(p.getUniqueId())) {
            p.sendMessage("§eДобро пожаловать! Пожалуйста, зарегистрируйтесь: /reg <пароль>");
        } else {
            p.sendMessage("§eПожалуйста, авторизуйтесь: /login <пароль>");
        }
    }
}
