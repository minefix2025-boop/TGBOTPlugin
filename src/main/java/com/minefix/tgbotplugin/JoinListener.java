package com.minefix.tgbotplugin;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class JoinListener implements Listener {
    private final DataStore store;
    private final Map<UUID, BukkitRunnable> tasks = new ConcurrentHashMap<>();

    public JoinListener(DataStore store) { this.store = store; }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        if (!store.isRegistered(p.getUniqueId())) {
            int timeout = PluginMain.getInstance().getConfig().getInt("registration.timeout-seconds", 20);
            int interval = PluginMain.getInstance().getConfig().getInt("registration.reminder-interval-seconds", 3);
            BukkitRunnable task = new BukkitRunnable() {
                int left = timeout;
                @Override
                public void run() {
                    if (store.isRegistered(p.getUniqueId())) { cancel(); tasks.remove(p.getUniqueId()); return; }
                    if (!p.isOnline()) { cancel(); tasks.remove(p.getUniqueId()); return; }
                    p.sendMessage("Зарегистрируйтесь. Осталось " + left + " сек. Регистрация: /reg <пароль>");
                    left -= interval;
                    if (left <= 0) {
                        p.kickPlayer("Вы не прошли регистрацию");
                        cancel(); tasks.remove(p.getUniqueId());
                    }
                }
            };
            task.runTaskTimer(PluginMain.getInstance(), 0L, interval * 20L);
            tasks.put(p.getUniqueId(), task);
        }
    }
}
