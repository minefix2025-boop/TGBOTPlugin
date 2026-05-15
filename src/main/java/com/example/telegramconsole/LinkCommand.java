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
        // Используем метод генерации кода из базы данных по имени игрока
        String code = plugin.getDatabaseManager().generateLinkCode(player.getName());
        player.sendMessage("§6[TG] Отправьте этот код вашему Telegram-боту: §b" + code);
        return true;
    }
}
