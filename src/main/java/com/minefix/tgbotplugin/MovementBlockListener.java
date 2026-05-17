package com.example.telegramconsole;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MovementBlockListener implements Listener {

    private final TelegramConsolePlugin plugin;
    private final Map<UUID, BukkitTask> activeTimers = new HashMap<>();

    public MovementBlockListener(TelegramConsolePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        boolean isRegistered = plugin.getDatabaseManager().playerExists(player.getName());
        startAuthTimer(player, isRegistered);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        stopTimer(event.getPlayer().getUniqueId());
        PendingApproval.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();

        // Если игрок в списке таймера (не авторизован) — отменяем ходьбу и вращение головой
        if (activeTimers.containsKey(uuid)) {
            Location from = event.getFrom();
            Location to = event.getTo();
            
            if (to != null && (from.getX() != to.getX() || from.getY() != to.getY() || from.getZ() != to.getZ() 
                    || from.getYaw() != to.getYaw() || from.getPitch() != to.getPitch())) {
                event.setTo(from); 
            }
        }
    }

    private void startAuthTimer(Player player, boolean isRegistered) {
        UUID uuid = player.getUniqueId();
        
        BukkitTask task = new BukkitRunnable() {
            int timeLeft = 20;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    activeTimers.remove(uuid);
                    return;
                }

                if (timeLeft <= 0) {
                    cancel();
                    activeTimers.remove(uuid);
                    player.kickPlayer("§cВремя на авторизацию истекло!");
                    return;
                }

                // Логика проверки: если игрок уже ввел верный пароль и ждет клика кнопок 2FA в ТГ
                if (PendingApproval.isAwaiting(uuid)) {
                    player.sendMessage("§6[2FA] Подтвердите вход в вашем Telegram-боте! Осталось: " + timeLeft + " сек.");
                } else {
                    if (isRegistered) {
                        player.sendMessage("§cАвторизуйтесь! Осталось на регистратцию " + timeLeft + " сек регистрация: /login <Пароль>");
                    } else {
                        player.sendMessage("§cЗарегистрируйтесь! Осталось на регистратцию " + timeLeft + " сек регистрация: /reg <Пароль>");
                    }
                }

                timeLeft -= 3;
            }
        }.runTaskTimer(plugin, 0L, 60L); // 60 тиков = 3 секунды

        activeTimers.put(uuid, task);
    }

    public void stopTimer(UUID uuid) {
        if (activeTimers.containsKey(uuid)) {
            activeTimers.get(uuid).cancel();
            activeTimers.remove(uuid);
        }
    }
}
