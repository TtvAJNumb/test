package com.donututils.donutrep.homes;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/** /home [name] - teleports to a named home, or your only home if you just have one. */
public final class HomeCommand implements CommandExecutor {

    private final HomeManager homeManager;

    public HomeCommand(HomeManager homeManager) {
        this.homeManager = homeManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(color("&cOnly players can do that."));
            return true;
        }
        List<String> names = homeManager.homeNames(player.getUniqueId());
        String name;
        if (args.length >= 1) {
            name = args[0];
        } else if (names.size() == 1) {
            name = names.get(0);
        } else if (names.isEmpty()) {
            player.sendMessage(color("&cYou have no homes yet - set one with /sethome <name>."));
            return true;
        } else {
            player.sendMessage(color("&cYou have multiple homes - specify one: /home <name>. Run /homes to list them."));
            return true;
        }
        Location location = homeManager.home(player.getUniqueId(), name);
        if (location == null) {
            player.sendMessage(color("&cNo home named '" + name + "'."));
            return true;
        }
        player.teleport(location);
        player.sendMessage(color("&aTeleported to '" + name + "'."));
        return true;
    }

    private static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
