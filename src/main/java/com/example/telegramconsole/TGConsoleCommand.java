package com.example.telegramconsole;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class TGConsoleCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("telegramconsole.admin")) {
            sender.sendMessage("§cУ вас нет прав администратора для этой команды.");
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("status")) {
            sender.sendMessage("§a[TelegramConsole] Бот активен, сессия управления консолью стабильна.");
            return true;
        }

        sender.sendMessage("§eИспользование: /tgconsole status");
        return true;
    }
}
