package minefix.tgbotplugin;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import java.util.UUID;

public class JoinListener implements Listener {

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // Если аккаунт заблокирован через Telegram-бота — кикаем
        if (DataStore.isPlayerBlocked(uuid)) {
            event.setJoinMessage(null);
            player.kickPlayer("§cВаш аккаунт заблокирован администратором через Telegram!");
        }
    }
}
