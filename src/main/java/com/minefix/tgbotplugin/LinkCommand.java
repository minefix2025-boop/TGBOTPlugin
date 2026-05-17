package com.minefix.tgbotplugin;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class LinkCommand implements CommandExecutor {
    private final DataStore store;
    private final TelegramBotImpl bot;

    public LinkCommand(DataStore store, TelegramBotImpl bot) { this.store = store; this.bot = bot; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) { sender.sendMessage("Только игроки."); return true; }
        Player p = (Player) sender;
        String code = store.createLinkCode(p.getUniqueId(), 300);
        p.sendMessage("Код для привязки: " + code + " \nОтправьте этот код боту в Telegram чтобы привязать аккаунт.");
        return true;
    }
}
