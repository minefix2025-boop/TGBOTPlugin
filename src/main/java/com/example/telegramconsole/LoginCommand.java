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
        if (!plugin.getDatabaseManager().isRegistered(p.getUniqueId())) {
            p.sendMessage("§cВы еще не зарегистрированы! Используйте /reg");
            return true;
        }
        if (plugin.getDatabaseManager().checkPassword(p.getUniqueId(), args[0])) {
            long tgId = plugin.getDatabaseManager().getTelegramId(p.getUniqueId());
            if (tgId != 0) {
                p.sendMessage("§6[2FA] Подтвердите вход в вашем Telegram боте!");
                plugin.getBotManager().sendMsg(tgId, "🔔 Попытка входа в аккаунт " + p.getName() + ".\nДля подтверждения используйте панель.");
                plugin.getBotManager().showAccountMenu(tgId, p.getUniqueId());
            } else {
                p.sendMessage("§aУспешный вход! Настоятельно рекомендуем привязать Telegram: /link");
            }
        } else {
            p.sendMessage("§cНеверный пароль!");
        }
        return true;
    }
}
