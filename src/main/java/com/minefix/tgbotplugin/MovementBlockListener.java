package com.minefix.tgbotplugin;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.async.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.HashMap;
import java.util.UUID;

public class MovementBlockListener implements Listener {

    private final HashMap<UUID, Integer> authTimers = new HashMap<>();

    public void startTimer(Player player) {
        UUID uuid = player.getUniqueId();
        stopTimer(uuid);

        int taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(PluginMain.getInstance(), new Runnable() {
            int secondsLeft = 60;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    stopTimer(uuid);
                    return;
                }

                if (DataStore.isRegistered(uuid) && !com.minefix.tgbotplugin.PendingApproval.contains(uuid)) {
                    stopTimer(uuid);
                    return;
                }

                if (secondsLeft <= 0) {
                    player.kickPlayer("§cВремя на авторизацию истекло!");
                    stopTimer(uuid);
                } else {
                    if (secondsLeft % 20 == 0 || secondsLeft <= 10) {
                        if (!DataStore.isRegistered(uuid)) {
                            player.sendMessage("§cПожалуйста, авторизуйтесь: /reg <пароль> или /login <пароль> (" + secondsLeft + " сек)");
                        } else if (com.minefix.tgbotplugin.PendingApproval.contains(uuid)) {
                            player.sendMessage("§eОжидание подтверждения входа через Telegram... (" + secondsLeft + " сек)");
                        }
                    }
                    secondsLeft--;
                }
            }
        }, 0L, 20L);

        authTimers.put(uuid, taskId);
    }

    public void stopTimer(UUID uuid) {
        if (authTimers.containsKey(uuid)) {
            Bukkit.getScheduler().cancelTask(authTimers.get(uuid));
            authTimers.remove(uuid);
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        if (!DataStore.isRegistered(uuid) || com.minefix.tgbotplugin.PendingApproval.contains(uuid)) {
            event.setTo(event.getFrom());
        }
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        if (!DataStore.isRegistered(uuid) || com.minefix.tgbotplugin.PendingApproval.contains(uuid)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§c❌ Вы не можете писать в чат до полной авторизации!");
        }
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        String message = event.getMessage().toLowerCase();

        if (!DataStore.isRegistered(uuid) || com.minefix.tgbotplugin.PendingApproval.contains(uuid)) {
            if (!message.startsWith("/login ") && !message.startsWith("/reg ")) {
                event.setCancelled(true);
                event.getPlayer().sendMessage("§c❌ Использование команд заблокировано до подтверждения входа!");
            }
        }
    }
}
