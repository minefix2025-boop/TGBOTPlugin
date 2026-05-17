package com.example.telegramconsole;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class LinkCommand implements CommandExecutor {
    private final TelegramConsolePlugin plugin;

    public LinkCommand(TelegramConsolePlugin plugin) { 
        this.plugin = plugin; 
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Команда только для игроков!");
            return true;
        }
        Player player = (Player) sender;

        // Используем твой рабочий метод генерации кода из базы данных
        String code = plugin.getDatabaseManager().generateLinkCode(player.getName());

        player.sendMessage("§a[TG] Код успешно сгенерирован!");

        // Создаем интерактивный текстовый компонент для копирования в один клик
        TextComponent message = new TextComponent("§eНажмите §b§lСЮДА§e, чтобы скопировать код: §b§l" + code);
        message.setClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, code));

        // Отправляем кликабельное сообщение через Spigot API
        player.spigot().sendMessage(message);
        
        return true;
    }
}
