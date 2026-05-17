package com.minefix.tgbotplugin;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import java.util.UUID;

// ИСПРАВЛЕНО: Добавлен недостающий импорт
import com.minefix.tgbotplugin.DataStore;

public class JoinListener implements Listener {

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (DataStore.isPlayerBlocked(uuid)) {
            event.setJoinMessage(null);
            player.kickPlayer("§cВаш аккаунт заблокирован администратором через Telegram!");
        }
    }
}
