package minefix.tgbotplugin;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import java.util.UUID;

public class AuthCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Команда только для игроков!");
            return true;
        }

        Player player = (Player) sender;
        UUID uuid = player.getUniqueId();

        if (args.length < 1) {
            player.sendMessage("§cИспользование: /" + label + " <пароль>");
            return true;
        }

        String password = args[0];

        if (label.equalsIgnoreCase("reg")) {
            if (DataStore.isRegistered(uuid)) {
                player.sendMessage("§cВы уже зарегистрированы! Используйте /login <пароль>");
                return true;
            }

            DataStore.registerPlayer(uuid, password);
            player.sendMessage("§aВы успешно зарегистрировались!");
            // После регистрации ТГ еще не привязан — сразу пускаем
            PluginMain.getMovementBlockListener().stopTimer(uuid);

        } else if (label.equalsIgnoreCase("login")) {
            if (!DataStore.isRegistered(uuid)) {
                player.sendMessage("§cВы еще не зарегистрированы! Используйте /reg <пароль>");
                return true;
            }

            if (!DataStore.checkPassword(uuid, password)) {
                player.sendMessage("§cНеверный пароль!");
                return true;
            }

            String tgChatId = DataStore.getTelegramChatId(uuid);
            if (tgChatId != null) {
                // Если привязан Telegram, включаем 2FA режим
                player.sendMessage("§eПароль верен! Подтвердите вход в вашем Telegram-боте...");
                PendingApproval.add(uuid);
                PluginMain.getTelegramBot().send2FARequest(player);
            } else {
                // Если ТГ нет — авторизуем полностью
                player.sendMessage("§aВы успешно авторизовались!");
                PluginMain.getMovementBlockListener().stopTimer(uuid);
            }
        }

        return true;
    }
}
