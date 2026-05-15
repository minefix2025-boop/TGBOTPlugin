package com.example.telegramconsole;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class LoginCommand implements CommandExecutor {
    private final TelegramConsolePlugin plugin;
    public LoginCommand(TelegramConsolePlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player p = (Player) sender;
        if (args.length < 1) {
            p.sendMessage("§cИспользование: /login <пароль>");
            return true;
        }
        if (!plugin.getDatabaseManager().playerExists(p.getName())) {
            p.sendMessage("§cВы еще не зарегистрированы! Используйте /reg");
            return true;
        }
        if (plugin.getDatabaseManager().isPlayerLocked(p.getName())) {
            p.sendMessage("§cВаш аккаунт временно заблокирован из-за частых ошибок ввода.");
            return true;
        }

        if (plugin.getDatabaseManager().loginPlayer(p.getName(), args[0])) {
            long tgId = plugin.getDatabaseManager().getTelegramId(p.getName());
            if (tgId != 0) {
                p.sendMessage("§6[2FA] Подтвердите вход в Telegram боте!");
                plugin.getBotManager().sendMsg(tgId, "🔔 Зафиксирован вход в аккаунт " + p.getName() + ".\nДля управления сессией используйте меню:");
                plugin.getBotManager().showAccountMenu(tgId, p.getName());
            } else {
                p.sendMessage("§aВход успешен! Защитите свой профиль: /link");
            }
        } else {
            plugin.getDatabaseManager().recordFailedLogin(p.getName());
            p.sendMessage("§cНеверный пароль!");
        }
        return true;
    }
}
