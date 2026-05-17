package com.minefix.tgbotplugin;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.mindrot.jbcrypt.BCrypt;

public class AuthCommand implements CommandExecutor {
    private final DataStore store;

    public AuthCommand(DataStore store) { this.store = store; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Команда для игроков.");
            return true;
        }
        Player p = (Player) sender;
        if (cmd.getName().equalsIgnoreCase("reg")) {
            if (args.length < 1) { p.sendMessage("Использование: /reg <пароль>"); return true; }
            String pass = args[0];
            if (store.isRegistered(p.getUniqueId())) {
                p.sendMessage("Вы уже зарегистрированы.");
                return true;
            }
            String hash = BCrypt.hashpw(pass, BCrypt.gensalt());
            store.savePlayerPassword(p.getUniqueId(), p.getName(), hash);
            store.setAuthorized(p.getUniqueId(), true);
            p.sendMessage("Регистрация успешна.");
            return true;
        } else if (cmd.getName().equalsIgnoreCase("login")) {
            if (args.length < 1) { p.sendMessage("Использование: /login <пароль>"); return true; }
            String pass = args[0];
            PlayerData pd = store.getPlayerData(p.getUniqueId());
            if (pd == null) { p.sendMessage("Аккаунт не найден. Зарегистрируйтесь: /reg <пароль>"); return true; }
            if (!BCrypt.checkpw(pass, pd.getPasswordHash())) {
                p.sendMessage("Неверный пароль.");
                store.registerFailedAttempt(p.getUniqueId());
                return true;
            }
            // пароль верный — запускаем Telegram 2FA если есть привязка
            if (pd.getTelegramChatId() != null) {
                String approvalId = store.createLoginApproval(p.getUniqueId(), p.getAddress().getAddress().getHostAddress(), 120);
                PluginMain.getInstance().getTelegramBot().sendLoginApprovalToAdmins(approvalId, p.getUniqueId(), p.getAddress().getAddress().getHostAddress());
                p.sendMessage("На админов отправлен запрос подтверждения входа. Ожидайте.");
                store.setAuthorized(p.getUniqueId(), false);
            } else {
                store.setAuthorized(p.getUniqueId(), true);
                p.sendMessage("Вход выполнен.");
            }
            return true;
        }
        return false;
    }
}
