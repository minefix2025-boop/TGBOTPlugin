package minefix.tgbotplugin;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MovementBlockListener implements Listener {

    private final JavaPlugin plugin;
    private final Map<UUID, BukkitTask> activeTimers = new HashMap<>();

    public MovementBlockListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // Проверяем, зарегистрирован ли игрок в базе данных (пример логики)
        boolean isRegistered = DataStore.isRegistered(uuid); 

        startAuthTimer(player, isRegistered);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        stopTimer(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // Если игрок еще не авторизован/зарегистрирован — полностью отменяем движение и повороты головы
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

                if (isRegistered) {
                    player.sendMessage("§cАвторизуйтесь! Осталось на авторизацию " + timeLeft + " сек. Авторизация: §e/login <Пароль>");
                } else {
                    player.sendMessage("§cЗарегистрируйтесь! Осталось на регистрацию " + timeLeft + " сек. Регистрация: §e/reg <Пароль>");
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
