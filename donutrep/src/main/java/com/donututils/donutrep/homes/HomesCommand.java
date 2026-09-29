package com.donututils.donutrep.homes;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/** /homes - lists every home you've set. */
public final class HomesCommand implements CommandExecutor {

    private final HomeManager homeManager;

    public HomesCommand(HomeManager homeManager) {
        this.homeManager = homeManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(color("&cOnly players can do that."));
            return true;
        }
        List<String> names = homeManager.homeNames(player.getUniqueId());
        if (names.isEmpty()) {
            player.sendMessage(color("&7You have no homes yet - set one with /sethome <name>."));
        } else {
            player.sendMessage(color("&6&lYour Homes &7(" + names.size() + "): &f" + String.join("&7, &f", names)));
        }
        return true;
    }

    private static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
