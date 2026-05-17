package com.minefix.tgbotplugin;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class LinkCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cЭту команду может использовать только игрок!");
            return true;
        }

        Player player = (Player) sender;
        String code = String.valueOf((int) (Math.random() * 899999) + 100000);
        
        // Помещаем код в общую статическую память бота
        TelegramBotImpl.pendingCodes.put(code, player.getName());

        player.sendMessage("§⚡ §aВаш код привязки Telegram: §e§l" + code);
        return true;
    }
}
