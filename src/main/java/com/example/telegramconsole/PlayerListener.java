package com.example.telegramconsole;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerListener implements Listener {
    private final TelegramConsolePlugin plugin;

    public PlayerListener(TelegramConsolePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player p = event.getPlayer();
        String name = p.getName();
        String ip = p.getAddress().getAddress().getHostAddress();

        if (plugin.getDatabaseManager().isPlayerLocked(name)) {
            p.kickPlayer("§cВаш аккаунт временно заблокирован!");
            return;
        }

        plugin.getDatabaseManager().recordLoginIP(name, ip);
    }
}
