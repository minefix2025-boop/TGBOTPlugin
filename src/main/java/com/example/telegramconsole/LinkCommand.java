package com.example.telegramconsole;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class LinkCommand implements CommandExecutor {
    private final TelegramConsolePlugin plugin;
    public LinkCommand(TelegramConsolePlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Команда только для игроков!");
            return true;
        }
        Player player = (Player) sender;
        String code = plugin.generateLinkCode(player.getUniqueId());
        player.sendMessage("§6[TG] Отправьте этот код вашему Telegram-боту: §b" + code);
        return true;
    }
}
