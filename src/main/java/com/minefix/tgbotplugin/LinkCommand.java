package com.minefix.tgbotplugin;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Random;

public class LinkCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Команда только для игроков!");
            return true;
        }

        // ИСПРАВЛЕНО: Явное приведение типов вместо несуществующего метода sender.getPlayer()
        Player player = (Player) sender;
        
        String code = String.valueOf(100000 + new Random().nextInt(900000));
        DataStore.saveTempLinkCode(player.getUniqueId(), code);

        player.sendMessage("§aВаш код для привязки Telegram сгенерирован!");
        
        TextComponent message = new TextComponent("§eНажмите СЮДА, чтобы скопировать код: §b§l" + code);
        message.setClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, code));
        
        player.spigot().sendMessage(message);
        return true;
    }
}
