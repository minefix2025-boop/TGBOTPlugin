package com.example.telegramconsole;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class RegCommand implements CommandExecutor {
    private final TelegramConsolePlugin plugin;

    public RegCommand(TelegramConsolePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player p = (Player) sender;

        if (args.length < 1) {
            p.sendMessage("§cИспользование: /reg <пароль>");
            return true;
        }

        if (plugin.getDatabaseManager().playerExists(p.getName())) {
            p.sendMessage("§cВы уже зарегистрированы! Используйте /login <пароль>");
            return true;
        }

        // Передаем args[0]
        if (plugin.getDatabaseManager().registerPlayer(p.getName(), args[0])) {
            p.sendMessage("§aВы успешно зарегистрировались! Защитите профиль: /link");
            plugin.getMovementBlockListener().stopTimer(p.getUniqueId());
        } else {
            p.sendMessage("§cПроизошла ошибка при регистрации. Попробуйте позже.");
        }
        return true;
    }
}
