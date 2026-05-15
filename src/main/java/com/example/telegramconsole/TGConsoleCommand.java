package com.example.telegramconsole;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class TGConsoleCommand implements CommandExecutor {
    private final TelegramConsolePlugin plugin;
    public TGConsoleCommand(TelegramConsolePlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("tgconsole.admin")) {
            sender.sendMessage("§cУ вас нет прав!");
            return true;
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            plugin.reloadConfig();
            sender.sendMessage("§a[TG] Конфигурация успешно перезагружена!");
            return true;
        }
        if (args.length > 1 && args[0].equalsIgnoreCase("token")) {
            plugin.getConfig().set("bot-token", args[1]);
            plugin.saveConfig();
            sender.sendMessage("§a[TG] Токен изменен! Перезапустите сервер.");
            return true;
        }
        sender.sendMessage("§6Панель управления:\n§7/tgconsole reload\n§7/tgconsole token <токен>");
        return true;
    }
}
