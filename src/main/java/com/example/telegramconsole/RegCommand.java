package com.example.telegramconsole;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class RegCommand implements CommandExecutor {
    private final TelegramConsolePlugin plugin;
    public RegCommand(TelegramConsolePlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player p = (Player) sender;
        if (args.length < 1) {
            p.sendMessage("§cИспользование: /reg <пароль>");
            return true;
        }
        if (plugin.getDatabaseManager().isRegistered(p.getUniqueId())) {
            p.sendMessage("§cВы уже зарегистрированы! Используйте /login");
            return true;
        }
        plugin.getDatabaseManager().register(p.getUniqueId(), args[0]);
        p.sendMessage("§aВы успешно зарегистрировались! Теперь привяжите Telegram через /link");
        return true;
    }
}
