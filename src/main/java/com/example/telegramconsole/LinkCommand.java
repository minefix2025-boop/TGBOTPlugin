package com.example.telegramconsole;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class LinkCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command!");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (args.length != 1) {
            player.sendMessage("§cUsage: /link <code>");
            return true;
        }
        
        String code = args[0];
        boolean success = BotManager.linkAccount(player.getUniqueId().toString(), player.getName(), code);
        
        if (success) {
            player.sendMessage("§a✓ Telegram account linked successfully!");
        } else {
            player.sendMessage("§c✗ Invalid or expired code!");
        }
        
        return true;
    }
}
