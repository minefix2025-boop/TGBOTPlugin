package com.minefix.tgbotplugin;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class JoinListener implements Listener {

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String nick = player.getName();
        
        SqliteDataStore.updateLastIp(nick, player.getAddress().getAddress().getHostAddress());
        PluginMain.getMovementBlockListener().startTimer(player);
    }
}
