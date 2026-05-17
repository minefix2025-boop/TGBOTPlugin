package com.minefix.tgbotplugin;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public class MovementBlockListener implements Listener {
    private final DataStore store;

    public MovementBlockListener(DataStore store) { this.store = store; }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        if (!store.isAuthorized(p.getUniqueId())) {
            Location from = e.getFrom();
            Location to = e.getTo();
            if (to == null) return;
            if (from.getBlockX() != to.getBlockX() || from.getBlockZ() != to.getBlockZ()) {
                e.setTo(new Location(from.getWorld(), from.getX(), from.getY(), from.getZ(), from.getYaw(), from.getPitch()));
            } else {
                to.setYaw(from.getYaw());
                to.setPitch(from.getPitch());
                e.setTo(to);
            }
        }
    }
}
