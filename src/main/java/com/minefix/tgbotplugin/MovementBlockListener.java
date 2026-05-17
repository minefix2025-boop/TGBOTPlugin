package com.minefix.tgbotplugin;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.HashMap;
import java.util.UUID;

public class MovementBlockListener implements Listener {

    // Хранилище запущенных таймеров для кика неавторизованных игроков
    private final HashMap<UUID, Integer> authTimers = new HashMap<>();

    // Метод запуска таймера (игрок должен войти за 60 секунд)
    public void startTimer(Player player) {
        UUID uuid = player.getUniqueId();
        
        // Отменяем старый таймер, если он почему-то был
        stopTimer(uuid);

        int taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(PluginMain.getInstance(), new Runnable() {
            int secondsLeft = 60;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    stopTimer(uuid);
                    return;
                }

                // Если игрок уже зарегистрировался/вошел и НЕ ждет 2FA, глушим таймер
                if (DataStore.isRegistered(uuid) && !PendingApproval.contains(uuid)) {
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
                        } else if (PendingApproval.contains(uuid)) {
                            player.sendMessage("§eОжидание подтверждения входа через Telegram... (" + secondsLeft + " сек)");
                        }
                    }
                    secondsLeft--;
                }
            }
        }, 0L, 20L); // Повторяем каждую секунду (20 тиков)

        authTimers.put(uuid, taskId);
    }

    // Метод остановки таймера кика
    public void stopTimer(UUID uuid) {
        if (authTimers.containsKey(uuid)) {
            Bukkit.getScheduler().cancelTask(authTimers.get(uuid));
            authTimers.remove(uuid);
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // Блокируем ходьбу, если игрок не вошел в аккаунт ИЛИ ждет 2FA клика в Telegram
        if (!DataStore.isRegistered(uuid) || PendingApproval.contains(uuid)) {
            event.setTo(event.getFrom());
        }
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (!DataStore.isRegistered(uuid) || PendingApproval.contains(uuid)) {
            event.setCancelled(true);
            player.sendMessage("§c❌ Вы не можете писать в чат до полной авторизации!");
        }
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        String message = event.getMessage().toLowerCase();

        // Разрешаем только команды авторизации, остальные блокируем
        if (!DataStore.isRegistered(uuid) || PendingApproval.contains(uuid)) {
            if (!message.startsWith("/login ") && !message.startsWith("/reg ")) {
                event.setCancelled(true);
                player.sendMessage("§c❌ Использование команд заблокировано до подтверждения входа!");
            }
        }
    }
}
